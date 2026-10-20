package ir.alidevcore.autocare.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.alidevcore.autocare.data.repository.MaintenanceRepository
import ir.alidevcore.autocare.data.repository.ReminderRepository
import ir.alidevcore.autocare.data.repository.VehicleRepository
import ir.alidevcore.autocare.notification.ReminderScheduler

@HiltWorker
class RescheduleRemindersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val allReminders = reminderRepository.getAllReminders()
            for (reminder in allReminders) {
                if (!reminder.isEnabled) continue
                val plan = maintenanceRepository.getServiceById(reminder.plannedServiceId) ?: continue
                val targetDate = plan.targetDate ?: continue
                val item = maintenanceRepository.getItemByIdOnce(plan.itemId) ?: continue
                val vehicle = vehicleRepository.getVehicleByIdOnce(plan.vehicleId) ?: continue

                val triggerTime = reminderScheduler.computeTriggerTime(targetDate, reminder.daysBefore)
                if (triggerTime > System.currentTimeMillis()) {
                    reminderScheduler.scheduleReminder(
                        notificationId = reminder.notificationId,
                        triggerAtMs = triggerTime,
                        vehicleName = vehicle.name,
                        serviceName = item.name,
                        vehicleId = vehicle.id,
                        daysUntilDue = reminder.daysBefore
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
