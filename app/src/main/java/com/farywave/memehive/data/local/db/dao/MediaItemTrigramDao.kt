package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farywave.memehive.data.local.db.entity.MediaItemTrigramEntity

@Dao
interface MediaItemTrigramDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaItemTrigram(
        mediaItemTrigramEntity: MediaItemTrigramEntity
    ): Long

    @Query("SELECT * FROM media_item_trigrams")
    suspend fun getAllMediaItemTrigram(): List<MediaItemTrigramEntity>

    @Query("""
        SELECT mediaItemId 
        FROM media_item_trigrams
        WHERE trigram IN (:trigrams)
        GROUP BY mediaItemId
        ORDER BY COUNT(*) DESC
    """)
    suspend fun searchByTrigrams(trigrams: List<String>): List<Long>


    @Query("""
        DELETE FROM media_item_trigrams
        WHERE mediaItemId = :mediaItemId
    """)
    suspend fun deleteTrigramsForMedia(mediaItemId: Long)
}