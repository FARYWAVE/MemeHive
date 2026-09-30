package com.farywave.memehive.data.local.db.repository

import android.content.Context
import androidx.room.Transaction
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.data.local.db.dao.CollectionEntryDao
import com.farywave.memehive.data.local.db.dao.MediaItemDao
import com.farywave.memehive.data.local.db.dao.MediaItemTagDao
import com.farywave.memehive.data.local.db.dao.MediaItemTrigramDao
import com.farywave.memehive.data.local.db.dao.TagDao
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.MediaItemTrigramEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import com.farywave.memehive.ui.model.Collection
import com.farywave.memehive.ui.model.MediaItem

class MediaItemRepository(
    private val mediaItemDao: MediaItemDao,
) {
    suspend fun insertMediaItem(mediaItem: MediaItem) = mediaItemDao.insert(mediaItem.toEntity())
    suspend fun getMediaItemById(id: Long) = mediaItemDao.get(id)?.toMediaItem()

    suspend fun getMediaItemsByCollectionID(collectionID: Long) = mediaItemDao.getByCollection(collectionID)

    suspend fun updateMediaItem(mediaItem: MediaItem) = mediaItemDao.update(mediaItem.toEntity())

    suspend fun deleteMediaItem(mediaItem: MediaItem) = mediaItemDao.delete(mediaItem.id)

    suspend fun deleteMediaItemsByIDs(mediaItemIDs: List<Long>) = mediaItemDao.deleteByIDs(mediaItemIDs)
}