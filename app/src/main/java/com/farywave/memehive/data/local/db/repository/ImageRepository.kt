package com.farywave.memehive.data.local.db.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.core.extension.sha256
import com.farywave.memehive.data.local.db.dao.ImageDao
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.mapper.toModel
import com.farywave.memehive.data.local.storage.ImageChanges
import com.farywave.memehive.domain.model.Image
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class ImageRepository(
    private val context: Context,
    private val imageDao: ImageDao,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Tracks new image files for one operation. Put the outermost db.withTransaction inside
     * this block and pass its tracker to each creation call. Only tracked files without a
     * database row are removed.
     */
    suspend fun <T> withImageChanges(block: suspend (ImageChanges) -> T): T =
        ImageChanges.track(
            ioDispatcher = ioDispatcher,
            isStored = imageDao::existsByPath,
            onCleanupFailure = { file, error ->
                Log.w("ImageRepository", "Failed to clean up image: $file", error)
            },
            block = block,
        )

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

        withContext(ioDispatcher) {
            for (path in pathsToDelete) {
                ensureActive()
                val file = File(path)
                if (!file.delete() && file.exists()) {
                    Log.w("ImageRepository", "Failed to delete unused file: $path")
                }
            }
        }

        return result
    }

    suspend fun insertOrGet(uri: Uri): Image {
        val sha256 = uri.sha256(context, ioDispatcher)

        val imageEntity = imageDao.getBySHA256(sha256)
        if (imageEntity != null) return imageEntity.toModel()

        val file = copyToInternalStorage(uri)
        val id = imageDao.insert(ImageEntity(0, file.path, sha256))

        return Image(id, file.toUri())
    }

    /** Reused images are not tracked; a new copy is recorded before copying or inserting can fail. */
    suspend fun insertOrGet(uri: Uri, changes: ImageChanges): Image {
        val sha256 = uri.sha256(context, ioDispatcher)

        val imageEntity = imageDao.getBySHA256(sha256)
        if (imageEntity != null) return imageEntity.toModel()

        val file = DeviceTools.copyToInternalStorage(
            context = context,
            uri = uri,
            ioDispatcher = ioDispatcher,
            onFileCreated = changes::recordCreated,
        )
        val id = imageDao.insert(ImageEntity(0, file.path, sha256))

        return Image(id, file.toUri())
    }

    suspend fun copyToInternalStorage(uri: Uri): File =
        DeviceTools.copyToInternalStorage(context, uri, ioDispatcher)
}
