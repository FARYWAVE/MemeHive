package com.farywave.memehive.data.local.db.repository

import com.farywave.memehive.data.local.db.dao.MediaItemTrigramDao
import com.farywave.memehive.data.local.db.entity.MediaItemTrigramEntity

class TrigramRepository(
    val trigramDao: MediaItemTrigramDao
) {
    suspend fun getMediaItemIdsByTrigrams(trigrams: Set<String>) = trigramDao.getMediaItemIdsByTrigrams(trigrams)

    suspend fun insertAll(trigrams: Set<String>, mediaItemId: Long) = trigramDao.insertAll(
        trigrams.map { MediaItemTrigramEntity(trigram = it, mediaItemId = mediaItemId) }
    )

    suspend fun deleteByMediaItemId(mediaItemId: Long) = trigramDao.deleteByMediaItemId(mediaItemId)

    suspend fun updateTrigramsForMediaItem(mediaItemId: Long, trigrams: Set<String>) {
        deleteByMediaItemId(mediaItemId)
        insertAll(trigrams, mediaItemId)
    }
}