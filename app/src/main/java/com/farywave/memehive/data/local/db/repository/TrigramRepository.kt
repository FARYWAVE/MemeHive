package com.farywave.memehive.data.local.db.repository

import com.farywave.memehive.data.local.db.dao.MediaItemTrigramDao
import com.farywave.memehive.data.local.db.entity.MediaItemTrigramEntity

class TrigramRepository(
    private val trigramDao: MediaItemTrigramDao
) {
    suspend fun insertAll(trigrams: Set<String>, mediaItemId: Long): Unit {
        trigramDao.insertAll(
            trigrams.map { MediaItemTrigramEntity(trigram = it, mediaItemId = mediaItemId) }
        )
    }

    suspend fun updateTrigramsForMediaItem(mediaItemId: Long, trigrams: Set<String>) {
        deleteByMediaItemId(mediaItemId)
        insertAll(trigrams, mediaItemId)
    }

    suspend fun deleteByMediaItemId(mediaItemId: Long): Unit {
        trigramDao.deleteByMediaItemId(mediaItemId)
    }
}
