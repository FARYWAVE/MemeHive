package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMediaItem(mediaItem: MediaItemEntity): Long

    @Update
    suspend fun updateMediaItem(mediaItem: MediaItemEntity)

    @Delete
    suspend fun deleteMediaItem(mediaItem: MediaItemEntity)

    @Query("SELECT * FROM media_items")
    fun getAllMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaItem(id: Long): MediaItemEntity?

    @Query("""
        SELECT m.*
        FROM media_items m

        LEFT JOIN collection_entries ce 
            ON ce.mediaItemId = m.id

        LEFT JOIN media_item_tags mt 
            ON mt.mediaItemId = m.id

        LEFT JOIN media_item_trigrams trig 
            ON trig.mediaItemId = m.id

        WHERE
            (:collectionId IS NULL OR ce.collectionId = :collectionId)
            AND (:tagIds IS NULL OR mt.tagId IN (:tagIds))
            AND (:trigrams IS NULL OR trig.trigram IN (:trigrams))

        GROUP BY m.id

        HAVING
            (:tagCount IS NULL OR COUNT(DISTINCT mt.tagId) = :tagCount)

        ORDER BY COUNT(DISTINCT trig.trigram) DESC
    """)
    suspend fun searchMedia(
        collectionId: Long?,
        trigrams: Set<String>?,
        tagIds: Set<Long>?,
        tagCount: Int? = tagIds?.size
    ): List<MediaItemEntity>
}