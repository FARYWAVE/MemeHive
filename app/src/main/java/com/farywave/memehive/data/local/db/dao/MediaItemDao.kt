package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.relation.MediaItemDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items ORDER BY id") @Transaction fun observeAll(): Flow<List<MediaItemDetails>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE (:collectionId IS NULL OR id IN (
            SELECT media_item_id FROM collection_entries
            WHERE collection_id = :collectionId
        ))
        ORDER BY id
    """
    )
    @Transaction
    fun observeByCollection(collectionId: Long?): Flow<List<MediaItemDetails>>

    @Query("SELECT * FROM media_items WHERE id = :mediaItemId")
    @Transaction
    suspend fun get(mediaItemId: Long): MediaItemDetails?

    @Query("SELECT * FROM media_items WHERE id IN (:mediaItemIds)")
    @Transaction
    suspend fun getByIds(mediaItemIds: List<Long>): List<MediaItemDetails>

    @Query(
        """
    SELECT * FROM media_items
    WHERE (:collectionId IS NULL OR id IN (
        SELECT media_item_id
        FROM collection_entries
        WHERE collection_id = :collectionId
    ))
"""
    )
    @Transaction
    suspend fun getByCollection(collectionId: Long): List<MediaItemDetails>

    @Query("SELECT id FROM media_items") suspend fun getAllIds(): List<Long>

    @Query(
        """
    SELECT media_item_id FROM media_item_tags
    WHERE tag_id IN (:tagIds)
    GROUP BY media_item_id
    HAVING COUNT(DISTINCT tag_id) = :tagCount
    """
    )
    suspend fun getIdsByTags(tagIds: List<Long>, tagCount: Int): List<Long>

    @Query("SELECT image_id FROM media_items WHERE id = :mediaItemId")
    suspend fun getImageId(mediaItemId: Long): Long?

    @Query("SELECT image_id FROM media_items WHERE id IN (:mediaItemIds) AND image_id IS NOT NULL")
    suspend fun getImageIds(mediaItemIds: Set<Long>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(mediaItem: MediaItemEntity): Long

    @Update suspend fun update(mediaItem: MediaItemEntity): Int

    @Query("DELETE FROM media_items WHERE id = :mediaItemId") suspend fun delete(mediaItemId: Long)

    @Query("DELETE FROM media_items WHERE id IN (:mediaItemIds)")
    suspend fun deleteByIds(mediaItemIds: Set<Long>)
}
