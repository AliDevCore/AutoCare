# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends androidx.hilt.work.HiltWorker

# Models & DAOs
-keep class ir.alidevcore.autocare.data.model.** { *; }
-keep class ir.alidevcore.autocare.data.dao.** { *; }

# Apache POI and its transitive dependencies
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.osgi.framework.**
-dontwarn aQute.**
-dontwarn com.graphbuilder.**
-dontwarn org.apache.commons.**
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn net.sf.saxon.**
-dontwarn org.bouncycastle.**
-dontwarn com.github.luben.zstd.**
-dontwarn org.etsi.uri.**
-dontwarn javax.xml.namespace.**
-dontwarn javax.xml.parsers.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**

# Keep POI classes used by ExcelExporter
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep interface org.apache.poi.** { *; }
-keep interface org.apache.xmlbeans.** { *; }
