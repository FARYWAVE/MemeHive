package com.farywave.memehive.data.local.db.repository

import androidx.room.Transaction
import com.farywave.memehive.data.local.db.dao.CollectionDao
import com.farywave.memehive.data.local.db.dao.CollectionEntryDao
import com.farywave.memehive.data.local.db.entity.CollectionEntryEntity
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.flow.map

class CollectionRepository(
    private val collectionDao: CollectionDao,
    private val collectionEntryDao: CollectionEntryDao,
) {
    suspend fun insertCollection(collection: Collection) =
        collectionDao.insertCollection(
            collection.toCollectionEntity()
        )

    suspend fun updateCollection(collection: Collection) =
        collectionDao.updateCollection(collection.toCollectionEntity())

    @Transaction
    suspend fun deleteCollection(collection: Collection) {
        collectionEntryDao.deleteByCollection(collection.id)
        collectionDao.deleteCollection(collection.toCollectionEntity())
    }

    suspend fun observeCollections() =
        collectionDao.getAllCollections().map { list ->
            list.map { it.toCollection() }
        }

    suspend fun insertCollectionEntry(collection: Collection, mediaItem: MediaItem) =
        collectionEntryDao.insertCollectionEntry(
            CollectionEntryEntity(collection.id, mediaItem.id)
        )

    suspend fun deleteCollectionEntry(collection: Collection, mediaItem: MediaItem) =
        collectionEntryDao.deleteCollectionEntry(CollectionEntryEntity(collection.id, mediaItem.id))
}