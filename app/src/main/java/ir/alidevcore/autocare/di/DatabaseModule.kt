package ir.alidevcore.autocare.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.alidevcore.autocare.data.dao.MaintenanceItemDao
import ir.alidevcore.autocare.data.dao.MaintenanceRecordDao
import ir.alidevcore.autocare.data.dao.PlannedServiceDao
import ir.alidevcore.autocare.data.dao.ReminderSettingDao
import ir.alidevcore.autocare.data.dao.VehicleDao
import ir.alidevcore.autocare.data.db.AutoCareDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AutoCareDatabase {
        return Room.databaseBuilder(
            context,
            AutoCareDatabase::class.java,
            AutoCareDatabase.DATABASE_NAME
        )
            .addMigrations(AutoCareDatabase.MIGRATION_1_2)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    try {
                        db.execSQL("UPDATE maintenance_records SET id = (SELECT COALESCE(MAX(id), 0) + 1 FROM maintenance_records WHERE id > 0) WHERE id <= 0")
                        db.execSQL("UPDATE planned_services SET id = (SELECT COALESCE(MAX(id), 0) + 1 FROM planned_services WHERE id > 0) WHERE id <= 0")
                    } catch (_: Exception) {}
                }
            })
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideVehicleDao(db: AutoCareDatabase): VehicleDao = db.vehicleDao()

    @Provides
    fun provideMaintenanceItemDao(db: AutoCareDatabase): MaintenanceItemDao = db.maintenanceItemDao()

    @Provides
    fun provideMaintenanceRecordDao(db: AutoCareDatabase): MaintenanceRecordDao = db.maintenanceRecordDao()

    @Provides
    fun providePlannedServiceDao(db: AutoCareDatabase): PlannedServiceDao = db.plannedServiceDao()

    @Provides
    fun provideReminderSettingDao(db: AutoCareDatabase): ReminderSettingDao = db.reminderSettingDao()
}
