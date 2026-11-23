# Kalamora — Your Words. Your World.

**Kalamora** is a native Android writing and publishing studio built with **Kotlin**, **Jetpack Compose (Material 3)**, **Firebase Authentication (Google Sign-In via Jetpack Credential Manager)**, and **Cloud Firestore**.

> **Architecture Note (Native Android vs. Capacitor):**
> Kalamora in this repository is already built as a **100% native Android application** (no React WebView or Capacitor wrapper is needed). It compiles directly to a real installable Android APK/AAB with native performance, native Google Sign-In, zero-permission Android Photo Picker, adaptive launcher icon, native splash screen, and full offline-resilient Firestore persistence.

---

## Features

- **Authentication & Accounts**: Google Sign-In via Jetpack Credential Manager, persistent user profile (display name, username, bio, profile photo, interface language), sign out, and full account + data deletion.
- **Book Writing Studio**: Create books with genre, language, cover photo (Android Photo Picker), rich chapter management (add, reorder, delete, rename, undo/redo, bold/italic/heading/quote/bullet formatting, live word count, auto-save, preview mode, and public/private publishing).
- **Poetry & Shayari Studio**: Dedicated editor for Shayari, Ghazals, Nazm, Couplets, and Poetry with title, tags, language selector, preview card, auto-save, and public/private controls.
- **Public Library & Readers**: Browse and search published books and shayari from the community with distraction-free reading views and native Android share sheet export.
- **Literary Design System**: Deep Indigo (`#302B63`), Warm Cream (`#F7F3EB`), Muted Gold (`#C6A15B`), Soft Off-White (`#FCFBF8`), Light & Dark modes with edge-to-edge system bars, and bundled English (`Playfair Display`, `Lora`, `Inter`) + Hindi (`Noto Sans Devanagari`) fonts.
- **Minimal Permissions**: Only `INTERNET` and `ACCESS_NETWORK_STATE`. Image selection uses Android's zero-permission Photo Picker (`ActivityResultContracts.PickVisualMedia`).

---

## Android Configuration Summary

| Setting | Value |
| --- | --- |
| App Name | `Kalamora` |
| Version Name / Code | `1.0.0` / `1` |
| Current Application ID | `com.aistudio.kalamora.vkpqzx` (bound to current Firebase `app/google-services.json`) |
| Min SDK / Target SDK | API 24 (Android 7.0) / API 36 (Android 16) |
| Default Orientation | Portrait (`android:screenOrientation="portrait"`) |
| Splash Screen | Native Deep Indigo launch screen + Android 12+ Splash Screen API (`values-v31/themes.xml`) |
| Gradle / AGP / JDK | Gradle `9.3.1` / AGP `9.1.1` / JDK `17` |

### Switching Application ID to `com.kalamora.app` (Optional for Play Store)

The project currently keeps `applicationId = "com.aistudio.kalamora.vkpqzx"` because your Firebase OAuth 2.0 Android client and SHA-1 fingerprint in `app/google-services.json` are registered for that package ID. To migrate to `com.kalamora.app` before publishing to Google Play:

1. Open **Firebase Console** -> **Project Settings** -> **Your apps** -> **Add app (Android)**.
2. Register package name `com.kalamora.app` along with your **debug** and **release** SHA-1 and SHA-256 certificate fingerprints.
3. Download the updated `google-services.json` and replace `app/google-services.json`.
4. Update `applicationId = "com.kalamora.app"` in `app/build.gradle.kts` (leave `namespace = "com.example"` unchanged).

---

## Security Rules & Ignored Files

`.gitignore` explicitly blocks sensitive files from ever being committed to GitHub:

- Keystores & signing credentials (`*.jks`, `*.keystore`, `debug.keystore`, `debug.keystore.base64`, `my-upload-key.jks`, `*.p12`, `*.pem`, `keystore.properties`)
- Environment secrets (`.env`, `.env.*`, `secrets.properties`, `signing.properties`)
- Firebase Admin / Service Account private keys (`*service-account*.json`, `*firebase-adminsdk*.json`)
- Build outputs (`build/`, `app/build/`, `*.apk`, `*.aab`)

> **Never** commit keystores, passwords, service account JSON keys, or private API keys to Git.

---

## Local Build Instructions

### Prerequisites

- **JDK 17** (Temurin / OpenJDK 17)
- **Android SDK** (SDK Platform 36, Build-Tools, Platform-Tools) with `ANDROID_HOME` configured
- **Gradle 9.3.1** (or open directly in Android Studio Ladybug / Meerkat+)

