# Suyziss Gallery

A local-first Android gallery built with Kotlin, Jetpack Compose and Material 3.

## Current implementation

- Runtime media permission flow for Android 13+ and earlier versions.
- Reads actual image and video entries from MediaStore (including Android's selected-media access model).
- Lazy thumbnail grid powered by Coil.
- Filename filtering.
- Basic full-screen media viewer and file metadata.
- Dynamic Material You colors on Android 12+.

No account, backend, demo media or network access is required for the gallery core.

## Build

Requires JDK 17 and Gradle 8.9+.

```bash
gradle :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Roadmap

The 26-section master specification is the product acceptance baseline. Implementation is incremental; features are only marked complete after implementation and verification. Albums, persistent favorites, editor, Media3 playback, private encrypted storage, trash, indexing worker, expanded tests and release signing are not yet implemented in this foundation commit.
