# Keep Media3 player components and reflection entry points.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# WorkManager instantiates workers by class name.
-keep class com.suyziss.gallery.data.MediaIndexWorker { *; }

# Compose and Kotlin metadata are handled by their standard R8 rules.
