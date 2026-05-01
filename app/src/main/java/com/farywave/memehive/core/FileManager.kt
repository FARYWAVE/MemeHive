package com.farywave.memehive.core

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

object FileManager {
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
}