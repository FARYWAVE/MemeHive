package com.farywave.memehive.data.local.db.repository

import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.mapper.toEntity
import com.farywave.memehive.data.local.db.mapper.toMediaItem
import com.farywave.memehive.ui.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MediaItemRepository(
    private val mediaItemDao: MediaItemDao,
) {
    fun observeMediaItems(collectionId: Long?): Flow<List<MediaItem>> =
        mediaItemDao.observeByCollection(collectionId)
            .map { items -> items.map { it.toMediaItem() } }
            .distinctUntilChanged()

    suspend fun insertMediaItem(mediaItem: MediaItem) = mediaItemDao.insert(mediaItem.toEntity())
    suspend fun getMediaItemById(id: Long) = mediaItemDao.get(id)?.toMediaItem()

    suspend fun getMediaItemsByCollectionId(collectionId: Long) = mediaItemDao.getByCollection(collectionId)

    suspend fun updateMediaItem(mediaItem: MediaItem) = mediaItemDao.update(mediaItem.toEntity())

    suspend fun deleteMediaItem(mediaItem: MediaItem) = mediaItemDao.delete(mediaItem.id)

    suspend fun deleteMediaItemsByIds(mediaItemIds: Set<Long>) = mediaItemDao.deleteByIds(mediaItemIds)

    suspend fun getMediaItemImageId(mediaItemId: Long) = mediaItemDao.getImageId(mediaItemId)

    suspend fun getMediaItemImageIds(mediaItemIds: Set<Long>) = mediaItemDao.getImageIds(mediaItemIds).toSet()
}