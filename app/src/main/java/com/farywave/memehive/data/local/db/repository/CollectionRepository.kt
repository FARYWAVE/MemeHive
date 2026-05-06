package com.farywave.memehive.data.local.db.repository

import androidx.room.Transaction
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.data.local.db.dao.CollectionDao
import com.farywave.memehive.data.local.db.dao.CollectionEntryDao
import com.farywave.memehive.data.local.db.entity.CollectionEntryEntity
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
        collection.cover?.let { DeviceTools.deleteFromInternalStorage(it) }
    }

    fun observeCollections() =
        collectionDao.getAllCollections()
            .flatMapLatest { collections ->
                val flows = collections.map { collection ->
                    collectionEntryDao.getEntryCount(collection.id)
                        .map { count ->
                            collection.toCollection(count)
                        }
                }

                combine(flows) { it.toList() }
            }

    suspend fun insertCollectionEntry(collection: Collection, mediaItem: MediaItem) =
        collectionEntryDao.insertCollectionEntry(
            CollectionEntryEntity(collection.id, mediaItem.id)
        )

    suspend fun deleteCollectionEntry(collection: Collection, mediaItem: MediaItem) =
        collectionEntryDao.deleteCollectionEntry(CollectionEntryEntity(collection.id, mediaItem.id))
}