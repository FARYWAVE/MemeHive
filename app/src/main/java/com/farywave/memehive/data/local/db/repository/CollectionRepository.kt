package com.farywave.memehive.data.local.db.repository

import android.content.Context
import androidx.room.Transaction
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.data.local.db.dao.CollectionDao
import com.farywave.memehive.data.local.db.dao.CollectionEntryDao
import com.farywave.memehive.data.local.db.entity.CollectionEntryEntity
import com.farywave.memehive.data.local.db.mapper.toEntity
import com.farywave.memehive.data.local.db.mapper.toModel
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.collections.emptyList

class CollectionRepository(
    private val collectionDao: CollectionDao,
    private val collectionEntryDao: CollectionEntryDao,
) {
    suspend fun insertCollection(collection: Collection) =
        collectionDao.insert(
            collection.toEntity()
        )

    suspend fun updateCollection(collection: Collection) =
        collectionDao.update(
            collection.toEntity()
        )

    suspend fun deleteCollection(collection: Collection) {
        collectionEntryDao.deleteByCollection(collection.id)
        collectionDao.delete(collection.id)
    }

    fun getAllCollections() =
        collectionDao.getAll()
            .flatMapLatest { collections ->

                if (collections.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val flows = collections.map { collection ->
                        collectionEntryDao.getEntryCount(collection.collection.id)
                            .map { count ->
                                collection.toModel(count)
                            }
                    }

                    combine(flows) { it.toList() }
                }
            }

    suspend fun insertCollectionEntries(mediaItemIds: Set<Long>, collectionId: Long) =
        collectionEntryDao.insert(
            mediaItemIds.map { CollectionEntryEntity(collectionId, it)}
        )

    suspend fun getCollectionIdsByMediaItem(mediaItemId: Long) =
        collectionEntryDao.getByMediaItem(mediaItemId).map { it.collectionId }.toSet()
    suspend fun deleteCollectionEntriesByItem(mediaItemId: Long) {
        collectionEntryDao.deleteByMediaItem(mediaItemId)
    }

    suspend fun addItemToCollections(mediaItemId: Long, collectionIds: Set<Long>) =
        collectionEntryDao.insert(
            collectionIds.map { CollectionEntryEntity(it, mediaItemId) }
        )

    suspend fun setCollectionCover(collectionID: Long, imageID: Long) = collectionDao.setCover(collectionID, imageID)
}