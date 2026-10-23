package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farywave.memehive.data.local.db.entity.MediaItemTrigramEntity

@Dao
interface MediaItemTrigramDao {
    @Query("SELECT * FROM media_item_trigrams")
    suspend fun getAll(): List<MediaItemTrigramEntity>

    @Query("""
        SELECT media_item_id
        FROM media_item_trigrams
        WHERE trigram IN (:trigrams)
        GROUP BY media_item_id
        ORDER BY COUNT(*) DESC
    """)
    suspend fun getMediaItemIdsByTrigrams(trigrams: Set<String>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(
        mediaItemTrigramEntity: MediaItemTrigramEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(mediaItemTrigramEntities: List<MediaItemTrigramEntity>)

    @Query("""
        DELETE FROM media_item_trigrams
        WHERE media_item_id = :mediaItemId
    """)
    suspend fun deleteByMediaItemId(mediaItemId: Long)
}