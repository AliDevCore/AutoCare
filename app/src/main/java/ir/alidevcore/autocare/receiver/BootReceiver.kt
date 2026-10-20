package ir.alidevcore.autocare.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ir.alidevcore.autocare.worker.RescheduleRemindersWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Reschedule all reminders after reboot
            val workRequest = OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
