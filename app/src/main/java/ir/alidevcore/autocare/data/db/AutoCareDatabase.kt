package ir.alidevcore.autocare.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.alidevcore.autocare.data.dao.*
import ir.alidevcore.autocare.data.model.*

@Database(
    entities = [
        Vehicle::class,
        MaintenanceItem::class,
        MaintenanceRecord::class,
        PlannedService::class,
        ReminderSetting::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AutoCareDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceItemDao(): MaintenanceItemDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao
    abstract fun plannedServiceDao(): PlannedServiceDao
    abstract fun reminderSettingDao(): ReminderSettingDao

    companion object {
        const val DATABASE_NAME = "autocare_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new table with nullable `date`
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `maintenance_records_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `date` INTEGER,
                        `km` INTEGER NOT NULL,
                        `cost` INTEGER,
                        `description` TEXT NOT NULL,
                        `parts` TEXT NOT NULL,
                        `workshop` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `maintenance_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // 2. Copy all existing records
                db.execSQL("""
                    INSERT INTO `maintenance_records_new` (`id`, `itemId`, `date`, `km`, `cost`, `description`, `parts`, `workshop`, `createdAt`)
                    SELECT `id`, `itemId`, `date`, `km`, `cost`, `description`, `parts`, `workshop`, `createdAt`
                    FROM `maintenance_records`
                """.trimIndent())

                // 3. Drop old table and rename new table
                db.execSQL("DROP TABLE `maintenance_records`")
                db.execSQL("ALTER TABLE `maintenance_records_new` RENAME TO `maintenance_records`")

                // 4. Recreate indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_itemId` ON `maintenance_records` (`itemId`)")
            }
        }
    }
}
