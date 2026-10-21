package com.farywave.memehive.data.local.db.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.farywave.memehive.core.extension.sha256
import com.farywave.memehive.data.local.db.dao.ImageDao
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.mapper.toModel
import com.farywave.memehive.data.local.storage.ImageChanges
import com.farywave.memehive.data.local.storage.ImageStorage
import com.farywave.memehive.domain.model.Image
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException

class ImageRepository(
    private val context: Context,
    private val imageDao: ImageDao,
    private val ioDispatcher: CoroutineDispatcher,
    private val storage: ImageStorage
) {
    fun toModel(image: ImageEntity): Image = image.toModel(storage.resolve(image.name).toUri())

    suspend fun sharingUri(imageId: Long): Uri {
        val image = imageDao.getById(imageId) ?: throw IOException("Image no longer exists")
        return storage.sharingUri(image.name)
    }

    suspend fun <T> withImageChanges(block: suspend (ImageChanges) -> T): T =
        ImageChanges.track(
            ioDispatcher = ioDispatcher,
            isStored = imageDao::existsByName,
            onCleanupFailure = { file, error ->
                Log.w("ImageRepository", "Failed to clean up image: $file", error)
            },
            block = block,
        )

    suspend fun <T> withImageCleanup(
        block: suspend (cleanup: suspend (Set<Long?>) -> Unit) -> T,
    ): T {
        val imagesToDelete = mutableSetOf<String>()

        val result = block { imageIds ->
            imageIds.forEach { imageId ->
                if (imageId != null && imageDao.getUsageCount(imageId) == 0) {
                    imageDao.getById(imageId)?.let { image ->
                        imageDao.delete(imageId)
                        imagesToDelete += image.name
                    }
                }
            }
        }

        withContext(NonCancellable + ioDispatcher) {
            for (name in imagesToDelete) {
                try {
                    storage.delete(name)
                } catch (error: Exception) {
                    Log.w("ImageRepository", "Failed to delete unused image: $name", error)
                }
            }
        }

        return result
    }

    suspend fun insertOrGet(uri: Uri): Image = withImageChanges { changes ->
        insertOrGet(uri, changes)
    }

    suspend fun insertOrGet(uri: Uri, changes: ImageChanges): Image {
        val sha256 = uri.sha256(context, ioDispatcher)

        val imageEntity = imageDao.getBySHA256(sha256)
        if (imageEntity != null) return toModel(imageEntity)

        val file = storage.copy(
            uri = uri,
            onFileCreated = changes::recordCreated,
        )
        val id = imageDao.insert(ImageEntity(0, file.name, sha256))

        return Image(id, file.toUri())
    }
}
