package com.farywave.memehive.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.farywave.memehive.R
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
    fun formatBytes(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes >= gb -> "${(bytes / gb).toInt()} ${stringResource(R.string.gigabyte)}"
            bytes >= mb -> "${(bytes / mb).toInt()} ${stringResource(R.string.megabyte)}"
            bytes >= kb -> "${(bytes / kb).toInt()} ${stringResource(R.string.kilobyte)}"
            else -> "$bytes ${stringResource(R.string.__byte)}"
        }
    }
}