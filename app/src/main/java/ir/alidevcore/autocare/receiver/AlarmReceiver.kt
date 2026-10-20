package ir.alidevcore.autocare.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.alidevcore.autocare.notification.NotificationHelper
import ir.alidevcore.autocare.notification.ReminderScheduler

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(ReminderScheduler.EXTRA_NOTIFICATION_ID, 0)
        val vehicleName = intent.getStringExtra(ReminderScheduler.EXTRA_VEHICLE_NAME) ?: ""
        val serviceName = intent.getStringExtra(ReminderScheduler.EXTRA_SERVICE_NAME) ?: ""
        val vehicleId = intent.getLongExtra(ReminderScheduler.EXTRA_VEHICLE_ID, 0L)
        val daysUntilDue = intent.getIntExtra(ReminderScheduler.EXTRA_DAYS_UNTIL, 0)

        NotificationHelper(context).showServiceReminder(
            notificationId, vehicleName, serviceName, daysUntilDue, vehicleId
        )
    }
}
