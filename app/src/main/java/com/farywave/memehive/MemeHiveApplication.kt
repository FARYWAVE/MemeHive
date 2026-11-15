package com.farywave.memehive

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder

class MemeHiveApplication : Application(), ImageLoaderFactory {
    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                    PICKER_CHANNEL_ID,
                    getString(R.string.notification_picker_title),
                    NotificationManager.IMPORTANCE_LOW,
                )
                .apply {
                    description = getString(R.string.notification_picker_description)
                }

        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }

    companion object {
        const val PICKER_CHANNEL_ID = "picker_channel"
        const val PICKER_NOTIFICATION_ID = 1
    }
}
