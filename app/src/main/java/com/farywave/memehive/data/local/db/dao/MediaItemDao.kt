package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
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
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(mediaItem: MediaItemEntity): Long

    @Update
    suspend fun update(mediaItem: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :mediaItemId")
    suspend fun delete(mediaItemId: Long)

    @Transaction
    @Query("SELECT * FROM media_items")
    fun getAll(): Flow<List<MediaItemDetails>>

    @Transaction
    @Query("SELECT * FROM media_items WHERE id = :mediaItemId")
    suspend fun get(mediaItemId: Long): MediaItemDetails?

    @Query("""
    SELECT media_item_id FROM media_item_tags
    WHERE tag_id IN (:tagIds)
    GROUP BY media_item_id
    HAVING COUNT(DISTINCT tag_id) = :tagCount
    """)
    suspend fun getIDsByTags(tagIds: List<Long>, tagCount: Int): List<Long>

    @Transaction
    @Query("SELECT * FROM media_items WHERE id IN (:mediaItemIds)")
    suspend fun getByIds(mediaItemIds: List<Long>): List<MediaItemDetails>

    @Transaction
    @Query("""
    SELECT * FROM media_items
    WHERE (:collectionId IS NULL OR id IN (
        SELECT media_item_id 
        FROM collection_entries 
        WHERE collection_id = :collectionId
    ))
""")
    suspend fun getByCollection(
        collectionId: Long?
    ): List<MediaItemDetails>

    @Query("SELECT id FROM media_items")
    suspend fun getAllIDs(): List<Long>
}