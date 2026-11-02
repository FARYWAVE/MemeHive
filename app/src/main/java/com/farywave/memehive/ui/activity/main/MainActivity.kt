package com.farywave.memehive.ui.activity.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.R
import com.farywave.memehive.ui.activity.picker.PickerActivity
import com.farywave.memehive.ui.component.AppFeedback
import com.farywave.memehive.ui.component.FeedbackHost
import com.farywave.memehive.ui.navigation.AppNavigation
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val feedback = AppFeedback()
    private var copying = false
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showPickerNotification() else reportNotificationFailure()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingUri =
            if (savedInstanceState == null)
                try {
                    getUriFromIntent(intent)
                } catch (failure: Exception) {
                    Log.w("MainActivity", "Could not read shared image", failure)
                    feedback.show(R.string.error_image_unavailable)
                    null
                }
            else null
        setContent {
            MemeHiveTheme {
                FeedbackHost(feedback) {
                    enableEdgeToEdge(
                        navigationBarStyle =
                            SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb())
                    )
                    val defaultLabel = stringResource(R.string.label_media_item)
                    AppNavigation(
                        initialUri = incomingUri,
                        onCopyImage = { image ->
                            copyToClipboard(defaultLabel, image.id)
                        },
                        onEnableNotification = { value ->
                            if (value) enablePicker()
                        },
                    )
                }
            }
        }
    }

    private fun enablePicker() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                showPickerNotification()
            } else {

                feedback.attempt(R.string.error_notification_enable) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {

            showPickerNotification()
        }
    }

    private fun reportNotificationFailure() {
        feedback.show(
            R.string.error_notification_permission,
            R.string.action_settings,
            onAction = {
                feedback.attempt(R.string.error_settings_open) {
                    startActivity(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            .putExtra(
                                Settings.EXTRA_CHANNEL_ID,
                                MemeHiveApplication.PICKER_CHANNEL_ID,
                            )
                    )
                }
            },
        )
    }

    @SuppressLint("MissingPermission")
    private fun showPickerNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = manager.getNotificationChannel(MemeHiveApplication.PICKER_CHANNEL_ID)
        if (
            !NotificationManagerCompat.from(this).areNotificationsEnabled() ||
                channel?.importance == NotificationManager.IMPORTANCE_NONE
        ) {
            reportNotificationFailure()
            return
        }
        feedback.attempt(R.string.error_notification_enable) {
            val intent = Intent(this, PickerActivity::class.java)

            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

            val notification =
                NotificationCompat.Builder(
                        this,
                        MemeHiveApplication.PICKER_CHANNEL_ID,
                    )
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(this.getString(R.string.app_name))
                    .setContentText(this.getString(R.string.notification_picker_message))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()

            NotificationManagerCompat.from(this)
                .notify(
                    MemeHiveApplication.PICKER_NOTIFICATION_ID,
                    notification,
                )
            feedback.show(R.string.success_picker_enabled)
        }
    }

    private fun copyToClipboard(label: String, imageId: Long) {
        if (copying) return
        copying = true
        lifecycleScope.launch {
            try {
                val useCases =
                    (applicationContext as MemeHiveApplication).container.mediaItemUseCases
                val uri = useCases.getImageSharingUri(imageId)
                val clip =
                    withContext(Dispatchers.IO) {
                        ClipData.newUri(contentResolver, label, uri)
                    }
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(clip)
                feedback.show(R.string.success_image_copied)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("MainActivity", "Could not copy image", error)
                feedback.show(R.string.error_image_copy)
            } finally {
                copying = false
            }
        }
    }

    private fun getUriFromIntent(intent: Intent): Uri? {
        if (intent.type?.startsWith("image/") != true) return null

        return when (intent.action) {
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java,
                )

            Intent.ACTION_VIEW,
            Intent.ACTION_EDIT -> intent.data
            else -> null
        }
    }
}
