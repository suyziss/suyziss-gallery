# Suyziss Gallery

A local-first Android gallery built with Kotlin, Jetpack Compose and Material 3.

## Implemented

- Reads actual photo and video records from MediaStore.
- Android 13+ media permissions, including Android 14 selected-photo/video access with an explicit partial-access notice.
- Cursor-based pagination using MediaStore query arguments and a stable date/ID cursor.
- Search by filename, photo/video filters, sorting by date, name and file size, and a live result count.
- Lazy thumbnail grid with animated shimmer placeholders and video-duration labels.
- Full-screen viewer with horizontal navigation between loaded results, pinch zoom for photos and an embedded Media3 video player.
- Separate MediaRepository, FilterMediaUseCase and ViewModel layers.
- Local metadata cache (app-private file) and WorkManager background cache refresh.
- System light/dark theme with dynamic Material You colors on Android 12+.
- MediaStore permission/query error states, retry action and empty-search messaging.
- Unit tests for filtering/sorting and Compose launch tests.

No account, backend, demo media or network access is required for the gallery core. The cache stores file metadata and content URIs, not copies of private media.

## Build debug APK

Requires JDK 17 and Gradle 8.9.

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Signed release

Release builds are published when a version tag such as `v0.2.0` is pushed. Configure these GitHub repository Actions secrets first:

- `SIGNING_KEYSTORE_BASE64`: Base64-encoded Android release keystore.
- `SIGNING_STORE_PASSWORD`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`

Do not commit the keystore or passwords to the repository. Example tag:

```bash
git tag v0.2.0
git push origin v0.2.0
```

The workflow derives versionName/versionCode from the tag, builds a minified and signed APK, and creates a GitHub Release.

## Not implemented yet

- Encrypted private album with biometric authentication.
- In-app crop editor and non-destructive editing workflow.
- Persistent database-backed index and exhaustive indexing beyond the bounded metadata cache.
- Automated end-to-end permission grant/revocation scenarios.

These are intentionally listed as pending rather than exposed as non-working controls.
