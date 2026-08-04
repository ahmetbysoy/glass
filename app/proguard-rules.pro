# Keep Room Database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Data Models & Models in core package
-keep class com.glasspro.tracker.core.model.** { *; }
-keep class com.glasspro.tracker.data.db.** { *; }

# Keep OkHttp & WebSocket rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.android.** { *; }

