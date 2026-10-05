package com.farywave.memehive.ui.use_case

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.core.collection_transfer.CollectionTransferTool
import com.farywave.memehive.core.collection_transfer.mapper.toCollection
import com.farywave.memehive.core.collection_transfer.mapper.toMediaItem
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.ui.model.Collection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CollectionTransferUseCases(
    val db: MemeHiveDatabase,
    val transferTool: CollectionTransferTool,
    val collectionUseCases: CollectionUseCases,
    val mediaItemUseCases: MediaItemUseCases
) {
    suspend fun importCollection(uri: Uri): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val (manifest, _) = transferTool.unzipCollection(uri)
                db.withTransaction {
                    val collection = collectionUseCases.createCollection(
                        manifest.collection.toCollection(
                            cover = manifest.collection.coverPath?.let { File(it).toUri() }
                        )
                    )

                    val mediaItemIds = manifest.mediaItems.map { transferMediaItem ->
                        mediaItemUseCases.createMediaItem(transferMediaItem.toMediaItem(transferMediaItem.path?.toUri()))
                    }

                    collectionUseCases.addItemsToCollection(mediaItemIds.map { it.id }.toSet(), collection.id)
                }
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    suspend fun exportCollection(collection: Collection, outputUri: Uri) {
        transferTool.exportCollection(
            collection = collection,
            mediaItems = mediaItemUseCases.getByCollectionId(collection.id),
            outputUri = outputUri
        )
    }
}