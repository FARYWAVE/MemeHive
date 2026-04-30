package com.farywave.memehive.core

import android.content.Context
import android.net.Uri
import java.io.File

object FileManager {
    fun copyToInternalStorage(context: Context, uri: Uri): File {
        val file = File(context.filesDir, "image_${System.currentTimeMillis()}.jpg")

        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file
    }

    fun deleteFromInternalStorage(file: File) {
        file.delete()
    }
}