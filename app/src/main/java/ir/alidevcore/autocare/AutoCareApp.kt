package ir.alidevcore.autocare

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ir.alidevcore.autocare.notification.NotificationHelper

@HiltAndroidApp
class AutoCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper(this).createNotificationChannels()
    }
}
