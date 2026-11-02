package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farywave.memehive.data.local.db.entity.CollectionEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionEntryDao {
    @Query(
        """
        SELECT * FROM collection_entries
        WHERE collection_id = :collectionId
    """
    )
    fun observeByCollection(collectionId: Long): Flow<List<CollectionEntryEntity>>

    @Query(
        """
        SELECT media_item_id FROM collection_entries
        WHERE collection_id = :collectionId
    """
    )
    suspend fun getIdsByCollection(collectionId: Long): List<Long>

    @Query(
        """
        SELECT entry.media_item_id FROM collection_entries AS entry
        WHERE entry.collection_id = :collectionId
          AND NOT EXISTS (
              SELECT 1 FROM collection_entries AS other
              WHERE other.media_item_id = entry.media_item_id
                AND other.collection_id != :collectionId
          )
    """
    )
    suspend fun getExclusiveIdsByCollection(collectionId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM collection_entries WHERE collection_id = :collectionId")
    fun observeEntryCount(collectionId: Long): Flow<Int>

    @Query("SELECT * FROM collection_entries WHERE media_item_id = :mediaItemId")
    suspend fun getByMediaItem(mediaItemId: Long): List<CollectionEntryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(collectionEntryEntity: CollectionEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(collectionEntryEntities: List<CollectionEntryEntity>): List<Long>

    @Query(
        "DELETE FROM collection_entries WHERE collection_id = :collectionId AND media_item_id = :mediaItemId"
    )
    suspend fun delete(collectionId: Long, mediaItemId: Long)

    @Query(
        """
        DELETE FROM collection_entries
        WHERE collection_id = :collectionId
    """
    )
    suspend fun deleteByCollection(collectionId: Long)

    @Query("DELETE FROM collection_entries WHERE media_item_id = :mediaItemId")
    suspend fun deleteByMediaItem(mediaItemId: Long)
}
