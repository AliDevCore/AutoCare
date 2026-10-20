package ir.alidevcore.autocare.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import ir.alidevcore.autocare.MainActivity
import ir.alidevcore.autocare.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_SERVICE_REMINDER = "service_reminder"
        const val CHANNEL_OVERDUE = "service_overdue"
        const val NOTIFICATION_ID_OFFSET = 10000
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_SERVICE_REMINDER,
                "یادآوری سرویس",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "یادآوری برای سرویس‌های برنامه‌ریزی‌شده"
            }

            val overdueChannel = NotificationChannel(
                CHANNEL_OVERDUE,
                "سرویس عقب‌افتاده",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اطلاع‌رسانی برای سرویس‌های عقب‌افتاده"
            }

            manager.createNotificationChannels(listOf(reminderChannel, overdueChannel))
        }
    }

    fun showServiceReminder(
        notificationId: Int,
        vehicleName: String,
        serviceName: String,
        daysUntilDue: Int,
        vehicleId: Long
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("vehicleId", vehicleId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when {
            daysUntilDue == 0 -> "امروز موعد سرویس $serviceName است"
            daysUntilDue == 1 -> "فردا موعد سرویس $serviceName است"
            daysUntilDue > 0 -> "تا $daysUntilDue روز دیگر موعد سرویس $serviceName است"
            else -> "موعد سرویس $serviceName گذشته است"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SERVICE_REMINDER)
            .setSmallIcon(R.drawable.ic_car_notification)
            .setContentTitle("یادآوری سرویس - $vehicleName")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}
