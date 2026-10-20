package ir.alidevcore.autocare.data.repository

import ir.alidevcore.autocare.data.dao.ReminderSettingDao
import ir.alidevcore.autocare.data.model.ReminderSetting
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderSettingDao
) {
    fun getRemindersForService(plannedServiceId: Long): Flow<List<ReminderSetting>> =
        reminderDao.getRemindersForService(plannedServiceId)

    suspend fun getRemindersForServiceOnce(plannedServiceId: Long): List<ReminderSetting> =
        reminderDao.getRemindersForServiceOnce(plannedServiceId)

    suspend fun insertReminder(reminder: ReminderSetting): Long =
        reminderDao.insertReminder(reminder)

    suspend fun insertReminders(reminders: List<ReminderSetting>) =
        reminderDao.insertReminders(reminders)

    suspend fun updateReminder(reminder: ReminderSetting) =
        reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: ReminderSetting) =
        reminderDao.deleteReminder(reminder)

    suspend fun deleteRemindersForService(plannedServiceId: Long) =
        reminderDao.deleteRemindersForService(plannedServiceId)

    suspend fun getAllReminders(): List<ReminderSetting> =
        reminderDao.getAllReminders()
}
