package com.farywave.memehive.data.local.db.repository

import com.farywave.memehive.data.local.db.dao.MediaItemTrigramDao
import com.farywave.memehive.data.local.db.entity.MediaItemTrigramEntity

class TrigramRepository(
    val trigramDao: MediaItemTrigramDao
) {
    suspend fun getMediaItemIDsByTrigrams(trigrams: Set<String>) = trigramDao.getMediaItemIDsByTrigrams(trigrams)

    suspend fun insertAll(trigrams: Set<String>, mediaItemID: Long) = trigramDao.insertAll(
        trigrams.map { MediaItemTrigramEntity(trigram = it, mediaItemID = mediaItemID) }
    )

    suspend fun deleteByMediaItemID(mediaItemID: Long) = trigramDao.deleteByMediaItemID(mediaItemID)

    suspend fun updateTrigramsForMediaItem(mediaItemID: Long, trigrams: Set<String>) {
        deleteByMediaItemID(mediaItemID)
        insertAll(trigrams, mediaItemID)
    }
}