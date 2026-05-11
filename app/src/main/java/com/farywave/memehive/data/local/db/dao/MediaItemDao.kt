package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.relation.MediaWithTags
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
    SELECT * FROM media_items
    WHERE (:collectionId IS NULL OR id IN (
        SELECT mediaItemId FROM collection_entries WHERE collectionId = :collectionId
    ))
    """)
    suspend fun getByCollection(collectionId: Long?): List<MediaItemEntity>

    @Query("""
    SELECT mediaItemId FROM media_item_tags
    WHERE tagId IN (:tagIds)
    GROUP BY mediaItemId
    HAVING COUNT(DISTINCT tagId) = :tagCount
    """)
    suspend fun getMediaIdsByTags(tagIds: List<Long>, tagCount: Int): List<Long>

    @Transaction
    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaWithTags(): List<MediaWithTags>

    @Transaction
    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getMediaWithTagsByIds(ids: List<Long>): List<MediaWithTags>

    @Transaction
    @Query("""
    SELECT * FROM media_items
    WHERE (:collectionId IS NULL OR id IN (
        SELECT mediaItemId 
        FROM collection_entries 
        WHERE collectionId = :collectionId
    ))
""")
    suspend fun getMediaWithTagsByCollection(
        collectionId: Long?
    ): List<MediaWithTags>

    @Transaction
    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaWithTagsById(id: Long): MediaWithTags?

    @Query("SELECT id FROM media_items")
    suspend fun getAllIds(): List<Long>
}