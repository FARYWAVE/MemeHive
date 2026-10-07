package com.farywave.memehive.ui.use_case

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.farywave.memehive.core.collection_transfer.CollectionTransferTool
import com.farywave.memehive.core.collection_transfer.mapper.toCollection
import com.farywave.memehive.core.collection_transfer.mapper.toMediaItem
import com.farywave.memehive.data.local.db.MemeHiveDatabase
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.domain.model.Collection
import java.io.File

class CollectionTransferUseCases(
    private val db: MemeHiveDatabase,
    private val transferTool: CollectionTransferTool,
    private val collectionUseCases: CollectionUseCases,
    private val mediaItemUseCases: MediaItemUseCases,
    private val imageRepository: ImageRepository,
) {
    suspend fun importCollection(uri: Uri) {
        val (manifest, directory) = transferTool.unzipCollection(uri)
        imageRepository.withImageChanges { changes ->
            db.withTransaction {
                val collection = collectionUseCases.createCollection(
                    collection = manifest.collection.toCollection(
                        cover = manifest.collection.coverPath?.let { File(directory, it).toUri() }
                    ),
                    changes = changes,
                )

                val mediaItems = manifest.mediaItems.map { transferMediaItem ->
                    mediaItemUseCases.createMediaItem(
                        mediaItem = transferMediaItem.toMediaItem(
                            image = transferMediaItem.path?.let { File(directory, it).toUri() }
                        ),
                        changes = changes,
                    )
                }

                collectionUseCases.addItemsToCollection(mediaItems.map { it.id }.toSet(), collection.id)
            }
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
