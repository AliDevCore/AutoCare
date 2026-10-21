package ir.alidevcore.autocare.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import ir.alidevcore.autocare.data.model.PlanStatus
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
            val now = System.currentTimeMillis()

            for (reminder in allReminders) {
                // If reminder disabled, cancel any existing system alarm
                if (!reminder.isEnabled) {
                    reminderScheduler.cancelReminder(reminder.notificationId)
                    continue
                }

                val plan = maintenanceRepository.getServiceById(reminder.plannedServiceId)
                // If plan deleted, completed, cancelled, or missing targetDate, cancel alarm
                if (plan == null || plan.status != PlanStatus.PENDING || plan.targetDate == null) {
                    reminderScheduler.cancelReminder(reminder.notificationId)
                    continue
                }

                val targetDate = plan.targetDate
                val triggerTime = reminderScheduler.computeTriggerTime(targetDate, reminder.daysBefore)

                // If trigger time has already passed, cancel any pending alarm so it doesn't fire erroneously
                if (triggerTime <= now) {
                    reminderScheduler.cancelReminder(reminder.notificationId)
                    continue
                }

                val item = maintenanceRepository.getItemByIdOnce(plan.itemId)
                val vehicle = vehicleRepository.getVehicleByIdOnce(plan.vehicleId)

                if (item != null && vehicle != null) {
                    // Schedules or idempotently updates existing alarm with FLAG_UPDATE_CURRENT
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
