package com.farywave.memehive.data.local.db.repository

import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.data.local.db.mapper.toEntity
import com.farywave.memehive.data.local.db.mapper.toMediaItem
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MediaItemRepository(
    private val mediaItemDao: MediaItemDao,
    private val resolveImage: (ImageEntity) -> Image,
) {
    fun observeMediaItems(collectionId: Long?): Flow<List<MediaItem>> {
        return mediaItemDao
            .observeByCollection(collectionId)
            .map { items -> items.map { it.toMediaItem(it.image?.let(resolveImage)) } }
            .distinctUntilChanged()
    }

    suspend fun getMediaItemById(id: Long): MediaItem? {
        return mediaItemDao.get(id)?.let { it.toMediaItem(it.image?.let(resolveImage)) }
    }

    suspend fun getMediaItemsByCollectionId(collectionId: Long): List<MediaItem> {
        return mediaItemDao.getByCollection(collectionId).map {
            it.toMediaItem(it.image?.let(resolveImage))
        }
    }

    suspend fun getMediaItemImageId(mediaItemId: Long): Long? {
        return mediaItemDao.getImageId(mediaItemId)
    }

    suspend fun getMediaItemImageIds(mediaItemIds: Set<Long>): Set<Long> {
        return mediaItemIds.chunked(1000).flatMap { mediaItemDao.getImageIds(it.toSet()) }.toSet()
    }

    suspend fun insertMediaItem(mediaItem: MediaItem): Long {
        return mediaItemDao.insert(mediaItem.toEntity())
    }

    suspend fun updateMediaItem(mediaItem: MediaItem) {
        val affectedCount = mediaItemDao.update(mediaItem.toEntity())
        if (affectedCount == 0)
            throw IllegalStateException("Failed to update media item: $mediaItem")
    }

    suspend fun deleteMediaItemsByIds(mediaItemIds: Set<Long>) {
        for (chunk in mediaItemIds.chunked(1000)) mediaItemDao.deleteByIds(chunk.toSet())
    }
}
