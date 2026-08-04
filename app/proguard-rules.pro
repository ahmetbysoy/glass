# ProGuard kuralları - GlassPro
-keep public class * extends android.app.Application
-keep public class * extends androidx.lifecycle.AndroidViewModel
-dontwarn kotlinx.coroutines.**
-keepclassmembers class * { @androidx.room.* <fields>; @androidx.room.* <methods>; }
