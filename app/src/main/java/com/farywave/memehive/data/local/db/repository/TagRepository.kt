package com.farywave.memehive.data.local.db.repository

import com.farywave.memehive.data.local.db.dao.MediaItemTagDao
import com.farywave.memehive.data.local.db.dao.TagDao
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.ui.model.MediaItem

class TagRepository(
    val tagDao: TagDao,
    val mediaItemTagDao: MediaItemTagDao
) {
    suspend fun getAll() = tagDao.getAll()

    suspend fun getIDsByNames(names: List<String>) = tagDao.getIDsByNames(names);

    suspend fun getMediaItemIDsByTagIDs(tagIDs: List<Long>) =
        mediaItemTagDao.getMediaItemIDsByTags(tagIDs)

    suspend fun getByName(name: String) = tagDao.getByName(name)

    suspend fun insert(name: String, mediaItemID: Long? = null) {
        val tag = getByName(name)?.id ?: tagDao.insert(TagEntity(name = name))
        if (mediaItemID != null) mediaItemTagDao.insert(MediaItemTagEntity(mediaItemID, tag))
    }

    suspend fun deleteByMediaItemID(mediaItemID: Long) = mediaItemTagDao.deleteByMediaItemID(mediaItemID)

    suspend fun deleteOrphanedTags() = tagDao.deleteOrphanedTags()

    suspend fun updateTagsForMediaItem(mediaItem: MediaItem) {
        deleteByMediaItemID(mediaItem.id)
        mediaItem.tags.forEach { tag ->
            insert(tag, mediaItem.id)
        }
        deleteOrphanedTags()
    }
}