package com.farywave.memehive.ui.use_case

import android.net.Uri
import androidx.core.net.toUri
import com.farywave.memehive.core.collection_transfer.CollectionTransferTool
import com.farywave.memehive.data.local.db.repository.CollectionRepository
import com.farywave.memehive.data.local.db.repository.ImageRepository
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CollectionUseCases(
    val collectionRepository: CollectionRepository,
    val imageRepository: ImageRepository,
) {
    suspend fun createCollection(collection: Collection): Long {
        val image = collection.cover?.let { cover ->
            imageRepository.insertOrGet(cover.uri)
        }
        collection.cover = image
        return collectionRepository.insertCollection(collection)
    }

    suspend fun updateCollection(collection: Collection) {
        val image = collection.cover?.let { cover ->
            imageRepository.insertOrGet(cover.uri)
        }
        collection.cover = image
        collectionRepository.updateCollection(collection)
    }

    suspend fun deleteCollection(collection: Collection) {
        collectionRepository.deleteCollection(collection)
        collection.cover?.let { cover -> imageRepository.deleteIfUnused(cover.id) }
    }

    fun getAllCollections() = collectionRepository.getAllCollections()

    suspend fun addItemsToCollection(mediaItemIds: Set<Long>, collectionId: Long) =
        collectionRepository.insertCollectionEntries(mediaItemIds, collectionId)

    fun getCollectionsByMediaItem(mediaItemId: Long) =
        collectionRepository.getCollectionsByMediaItem(mediaItemId)

    suspend fun updateMediaItemCollections(mediaItemId: Long, collectionIds: List<Long>) {
        collectionRepository.deleteCollectionEntriesByItem(mediaItemId)
        collectionRepository.addItemToCollections(mediaItemId, collectionIds)
    }

    suspend fun setCollectionCover(collectionId: Long, imageID: Long) =
        collectionRepository.setCollectionCover(collectionId, imageID)
}