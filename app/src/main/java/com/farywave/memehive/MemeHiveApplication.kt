package com.farywave.memehive

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class MemeHiveApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            PICKER_CHANNEL_ID,
            "Picker",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Picker access notification"
        }

        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }

    companion object {
        const val PICKER_CHANNEL_ID = "picker_channel"
        const val PICKER_NOTIFICATION_ID = 1
    }
}