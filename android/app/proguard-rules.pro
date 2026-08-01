# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class com.dylandos.iptv.data.db.AppDatabase { *; }

# --- Hilt / Dagger ---
-dontwarn dagger.hilt.android.**
-keep class dagger.hilt.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.qualifiers.* <methods>;
}

# --- Kotlinx Coroutines ---
-dontwarn kotlinx.coroutines.**

# --- LibVLC (org.videolan.android:libvlc-all) ---
-dontwarn org.videolan.**
-keep class org.videolan.libvlc.** { *; }
-keepclassmembers class org.videolan.libvlc.** { *; }

# --- MPV (is.xyz.mpv) ---
-dontwarn is.xyz.mpv.**
-keep class is.xyz.mpv.** { *; }

# --- Compose ---
-dontwarn androidx.compose.**
-keepclassmembers class **ComposableKt { *; }

# --- Coil ---
-dontwarn coil.**
-keep class coil.** { *; }

# --- OkHttp (transitive) ---
-dontwarn okhttp3.**
-dontwarn okio.**

# --- DataStore / Protobuf ---
-dontwarn android.datastore.**
