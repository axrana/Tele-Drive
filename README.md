# Tele Drive 🚀

Tele Drive is a multi-user Google Drive clone for Android that uses Telegram as free cloud storage.

## Features
- **Multi-user Login:** Every user logs in with their own Telegram account (OTP).
- **Private Storage:** Files are stored in an automatically created private Telegram channel.
- **Drive-style UI:** Modern Jetpack Compose UI with folders, file grid, and search.
- **Smart Metadata:** All file/folder names and structures are preserved in a local Room database.
- **Telegram Backend:** Leverages TDLib for high-performance file management.

## Project Setup & TDLib Requirements ⚠️

This project is pinned to **TDLib v1.8.0** for stability and JNI compatibility.

### 1. API Credentials
Get your `api_id` and `api_hash` from [my.telegram.org](https://my.telegram.org) and place them in `app/src/main/res/values/strings.xml`:
```xml
<string name="telegram_api_id">YOUR_API_ID</string>
<string name="telegram_api_hash">YOUR_API_HASH</string>
```

### 2. JNI Libraries (Required)
TDLib requires native binaries to function. You must place the `libtdjni.so` files (v1.8.0) into the following directory:
`app/src/main/jniLibs/[ABI]/libtdjni.so`

**Supported ABIs:**
- `armeabi-v7a`
- `arm64-v8a`
- `x86`
- `x86_64`

### 3. Build Instructions
- Use Android Studio Hedgehog or newer.
- Build with Gradle: `./gradlew assembleDebug`
- ProGuard is enabled by default for release builds to protect JNI signatures.

## Tech Stack
- **UI:** Jetpack Compose (Material Design 3)
- **Database:** Room (SQLite)
- **Networking:** TDLib (Telegram Database Library)
- **Concurrency:** Kotlin Coroutines + Flow
- **Background Tasks:** WorkManager (for uploads)
- **Images:** Coil (thumbnail loading)

## License
MIT License
