package ir.alidevcore.autocare.data.dao

import androidx.room.*
import ir.alidevcore.autocare.data.model.ReminderSetting
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderSettingDao {
    @Query("SELECT * FROM reminder_settings WHERE plannedServiceId = :plannedServiceId")
    fun getRemindersForService(plannedServiceId: Long): Flow<List<ReminderSetting>>

    @Query("SELECT * FROM reminder_settings WHERE plannedServiceId = :plannedServiceId")
    suspend fun getRemindersForServiceOnce(plannedServiceId: Long): List<ReminderSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderSetting): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderSetting>)

    @Update
    suspend fun updateReminder(reminder: ReminderSetting)

    @Delete
    suspend fun deleteReminder(reminder: ReminderSetting)

    @Query("DELETE FROM reminder_settings WHERE plannedServiceId = :plannedServiceId")
    suspend fun deleteRemindersForService(plannedServiceId: Long)

    @Query("SELECT * FROM reminder_settings")
    suspend fun getAllReminders(): List<ReminderSetting>
}