### Build Debug APK Locally

```bash
# Ensure .env exists (copied from .env.example)
cp -n .env.example .env

# Generate a local debug.keystore once if you do not already have one
keytool -genkeypair -v \
  -keystore debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"

# Build the debug APK
gradle :app:assembleDebug
```

Debug APK output path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Build Signed Release APK Locally

```bash
KEYSTORE_PATH="./my-upload-key.jks" \
STORE_PASSWORD="YOUR_STORE_PASSWORD" \
KEY_PASSWORD="YOUR_KEY_PASSWORD" \
gradle :app:assembleRelease
```

Release APK output path:

```text
app/build/outputs/apk/release/app-release.apk
```

---

## GitHub Repository Setup

Initialize Git locally and push to a new GitHub repository:

```bash
git init
git branch -M main
git add .
git status   # Verify NO .jks, .keystore, .env, or service-account JSON files are staged
git commit -m "Initial commit: Kalamora native Android app with CI/CD workflows"
git remote add origin https://github.com/<YOUR_USERNAME>/kalamora-android.git
git push -u origin main
```

---

## Automatic APK Generation (GitHub Actions)

### 1. Debug APK Workflow (`.github/workflows/android-apk.yml`)

- **Triggers**: Every push to `main` and manual run via **Actions -> Kalamora Android Debug APK -> Run workflow**.
- **Output Artifact**: `Kalamora-Android-APK` (`app/build/outputs/apk/debug/app-debug.apk`).
- **Optional Secret for Google Sign-In on Debug Builds**:
  - Add `DEBUG_KEYSTORE_BASE64` (base64 of the debug keystore whose SHA-1 is registered in Firebase Console) under **Settings -> Secrets and variables -> Actions** so Google Sign-In works immediately on downloaded CI debug APKs.

### 2. Signed Release APK Workflow (`.github/workflows/android-release.yml`)

- **Triggers**: Manual run via **Actions -> Kalamora Android Signed Release APK -> Run workflow** (with optional `.aab` bundle toggle) or pushing a version tag such as `v1.0.0`.
- **Verification**: Fails fast if signing secrets are missing and runs `apksigner verify --print-certs` to prove the output APK is genuinely signed.
- **Output Artifact**: `Kalamora-Android-Release-APK` (`app/build/outputs/apk/release/app-release.apk`).

#### Generating a Secure Release Upload Keystore

Run this once on your local machine (keep the `.jks` file and passwords safely backed up offline—never commit them):

```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias upload
```

> **Important:** The alias **must** be `upload` to match `app/build.gradle.kts`.

Encode the keystore to Base64 (without line breaks):

```bash
# Linux
base64 -w 0 my-upload-key.jks

# macOS
base64 -i my-upload-key.jks | tr -d '\n'
```

Print your release certificate SHA-1 and SHA-256 and register both in **Firebase Console -> Project Settings -> Your Android App -> Add fingerprint** so Google Sign-In works in release builds:

```bash
keytool -list -v -keystore my-upload-key.jks -alias upload
```

#### Required GitHub Actions Secrets (Release Workflow)

Go to **GitHub Repository -> Settings -> Secrets and variables -> Actions -> New repository secret** and add:

| Secret Name | Value |
| --- | --- |
| `KEYSTORE_BASE64` | Base64-encoded string of `my-upload-key.jks` |
| `STORE_PASSWORD` | Keystore password chosen during `keytool` |
| `KEY_PASSWORD` | Key password for alias `upload` |
| `GOOGLE_SERVICES_JSON_BASE64` *(optional)* | Base64-encoded `app/google-services.json` if you choose not to track it in Git |

---

## Downloading & Installing the APK on an Android Phone

1. Open your GitHub repository -> **Actions** tab.
2. Click the latest completed **Kalamora Android Debug APK** (or **Kalamora Android Signed Release APK**) workflow run.
3. Scroll to **Artifacts** and download **`Kalamora-Android-APK`** (zip file).
4. Extract `app-debug.apk` and transfer it to your Android phone (USB, Google Drive, or direct browser download on your phone).
5. On your Android phone, tap the `.apk` file. If prompted, enable **Install unknown apps** for your browser or file manager, then tap **Install**.
6. Open **Kalamora**, sign in with Google, and verify Books, Chapters, Auto-save, Shayari, Public Library, Profile Settings, and Cover Photo upload.
