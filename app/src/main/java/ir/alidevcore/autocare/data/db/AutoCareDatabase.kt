package ir.alidevcore.autocare.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
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
    }
}
