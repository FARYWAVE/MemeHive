package com.farywave.memehive.data.local.db.repository

import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.data.local.db.dao.CollectionDao
import com.farywave.memehive.data.local.db.dao.CollectionEntryDao
import com.farywave.memehive.data.local.db.entity.CollectionEntryEntity
import com.farywave.memehive.data.local.db.mapper.toEntity
import com.farywave.memehive.data.local.db.mapper.toModel
import com.farywave.memehive.domain.model.Collection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CollectionRepository(
    private val collectionDao: CollectionDao,
    private val resolveImage: (ImageEntity) -> Image,
    private val collectionEntryDao: CollectionEntryDao,
) {
    fun getAllCollections(): Flow<List<Collection>> {
        return collectionDao.getAll()
            .map { collections ->
                collections.map { it.toModel(it.coverImage?.let(resolveImage)) }
            }
            .distinctUntilChanged()
    }

    suspend fun getCollectionCoverId(collectionId: Long): Long? {
        return collectionDao.getCoverId(collectionId)
    }

    suspend fun getCollectionIdsByMediaItem(mediaItemId: Long): Set<Long> {
        return collectionEntryDao.getByMediaItem(mediaItemId).map { it.collectionId }.toSet()
    }

    suspend fun insertCollection(collection: Collection): Long {
        return collectionDao.insert(
            collection.toEntity()
        )
    }

    suspend fun insertCollectionEntries(mediaItemIds: Set<Long>, collectionId: Long): List<Long> {
        return collectionEntryDao.insert(
            mediaItemIds.map { CollectionEntryEntity(collectionId, it) }
        )
    }

    suspend fun addItemToCollections(mediaItemId: Long, collectionIds: Set<Long>): List<Long> {
        return collectionEntryDao.insert(
            collectionIds.map { CollectionEntryEntity(it, mediaItemId) }
        )
    }

    suspend fun updateCollection(collection: Collection) {
        val affectedCount = collectionDao.update(
            collection.toEntity()
        )
        if (affectedCount == 0) throw IllegalStateException("Failed to update collection: ${collection.id}")
    }

    suspend fun setCollectionCover(collectionId: Long, imageId: Long) {
        val affectedCount = collectionDao.setCover(collectionId, imageId)
        if (affectedCount == 0) throw IllegalStateException("Failed to set collection cover: $collectionId")
    }

    suspend fun deleteCollection(collection: Collection) {
        collectionEntryDao.deleteByCollection(collection.id)
        collectionDao.delete(collection.id)
    }

    suspend fun deleteCollectionEntriesByItem(mediaItemId: Long) {
        collectionEntryDao.deleteByMediaItem(mediaItemId)
    }
}
