package com.farywave.memehive.core

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.R
import com.farywave.memehive.ui.activity.picker.PickerActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

object DeviceTools {
    suspend fun copyToInternalStorage(
        context: Context,
        uri: Uri,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        onFileCreated: (File) -> Unit = {},
    ): File = withContext(ioDispatcher) {
        val resolver = context.contentResolver

        val mimeType = resolver.getType(uri)
        val extFromMime = mimeType?.substringAfter("/")
        val extFromName = getFileName(context, uri, ioDispatcher)?.substringAfterLast('.', "")

        val extension = extFromMime ?: extFromName ?: "jpg"

        copyImageToDirectory(context.filesDir, extension, ioDispatcher, onFileCreated) {
            resolver.openInputStream(uri) ?: throw IOException("Could not open the source image.")
        }
    }

    suspend fun copyToInternalStorage(
        context: Context,
        sourceFile: File,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): File = withContext(ioDispatcher) {
        copyImageToDirectory(context.filesDir, sourceFile.extension, ioDispatcher) {
            sourceFile.inputStream()
        }
    }

    internal suspend fun copyImageToDirectory(
        directory: File,
        extension: String,
        ioDispatcher: CoroutineDispatcher,
        onFileCreated: (File) -> Unit = {},
        openInput: () -> InputStream,
    ): File = withContext(ioDispatcher) {
        openInput().use { input ->
            val file = File.createTempFile("image_", ".${extension.ifBlank { "jpg" }}", directory)
            // Register inside the IO block: cancellation can discard a withContext return value.
            onFileCreated(file)
            file.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    ensureActive()
                    val count = input.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
            }
            if (file.length() == 0L) throw IOException("Failed to copy file")
            file
        }
    }

    suspend fun deleteFromInternalStorage(
        file: File,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        withContext(ioDispatcher) { file.delete() }
    }

    suspend fun getFileName(
        context: Context,
        uri: Uri,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): String? = withContext(ioDispatcher) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
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

    suspend fun dirSize(
        dir: File,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Long = withContext(ioDispatcher) {
        dir.walkBottomUp().sumOf {
            ensureActive()
            it.length()
        }
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

    @SuppressLint("MissingPermission")
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
