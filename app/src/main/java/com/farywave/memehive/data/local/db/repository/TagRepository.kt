package com.farywave.memehive.data.local.db.repository

import com.farywave.memehive.data.local.db.dao.MediaItemTagDao
import com.farywave.memehive.data.local.db.dao.TagDao
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import com.farywave.memehive.data.local.db.entity.TagEntity
import com.farywave.memehive.domain.model.MediaItem

class TagRepository(
    private val tagDao: TagDao,
    private val mediaItemTagDao: MediaItemTagDao
) {
    suspend fun getAll() = tagDao.getAll()

    suspend fun getIdsByNames(names: List<String>) = tagDao.getIdsByNames(names);

    suspend fun getMediaItemIdsByTagIds(tagIds: List<Long>) =
        mediaItemTagDao.getMediaItemIdsByTags(tagIds)

    suspend fun getByName(name: String) = tagDao.getByName(name)

    suspend fun insert(name: String, mediaItemId: Long? = null) {
        val tag = getByName(name)?.id ?: tagDao.insert(TagEntity(name = name))
        if (mediaItemId != null) mediaItemTagDao.insert(MediaItemTagEntity(mediaItemId, tag))
    }

    suspend fun deleteByMediaItemId(mediaItemId: Long) = mediaItemTagDao.deleteByMediaItemId(mediaItemId)

    suspend fun deleteOrphanedTags() = tagDao.deleteOrphanedTags()

    suspend fun updateTagsForMediaItem(mediaItem: MediaItem) {
        deleteByMediaItemId(mediaItem.id)
        mediaItem.tags.forEach { tag ->
            insert(tag, mediaItem.id)
        }
        deleteOrphanedTags()
    }
}