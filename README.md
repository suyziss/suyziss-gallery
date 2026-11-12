<div align="center">

  <img src="https://avatars.githubusercontent.com/u/347293953?v=4" width="112" height="112" alt="Suyziss profile photo" style="border-radius: 50%;" />

  <h1>Suyziss Gallery</h1>
  <p><strong>Private by design. Simple to use. Built for Android.</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android" />
    <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/Design-Material%203-6750A4" alt="Material 3" />
    <a href="https://github.com/suyziss/suyziss-gallery/actions/workflows/android.yml"><img src="https://github.com/suyziss/suyziss-gallery/actions/workflows/android.yml/badge.svg?branch=main" alt="Android CI" /></a>
  </p>

  <p>
    <a href="https://github.com/suyziss/suyziss-gallery/actions">Builds</a> ·
    <a href="https://github.com/suyziss/suyziss-gallery/issues">Issues</a> ·
    <a href="https://github.com/suyziss">Developer</a>
  </p>

</div>

---

## Overview

**Suyziss Gallery** is a local-first Android gallery for browsing photos and videos stored on the device. It uses the Android media library directly: no account, cloud service, backend, or sample media is needed.

The interface is built with **Jetpack Compose** and **Material 3**, with dynamic system colors on Android 12 and newer.

## Features

| Area | Current implementation |
|---|---|
| Media library | Reads real photos and videos from Android MediaStore |
| Permissions | Android 13+ media permissions and Android 14 selected-media access |
| Browsing | Cursor-based pagination, lazy thumbnail grid |
| Search | Filename search, media-type filters, live result count |
| Sorting | Date, name, and file size |
| Viewer | Full-screen horizontal pager with swipe navigation |
| Photo viewing | Pinch-to-zoom and pan |
| Video playback | Media3 ExoPlayer integration and duration labels |
| Visual feedback | Thumbnail shimmer, empty states, permission/query error states |
| Theme | Material 3 with system light/dark mode and dynamic colors |
| Background work | WorkManager cache refresh |
| Architecture | Repository, use case, ViewModel, and Compose UI layers |

### Privacy

- Media is read from the device through Android's media APIs.
- No account or remote backend is required for the gallery's core functionality.
- The metadata cache is stored in the app's private storage.
- The cache contains media metadata and content URIs, not duplicate copies of personal photos or videos.
- Android's selected-photo access is handled explicitly; the app does not assume it can access the entire library.

## Tech stack

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- AndroidX Lifecycle and ViewModel
- MediaStore
- Coil
- AndroidX Media3
- WorkManager
- Kotlin Coroutines

## Project structure

The project keeps platform/data operations separate from presentation logic:

- **Data** — MediaStore access, repository implementation, and local metadata cache.
- **Domain** — media filtering and sorting use cases.
- **Presentation** — ViewModel and Compose screens.
- **Background** — WorkManager tasks for cache refresh.

This separation is intended to keep business rules independent from individual UI components.

## Build locally

### Requirements

- JDK 17
- Gradle 8.9
- Android SDK with API 35

### Run unit tests and build a debug APK

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Install with ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Continuous integration

GitHub Actions runs the Android workflow on pushes to `main` and pull requests:

1. Runs unit tests.
2. Builds the debug APK.
3. Uploads the APK as a workflow artifact.
4. Runs instrumentation tests on an Android emulator after the debug job succeeds.

Open [GitHub Actions](https://github.com/suyziss/suyziss-gallery/actions) to inspect the latest run and download available artifacts.

## Signed releases

Pushing a version tag such as `v0.2.0` starts the release workflow. Before using it, configure these repository Actions secrets:

- `SIGNING_KEYSTORE_BASE64` — Base64-encoded Android keystore.
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Never commit the keystore or signing passwords to the repository.

Create and push a tag:

```bash
git tag v0.2.0
git push origin v0.2.0
```

The workflow derives the app version from the tag, builds a minified release APK, and publishes a GitHub Release.

## Roadmap

The following items are **not implemented yet** and are not presented as available features:

- Encrypted private album with biometric authentication.
- In-app crop editor and non-destructive photo editing.
- Persistent database-backed media index and exhaustive indexing beyond the current bounded metadata cache.
- Automated end-to-end tests for permission grant and revocation flows.

## Contributing and feedback

Bug reports and feature requests can be submitted through [GitHub Issues](https://github.com/suyziss/suyziss-gallery/issues).

---

<div align="center">

  **Made by [@suyziss](https://github.com/suyziss)**

  <sub>Suyziss Gallery · Android · Kotlin</sub>

</div>
