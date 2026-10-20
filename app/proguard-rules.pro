# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends androidx.hilt.work.HiltWorker

# Apache POI
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn com.github.luben.zstd.**
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }

# Models
-keep class ir.alidevcore.autocare.data.model.** { *; }
-keep class ir.alidevcore.autocare.data.dao.** { *; }
