package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.Book
import com.example.data.model.Chapter
import com.example.data.model.KalamoraCatalog
import com.example.data.model.Shayari
import com.example.data.model.TextStatsUtil
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KalamoraRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Kalamora Cloud.")
        }
        return uid
    }

    // =========================================================
    // USER PROFILE
    // =========================================================

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val rawName = user.displayName?.trim().takeIf { !it.isNullOrBlank() } ?: "Kalamora Writer"
                val safeDisplayName = rawName.take(100)
                val baseUsername = (user.email?.substringBefore("@") ?: safeDisplayName)
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "_")
                    .trim('_')
                    .take(28)
                    .ifBlank { "writer" }
                val safeUsername = if (baseUsername.length < 3) "${baseUsername}_ink" else baseUsername
                val payload = mutableMapOf<String, Any?>(
                    "uid" to uid,
                    "displayName" to safeDisplayName,
                    "username" to safeUsername,
                    "email" to (user.email?.take(254) ?: ""),
                    "photoURL" to user.photoUrl?.toString()?.take(250000),
                    "preferredLanguage" to "en",
                    "darkMode" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        username: String,
        bio: String?,
        photoURL: String?,
        preferredLanguage: String,
        darkMode: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val safeName = displayName.trim().take(100)
            require(safeName.isNotEmpty()) { "Display name cannot be empty." }
            val safeUsername = username.trim().replace(" ", "_").take(40)
            require(safeUsername.length >= 2) { "Username must be at least 2 characters." }
            val safeLang = if (preferredLanguage in listOf("en", "hi")) preferredLanguage else "en"
            val updates = mutableMapOf<String, Any?>(
                "displayName" to safeName,
                "username" to safeUsername,
                "bio" to bio?.trim()?.take(300),
                "photoURL" to photoURL?.take(250000),
                "preferredLanguage" to safeLang,
                "darkMode" to darkMode,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // =========================================================
    // BOOKS
    // =========================================================

    fun observeUserBooks(userId: String): Flow<List<Book>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "books"
        emitAll(
            db.collection("books")
                .whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Book::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicBooks(): Flow<List<Book>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "books"
        emitAll(
            db.collection("books")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("status", "published")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Book::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.publishedAt ?: it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeBook(bookId: String): Flow<Book?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || bookId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "books/$bookId"
        emitAll(
            db.collection("books").document(bookId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(Book::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createBook(
        title: String,
        subtitle: String?,
        authorName: String,
        description: String,
        category: String,
        language: String,
        coverTheme: String,
        coverURL: String?,
        makePublic: Boolean = false
    ): Result<String> {
        var path = "books"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().take(160)
            require(safeTitle.isNotEmpty()) { "Book title is required." }
            val safeAuthor = authorName.trim().take(100)
            require(safeAuthor.isNotEmpty()) { "Author name is required." }
            val safeCategory = if (category in KalamoraCatalog.bookCategories) category else "Other"
            val safeLang = language.trim().take(40).ifBlank { "English" }
            val safeVisibility = if (makePublic) "public" else "private"
            val safeStatus = if (makePublic) "published" else "draft"

            val bookRef = db.collection("books").document()
            val firstChapterRef = bookRef.collection("chapters").document()
            path = bookRef.path

            val bookPayload = mutableMapOf<String, Any?>(
                "ownerId" to uid,
                "title" to safeTitle,
                "subtitle" to subtitle?.trim()?.take(200)?.ifBlank { null },
                "description" to description.trim().take(2000),
                "authorName" to safeAuthor,
                "category" to safeCategory,
                "language" to safeLang,
                "coverURL" to coverURL?.take(250000)?.ifBlank { null },
                "coverTheme" to coverTheme.ifBlank { "indigo_gold" }.take(40),
                "visibility" to safeVisibility,
                "status" to safeStatus,
                "chapterCount" to 1L,
                "totalWordCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "publishedAt" to if (makePublic) FieldValue.serverTimestamp() else null
            ).filterValues { it != null }

            val firstChapterPayload = mapOf(
                "ownerId" to uid,
                "bookId" to bookRef.id,
                "bookVisibility" to safeVisibility,
                "title" to "Chapter 1",
                "content" to "",
                "chapterOrder" to 0L,
                "wordCount" to 0L,
                "characterCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(bookRef, bookPayload)
            batch.set(firstChapterRef, firstChapterPayload)
            batch.commit().await()
            Result.success(bookRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBookMetadata(
        bookId: String,
        title: String,
        subtitle: String?,
        authorName: String,
        description: String,
        category: String,
        language: String,
        coverTheme: String,
        coverURL: String?
    ): Result<Unit> {
        val path = "books/$bookId"
        return try {
            requireUserId()
            require(bookId.isNotBlank())
            val safeTitle = title.trim().take(160)
            require(safeTitle.isNotEmpty()) { "Book title cannot be empty." }
            val safeAuthor = authorName.trim().take(100)
            require(safeAuthor.isNotEmpty()) { "Author name cannot be empty." }
            val safeCategory = if (category in KalamoraCatalog.bookCategories) category else "Other"
            val safeLang = language.trim().take(40).ifBlank { "English" }

            val updates = mapOf<String, Any?>(
                "title" to safeTitle,
                "subtitle" to subtitle?.trim()?.take(200)?.ifBlank { null },
                "authorName" to safeAuthor,
                "description" to description.trim().take(2000),
                "category" to safeCategory,
                "language" to safeLang,
                "coverTheme" to coverTheme.ifBlank { "indigo_gold" }.take(40),
                "coverURL" to coverURL?.take(250000)?.ifBlank { null },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("books").document(bookId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setBookVisibility(bookId: String, makePublic: Boolean): Result<Unit> {
        val path = "books/$bookId"
        return try {
            val uid = requireUserId()
            require(bookId.isNotBlank())
            val newVisibility = if (makePublic) "public" else "private"
            val newStatus = if (makePublic) "published" else "draft"

            val bookRef = db.collection("books").document(bookId)
            val chaptersSnapshot = bookRef.collection("chapters")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()

            val bookUpdates = mutableMapOf<String, Any>(
                "visibility" to newVisibility,
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (makePublic) {
                bookUpdates["publishedAt"] = FieldValue.serverTimestamp()
            }

            // Commit in chunks of 450 operations to stay safely under Firestore 500 batch limit
            val chapterDocs = chaptersSnapshot.documents
            val firstChunk = chapterDocs.take(440)
            val batch = db.batch()
            batch.update(bookRef, bookUpdates)
            firstChunk.forEach { chDoc ->
                batch.update(
                    chDoc.reference,
                    mapOf(
                        "bookVisibility" to newVisibility,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()

            if (chapterDocs.size > 440) {
                chapterDocs.drop(440).chunked(440).forEach { chunk ->
                    val nextBatch = db.batch()
                    chunk.forEach { chDoc ->
                        nextBatch.update(
                            chDoc.reference,
                            mapOf(
                                "bookVisibility" to newVisibility,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    nextBatch.commit().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBookWithChapters(bookId: String): Result<Unit> {
        val path = "books/$bookId"
        return try {
            val uid = requireUserId()
            require(bookId.isNotBlank())
            val bookRef = db.collection("books").document(bookId)
            val chaptersSnapshot = bookRef.collection("chapters")
                .whereEqualTo("ownerId", uid)
                .get()
                .await()

            chaptersSnapshot.documents.chunked(440).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            bookRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================
    // CHAPTERS
    // =========================================================

    fun observeOwnerChapters(bookId: String, ownerId: String): Flow<List<Chapter>> = flow {
        if (bookId.isBlank() || ownerId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "books/$bookId/chapters"
        emitAll(
            db.collection("books").document(bookId).collection("chapters")
                .whereEqualTo("ownerId", ownerId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Chapter::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.chapterOrder }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == ownerId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicChapters(bookId: String): Flow<List<Chapter>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || bookId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "books/$bookId/chapters"
        emitAll(
            db.collection("books").document(bookId).collection("chapters")
                .whereEqualTo("bookVisibility", "public")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Chapter::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.chapterOrder }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createChapter(
        bookId: String,
        bookVisibility: String,
        title: String,
        chapterOrder: Long,
        newChapterCount: Long
    ): Result<String> {
        var path = "books/$bookId/chapters"
        return try {
            val uid = requireUserId()
            require(bookId.isNotBlank())
            val safeTitle = title.trim().take(200).ifBlank { "Chapter ${chapterOrder + 1}" }
            val safeVis = if (bookVisibility == "public") "public" else "private"
            val bookRef = db.collection("books").document(bookId)
            val chRef = bookRef.collection("chapters").document()
            path = chRef.path

            val payload = mapOf(
                "ownerId" to uid,
                "bookId" to bookId,
                "bookVisibility" to safeVis,
                "title" to safeTitle,
                "content" to "",
                "chapterOrder" to chapterOrder.coerceIn(0L, 10000L),
                "wordCount" to 0L,
                "characterCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val batch = db.batch()
            batch.set(chRef, payload)
            batch.update(
                bookRef,
                mapOf(
                    "chapterCount" to newChapterCount.coerceIn(0L, 500L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(chRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveChapterContent(
        bookId: String,
        chapterId: String,
        title: String,
        content: String,
        newBookTotalWordCount: Long
    ): Result<Unit> {
        val path = "books/$bookId/chapters/$chapterId"
        return try {
            requireUserId()
            require(bookId.isNotBlank() && chapterId.isNotBlank())
            val safeTitle = title.trim().take(200).ifBlank { "Untitled Chapter" }
            val safeContent = content.take(200000)
            val words = TextStatsUtil.countWords(safeContent).coerceIn(0L, 200000L)
            val chars = TextStatsUtil.countCharacters(safeContent).coerceIn(0L, 200000L)

            val bookRef = db.collection("books").document(bookId)
            val chRef = bookRef.collection("chapters").document(chapterId)

            val batch = db.batch()
            batch.update(
                chRef,
                mapOf(
                    "title" to safeTitle,
                    "content" to safeContent,
                    "wordCount" to words,
                    "characterCount" to chars,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                bookRef,
                mapOf(
                    "totalWordCount" to newBookTotalWordCount.coerceIn(0L, 5000000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reorderChapters(bookId: String, orderedChapterIds: List<String>): Result<Unit> {
        val path = "books/$bookId/chapters"
        return try {
            requireUserId()
            require(bookId.isNotBlank())
            val chaptersCol = db.collection("books").document(bookId).collection("chapters")
            val batch = db.batch()
            orderedChapterIds.forEachIndexed { index, chId ->
                batch.update(
                    chaptersCol.document(chId),
                    mapOf(
                        "chapterOrder" to index.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChapter(
        bookId: String,
        chapterId: String,
        remainingChapterCount: Long,
        newBookTotalWordCount: Long
    ): Result<Unit> {
        val path = "books/$bookId/chapters/$chapterId"
        return try {
            requireUserId()
            require(bookId.isNotBlank() && chapterId.isNotBlank())
            val bookRef = db.collection("books").document(bookId)
            val chRef = bookRef.collection("chapters").document(chapterId)
            val batch = db.batch()
            batch.delete(chRef)
            batch.update(
                bookRef,
                mapOf(
                    "chapterCount" to remainingChapterCount.coerceIn(0L, 500L),
                    "totalWordCount" to newBookTotalWordCount.coerceIn(0L, 5000000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================
    // SHAYARI
    // =========================================================

    fun observeUserShayari(userId: String): Flow<List<Shayari>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "shayari"
        emitAll(
            db.collection("shayari")
                .whereEqualTo("ownerId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Shayari::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicShayari(): Flow<List<Shayari>> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "shayari"
        emitAll(
            db.collection("shayari")
                .whereEqualTo("visibility", "public")
                .whereEqualTo("status", "published")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Shayari::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.publishedAt ?: it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeShayariById(shayariId: String): Flow<Shayari?> = flow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || shayariId.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "shayari/$shayariId"
        emitAll(
            db.collection("shayari").document(shayariId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(Shayari::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createShayari(
        authorName: String,
        title: String,
        content: String,
        category: String,
        language: String,
        makePublic: Boolean = false
    ): Result<String> {
        var path = "shayari"
        return try {
            val uid = requireUserId()
            val safeTitle = title.trim().take(160)
            require(safeTitle.isNotEmpty()) { "Title is required." }
            // Preserve verbatim poetry lines and breaks — NEVER trim internal formatting or alter words
            val safeContent = content.take(50000)
            require(safeContent.isNotBlank()) { "Please write your shayari before saving." }
            val safeAuthor = authorName.trim().take(100).ifBlank { "Writer" }
            val safeCategory = if (category in KalamoraCatalog.shayariCategories) category else "Other"
            val safeLang = language.trim().take(40).ifBlank { "Hindi" }
            val safeVis = if (makePublic) "public" else "private"
            val safeStatus = if (makePublic) "published" else "draft"
            val words = TextStatsUtil.countWords(safeContent).coerceIn(0L, 50000L)

            val docRef = db.collection("shayari").document()
            path = docRef.path

            val payload = mutableMapOf<String, Any?>(
                "ownerId" to uid,
                "authorName" to safeAuthor,
                "title" to safeTitle,
                "content" to safeContent,
                "category" to safeCategory,
                "language" to safeLang,
                "visibility" to safeVis,
                "status" to safeStatus,
                "wordCount" to words,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "publishedAt" to if (makePublic) FieldValue.serverTimestamp() else null
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateShayari(
        shayariId: String,
        authorName: String,
        title: String,
        content: String,
        category: String,
        language: String,
        makePublic: Boolean
    ): Result<Unit> {
        val path = "shayari/$shayariId"
        return try {
            requireUserId()
            require(shayariId.isNotBlank())
            val safeTitle = title.trim().take(160)
            require(safeTitle.isNotEmpty()) { "Title is required." }
            val safeContent = content.take(50000)
            require(safeContent.isNotBlank()) { "Shayari content cannot be empty." }
            val safeAuthor = authorName.trim().take(100).ifBlank { "Writer" }
            val safeCategory = if (category in KalamoraCatalog.shayariCategories) category else "Other"
            val safeLang = language.trim().take(40).ifBlank { "Hindi" }
            val safeVis = if (makePublic) "public" else "private"
            val safeStatus = if (makePublic) "published" else "draft"
            val words = TextStatsUtil.countWords(safeContent).coerceIn(0L, 50000L)

            val updates = mutableMapOf<String, Any>(
                "authorName" to safeAuthor,
                "title" to safeTitle,
                "content" to safeContent,
                "category" to safeCategory,
                "language" to safeLang,
                "visibility" to safeVis,
                "status" to safeStatus,
                "wordCount" to words,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (makePublic) {
                updates["publishedAt"] = FieldValue.serverTimestamp()
            }

            db.collection("shayari").document(shayariId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setShayariVisibility(shayariId: String, makePublic: Boolean): Result<Unit> {
        val path = "shayari/$shayariId"
        return try {
            requireUserId()
            require(shayariId.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "visibility" to if (makePublic) "public" else "private",
                "status" to if (makePublic) "published" else "draft",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (makePublic) {
                updates["publishedAt"] = FieldValue.serverTimestamp()
            }
            db.collection("shayari").document(shayariId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteShayari(shayariId: String): Result<Unit> {
        val path = "shayari/$shayariId"
        return try {
            requireUserId()
            require(shayariId.isNotBlank())
            db.collection("shayari").document(shayariId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // =========================================================
    // ACCOUNT DELETION DATA CLEANUP
    // =========================================================

    suspend fun deleteAllUserData(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            // 1. Delete all books & their chapters
            val books = db.collection("books").whereEqualTo("ownerId", uid).get().await()
            for (bookDoc in books.documents) {
                val chapters = bookDoc.reference.collection("chapters")
                    .whereEqualTo("ownerId", uid)
                    .get()
                    .await()
                chapters.documents.chunked(440).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
                bookDoc.reference.delete().await()
            }
            // 2. Delete all shayari
            val shayariDocs = db.collection("shayari").whereEqualTo("ownerId", uid).get().await()
            shayariDocs.documents.chunked(440).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            // 3. Delete user profile
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
