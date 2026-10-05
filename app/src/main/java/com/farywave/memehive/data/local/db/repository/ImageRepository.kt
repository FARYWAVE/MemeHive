package com.farywave.memehive.data.local.db.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.farywave.memehive.core.extension.sha256
import com.farywave.memehive.data.local.db.dao.ImageDao
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.mapper.toModel
import com.farywave.memehive.ui.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ImageRepository(
    private val context: Context,
    private val imageDao: ImageDao,
) {
    suspend fun <T> withImageCleanup(
        block: suspend (cleanup: suspend (Set<Long?>) -> Unit) -> T,
    ): T {
        val pathsToDelete = mutableSetOf<String>()

        val result = block { imageIds ->
            imageIds.forEach { imageId ->
                if (imageId != null && imageDao.getUsageCount(imageId) == 0) {
                    imageDao.getById(imageId)?.let { image ->
                        imageDao.delete(imageId)
                        pathsToDelete += image.path
                    }
                }
            }
        }

        withContext(Dispatchers.IO) {
            for (path in pathsToDelete) {
                val file = File(path)
                if (!file.delete() && file.exists()) {
                    Log.w("ImageRepository", "Failed to delete unused file: $path")
                }
            }
        }

        return result
    }

    suspend fun insertOrGet(uri: Uri): Image {
        val sha256 = uri.sha256(context)

        val imageEntity = imageDao.getBySHA256(sha256)
        if (imageEntity != null) return imageEntity.toModel()

        val file = copyToInternalStorage(uri)
        val id = imageDao.insert(ImageEntity(0, file.path, sha256))

        return Image(id, file.toUri())
    }

    fun copyToInternalStorage(uri: Uri): File {
        val resolver = context.contentResolver

        val mimeType = resolver.getType(uri)
        val extFromMime = mimeType?.substringAfter("/")
        val extFromName = getFileName(uri)?.substringAfterLast('.', "")

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

    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val index = it.getColumnIndex("_display_name")
            if (it.moveToFirst() && index != -1) {
                it.getString(index)
            } else null
        }
    }
}