package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM collections WHERE id = :collectionID")
    suspend fun delete(collectionID: Long)

    @Query("SELECT * FROM collections")
    fun getAll(): Flow<List<CollectionDetails>>

    @Query("UPDATE collections SET cover_image_id = :imageID WHERE id = :collectionID")
    suspend fun setCover(collectionID: Long, imageID: Long)
}