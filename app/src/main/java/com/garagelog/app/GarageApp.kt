package com.garagelog.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.garagelog.app.worker.ReminderWorker

class GarageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ReminderWorker.schedule(this)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания о ТО и пробеге",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Уведомления о ближайшем обслуживании и необходимости обновить пробег"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "garage_reminders"
    }
}
