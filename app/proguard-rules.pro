# GlassPro ProGuard Rules for Production Release

# Keep Coroutines
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# Keep Room DB
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.Migration
-dontwarn androidx.room.paging.**

# Keep OkHttp & WebSockets
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Models and JSON Serializers
-keep class com.glasspro.tracker.core.model.** { *; }
-keep class com.glasspro.tracker.data.db.** { *; }
