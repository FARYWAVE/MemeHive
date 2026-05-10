package com.farywave.memehive.core

import android.Manifest
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.R
import com.farywave.memehive.ui.picker.PickerActivity
import java.io.File
import java.io.IOException

object DeviceTools {
    fun copyToInternalStorage(context: Context, uri: Uri): File {
        val resolver = context.contentResolver

        val mimeType = resolver.getType(uri)
        val extFromMime = mimeType?.substringAfter("/")
        val extFromName = getFileName(context, uri)?.substringAfterLast('.', "")

        val extension = extFromMime ?: extFromName ?: "jpg"

        val file = File(
            context.filesDir,
            "image_${System.currentTimeMillis()}.$extension"
        )

        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Can't open input stream")

        if (!file.exists() || file.length() == 0L) {
            throw IOException("Failed to copy file")
        }

        return file
    }

    fun deleteFromInternalStorage(file: File) {
        file.delete()
    }

    fun getFileName(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val index = it.getColumnIndex("_display_name")
            if (it.moveToFirst() && index != -1) {
                it.getString(index)
            } else null
        }
    }

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName}"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun dirSize(dir: File): Long {
        return dir.walkBottomUp().sumOf { it.length() }
    }

    fun openLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }


    @Composable
    fun requestMedia(onGranted: (uri: Uri?) -> Unit) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onGranted(uri)
    }

    @Composable
    fun requestMultipleMedia(onGranted: (uris: List<Uri>) -> Unit) =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->
            onGranted(uris)
        }

    fun showPickerNotification(
        context: Context,
    ) {
        val intent = Intent(context, PickerActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or
                    PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification =
            NotificationCompat.Builder(
                context,
                MemeHiveApplication.PICKER_CHANNEL_ID
            )
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.picket_notification_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        NotificationManagerCompat.from(context)
            .notify(
                MemeHiveApplication.PICKER_NOTIFICATION_ID,
                notification
            )
    }

    fun hidePickerNotification(context: Context) {

        NotificationManagerCompat.from(context)
            .cancel(MemeHiveApplication.PICKER_NOTIFICATION_ID)
    }
}