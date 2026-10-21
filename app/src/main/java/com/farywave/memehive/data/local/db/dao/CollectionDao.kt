package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.farywave.memehive.data.local.db.entity.CollectionEntity
import com.farywave.memehive.data.local.db.relation.CollectionDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(collection: CollectionEntity): Long

    @Update
    suspend fun update(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun delete(collectionId: Long)

    @Transaction
    @Query(
        """
    SELECT
        c.id,
        c.name,
        c.cover_image_id,
        i.id AS cover_id,
        i.name AS cover_name,
        i.sha256 AS cover_sha256,
        COUNT(ce.media_item_id) AS media_item_count
    FROM collections AS c
    LEFT JOIN images AS i
        ON i.id = c.cover_image_id
    LEFT JOIN collection_entries AS ce
        ON ce.collection_id = c.id
    GROUP BY
        c.id,
        c.name,
        c.cover_image_id,
        i.id,
        i.name,
        i.sha256
    ORDER BY c.id
    """
    )
    fun getAll(): Flow<List<CollectionDetails>>

    @Query("UPDATE collections SET cover_image_id = :imageId WHERE id = :collectionId")
    suspend fun setCover(collectionId: Long, imageId: Long)

    @Query("SELECT cover_image_id FROM collections WHERE id = :collectionId")
    suspend fun getCoverId(collectionId: Long): Long?
}