package ir.alidevcore.autocare.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ir.alidevcore.autocare.receiver.AlarmReceiver

class ReminderScheduler(private val context: Context) {

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_VEHICLE_NAME = "vehicle_name"
        const val EXTRA_SERVICE_NAME = "service_name"
        const val EXTRA_VEHICLE_ID = "vehicle_id"
        const val EXTRA_DAYS_UNTIL = "days_until"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(
        notificationId: Int,
        triggerAtMs: Long,
        vehicleName: String,
        serviceName: String,
        vehicleId: Long,
        daysUntilDue: Int
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_VEHICLE_NAME, vehicleName)
            putExtra(EXTRA_SERVICE_NAME, serviceName)
            putExtra(EXTRA_VEHICLE_ID, vehicleId)
            putExtra(EXTRA_DAYS_UNTIL, daysUntilDue)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // If we can't schedule exact alarms, use inexact
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        }
    }

    fun cancelReminder(notificationId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    /**
     * Computes trigger time for a reminder based on target date and days before
     */
    fun computeTriggerTime(targetDateMs: Long, daysBefore: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = targetDateMs
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysBefore)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 9) // 9 AM
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
