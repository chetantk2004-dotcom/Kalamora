const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_author_123";
const BOB_UID = "bob_reader_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// =========================================================
// 1. USERS COLLECTION (PII ISOLATION & PROFILE VALIDATION)
// =========================================================

test("Users: unauthenticated cannot read or write profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      username: "alice_writes",
      email: "alice@example.com",
      preferredLanguage: "en",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Users: owner can create/read/update own profile; other user blocked from PII", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  // Mirror UserRepository.ensureUserProfile payload
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Meera Sharma",
      username: "meera_sharma",
      email: "meera@example.com",
      preferredLanguage: "en",
      darkMode: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Owner can get own profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Owner cannot list all users
  await assertFails(aliceDb.collection("users").get());

  // Other user cannot read Alice's PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Owner can update allowed profile fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "मीरा शर्मा",
      preferredLanguage: "hi",
      bio: "Hindi and English novelist",
      darkMode: true,
      updatedAt: pastDate(1000),
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(500),
    })
  );

  // Future timestamp must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future Edit",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Immutable createdAt mutation must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
});

// =========================================================
// 2. BOOKS & CHAPTERS (PRIVATE VS PUBLIC PUBLISHING)
// =========================================================

test("Books: private book is owner-only; publishing opens read; unpublishing blocks immediately", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  // Create private book (default)
  await assertSucceeds(
    aliceDb.collection("books").doc("book_1").set({
      ownerId: ALICE_UID,
      title: "Monsoon Letters / मानसून के पत्र",
      subtitle: "Stories from the Hills",
      description: "A bilingual collection of letters.",
      authorName: "Meera Sharma",
      category: "Fiction",
      language: "Hindi",
      coverTheme: "indigo_gold",
      visibility: "private",
      status: "draft",
      chapterCount: 1,
      totalWordCount: 12,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create chapter under book_1
  await assertSucceeds(
    aliceDb
      .collection("books")
      .doc("book_1")
      .collection("chapters")
      .doc("ch_1")
      .set({
        ownerId: ALICE_UID,
        bookId: "book_1",
        bookVisibility: "private",
        title: "अध्याय 1: पहली बारिश",
        content: "बारिश की पहली बूँद के साथ पुरानी यादें लौट आईं।",
        chapterOrder: 0,
        wordCount: 12,
        characterCount: 48,
        createdAt: created,
        updatedAt: created,
      })
  );

  // TEST 10: Private book & chapter CANNOT be accessed by Bob
  await assertFails(bobDb.collection("books").doc("book_1").get());
  await assertFails(
    bobDb.collection("books").doc("book_1").collection("chapters").doc("ch_1").get()
  );

  // Owner query succeeds; unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("books").where("ownerId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("books").get());

  // Public query succeeds (returns empty while private)
  await assertSucceeds(
    bobDb
      .collection("books")
      .where("visibility", "==", "public")
      .where("status", "==", "published")
      .get()
  );

  // Publish book & sync chapter visibility atomically (mirrors BookRepository.setBookVisibility)
  const publishTime = pastDate(2000);
  const publishBatch = aliceDb.batch();
  publishBatch.update(aliceDb.collection("books").doc("book_1"), {
    visibility: "public",
    status: "published",
    publishedAt: publishTime,
    updatedAt: publishTime,
  });
  publishBatch.update(
    aliceDb.collection("books").doc("book_1").collection("chapters").doc("ch_1"),
    {
      bookVisibility: "public",
      updatedAt: publishTime,
    }
  );
  await assertSucceeds(publishBatch.commit());

  // TEST 11: Public book & chapter CAN be read by Bob, CANNOT be edited or deleted by Bob
  await assertSucceeds(bobDb.collection("books").doc("book_1").get());
  await assertSucceeds(
    bobDb.collection("books").doc("book_1").collection("chapters").doc("ch_1").get()
  );
  await assertSucceeds(
    bobDb
      .collection("books")
      .doc("book_1")
      .collection("chapters")
      .where("bookVisibility", "==", "public")
      .get()
  );
  await assertFails(
    bobDb.collection("books").doc("book_1").update({
      title: "Hijacked",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    bobDb
      .collection("books")
      .doc("book_1")
      .collection("chapters")
      .doc("ch_1")
      .update({
        content: "Tampered",
        wordCount: 1,
        characterCount: 8,
        updatedAt: pastDate(500),
      })
  );

  // TEST 12: Changing public book back to private blocks public access immediately
  const unpublishTime = pastDate(1000);
  const unpublishBatch = aliceDb.batch();
  unpublishBatch.update(aliceDb.collection("books").doc("book_1"), {
    visibility: "private",
    status: "draft",
    updatedAt: unpublishTime,
  });
  unpublishBatch.update(
    aliceDb.collection("books").doc("book_1").collection("chapters").doc("ch_1"),
    {
      bookVisibility: "private",
      updatedAt: unpublishTime,
    }
  );
  await assertSucceeds(unpublishBatch.commit());

  await assertFails(bobDb.collection("books").doc("book_1").get());
  await assertFails(
    bobDb.collection("books").doc("book_1").collection("chapters").doc("ch_1").get()
  );
});

test("Books: reject mismatched visibility/status, ownerId spoofing, and @DocumentId id key", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(5000);

  // Mismatched visibility='public' with status='draft' must fail
  await assertFails(
    aliceDb.collection("books").doc("bad_vis").set({
      ownerId: ALICE_UID,
      title: "Bad Book",
      description: "",
      authorName: "Alice",
      category: "Fiction",
      language: "English",
      coverTheme: "indigo_gold",
      visibility: "public",
      status: "draft",
      chapterCount: 0,
      totalWordCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Extra "id" field must fail (guards @DocumentId parity)
  await assertFails(
    aliceDb.collection("books").doc("bad_id").set({
      id: "bad_id",
      ownerId: ALICE_UID,
      title: "Bad ID Book",
      description: "",
      authorName: "Alice",
      category: "Fiction",
      language: "English",
      coverTheme: "indigo_gold",
      visibility: "private",
      status: "draft",
      chapterCount: 0,
      totalWordCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // OwnerId spoofing must fail
  await assertFails(
    aliceDb.collection("books").doc("spoofed").set({
      ownerId: BOB_UID,
      title: "Spoofed Book",
      description: "",
      authorName: "Bob",
      category: "Fiction",
      language: "English",
      coverTheme: "indigo_gold",
      visibility: "private",
      status: "draft",
      chapterCount: 0,
      totalWordCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Chapters: owner can edit content, reorder, and delete; non-owner cannot create orphan chapter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("books").doc("book_ch").set({
      ownerId: ALICE_UID,
      title: "Chapter Test",
      description: "Desc",
      authorName: "Alice",
      category: "Mystery",
      language: "English",
      coverTheme: "cream_ink",
      visibility: "private",
      status: "draft",
      chapterCount: 0,
      totalWordCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot create a chapter in Alice's book
  await assertFails(
    bobDb
      .collection("books")
      .doc("book_ch")
      .collection("chapters")
      .doc("bob_ch")
      .set({
        ownerId: BOB_UID,
        bookId: "book_ch",
        bookVisibility: "private",
        title: "Intrusion",
        content: "Nope",
        chapterOrder: 0,
        wordCount: 1,
        characterCount: 4,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Alice creates chapter, updates content, reorders, and deletes
  await assertSucceeds(
    aliceDb
      .collection("books")
      .doc("book_ch")
      .collection("chapters")
      .doc("ch_a")
      .set({
        ownerId: ALICE_UID,
        bookId: "book_ch",
        bookVisibility: "private",
        title: "Chapter 1",
        content: "Initial line",
        chapterOrder: 0,
        wordCount: 2,
        characterCount: 12,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("books")
      .doc("book_ch")
      .collection("chapters")
      .doc("ch_a")
      .update({
        title: "Chapter 1: Dawn",
        content: "Updated content with\nmultiple lines.",
        wordCount: 5,
        characterCount: 36,
        updatedAt: pastDate(1500),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("books")
      .doc("book_ch")
      .collection("chapters")
      .doc("ch_a")
      .update({
        chapterOrder: 2,
        updatedAt: pastDate(1000),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("books")
      .doc("book_ch")
      .collection("chapters")
      .doc("ch_a")
      .delete()
  );
});

// =========================================================
// 3. SHAYARI COLLECTION (PRIVATE DRAFT & PUBLIC READING)
// =========================================================

test("Shayari: CRUD, line-break preservation, private isolation, public reading, and unpublish", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  // Create private shayari
  await assertSucceeds(
    aliceDb.collection("shayari").doc("sh_1").set({
      ownerId: ALICE_UID,
      authorName: "मीरा",
      title: "चाँद और ख़ामोशी",
      content: "ख़ामोश रातों में जब चाँद मुस्कुराता है,\nकाग़ज़ पर हर एहसास खुद उतर आता है।",
      category: "Love",
      language: "Hindi",
      visibility: "private",
      status: "draft",
      wordCount: 15,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Private shayari blocked for Bob
  await assertFails(bobDb.collection("shayari").doc("sh_1").get());

  // Unfiltered list blocked
  await assertFails(bobDb.collection("shayari").get());

  // Owner query allowed
  await assertSucceeds(
    aliceDb.collection("shayari").where("ownerId", "==", ALICE_UID).get()
  );

  // Publish shayari
  await assertSucceeds(
    aliceDb.collection("shayari").doc("sh_1").update({
      visibility: "public",
      status: "published",
      publishedAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Public shayari readable by Bob via get and public query
  await assertSucceeds(bobDb.collection("shayari").doc("sh_1").get());
  await assertSucceeds(
    bobDb
      .collection("shayari")
      .where("visibility", "==", "public")
      .where("status", "==", "published")
      .get()
  );

  // Bob cannot modify or delete Alice's public shayari
  await assertFails(
    bobDb.collection("shayari").doc("sh_1").update({
      title: "Stolen",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(bobDb.collection("shayari").doc("sh_1").delete());

  // Alice unpublishes back to private -> Bob blocked again
  await assertSucceeds(
    aliceDb.collection("shayari").doc("sh_1").update({
      visibility: "private",
      status: "draft",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.collection("shayari").doc("sh_1").get());

  // Alice deletes shayari
  await assertSucceeds(aliceDb.collection("shayari").doc("sh_1").delete());
});

test("Atomic Batch Parity: createBook + chapter 1 batch and saveChapterContent + totalWordCount batch succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(8000);

  // Mirrors KalamoraRepository.createBook atomic batch
  const bookRef = aliceDb.collection("books").doc("batch_book_1");
  const ch1Ref = bookRef.collection("chapters").doc("batch_ch_1");
  const createBatch = aliceDb.batch();
  createBatch.set(bookRef, {
    ownerId: ALICE_UID,
    title: "Batch Novel",
    description: "Created with chapter 1 atomically",
    authorName: "Meera",
    category: "Philosophy",
    language: "English",
    coverTheme: "forest_linen",
    visibility: "private",
    status: "draft",
    chapterCount: 1,
    totalWordCount: 0,
    createdAt: created,
    updatedAt: created,
  });
  createBatch.set(ch1Ref, {
    ownerId: ALICE_UID,
    bookId: "batch_book_1",
    bookVisibility: "private",
    title: "Chapter 1",
    content: "",
    chapterOrder: 0,
    wordCount: 0,
    characterCount: 0,
    createdAt: created,
    updatedAt: created,
  });
  await assertSucceeds(createBatch.commit());

  // Mirrors KalamoraRepository.saveChapterContent atomic batch
  const saveTime = pastDate(1000);
  const saveBatch = aliceDb.batch();
  saveBatch.update(ch1Ref, {
    title: "Chapter 1: Beginnings",
    content: "Every great book begins with a single word.\nहर महान पुस्तक की शुरुआत एक शब्द से होती है।",
    wordCount: 18,
    characterCount: 89,
    updatedAt: saveTime,
  });
  saveBatch.update(bookRef, {
    totalWordCount: 18,
    updatedAt: saveTime,
  });
  await assertSucceeds(saveBatch.commit());
});

