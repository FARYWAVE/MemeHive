package com.farywave.memehive.ui.use_case

import android.net.Uri
import androidx.core.net.toUri
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.core.collection_transfer.CollectionTransferTool
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CollectionTransferUseCases(
    val transferTool: CollectionTransferTool,
    val collectionUseCases: CollectionUseCases,
    val mediaItemUseCases: MediaItemUseCases
) {
    suspend fun importCollection(uri: Uri): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val (manifest, _) = transferTool.unzipCollection(uri)

                val collectionId = collectionUseCases.createCollection(
                    Collection(
                        id = 0,
                        name = manifest.collection.name,
                        cover = manifest.collection.coverPath?.let { Image(uri = File(it).toUri()) }, //TODO Use Uri
                        mediaItemCount = 0
                    )
                )

                val mediaItemIds = manifest.mediaItems.map { transferMediaItem ->
                    mediaItemUseCases.createMediaItem(transferMediaItem.toMediaItem(transferMediaItem.path?.toUri()))
                }

                collectionUseCases.addItemsToCollection(mediaItemIds.toSet(), collectionId)
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    suspend fun exportCollection(collection: Collection, outputUri: Uri) {
        transferTool.exportCollection(
            collection = collection,
            mediaItems = mediaItemUseCases.getByCollectionID(collection.id),
            outputUri = outputUri
        )
    }
}