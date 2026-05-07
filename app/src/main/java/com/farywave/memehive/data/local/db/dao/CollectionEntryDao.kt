package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farywave.memehive.data.local.db.entity.CollectionEntryEntity
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionEntryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionEntry(collectionEntryEntity: CollectionEntryEntity): Long

    @Delete
    suspend fun deleteCollectionEntry(collectionEntryEntity: CollectionEntryEntity)

    @Query("""
        SELECT * FROM collection_entries
        WHERE collectionId = :collectionId
    """)
    fun getAllCollectionEntries(collectionId: Long): Flow<List<CollectionEntryEntity>>

    @Query("""
        SELECT mediaItemId FROM collection_entries
        WHERE collectionId = :collectionId
    """)
    suspend fun getMediaIds(collectionId: Long): List<Long>

    @Query("""
        DELETE FROM collection_entries
        WHERE collectionId = :collectionId
    """)
    suspend fun deleteByCollection(collectionId: Long)

    @Query("SELECT COUNT(*) FROM collection_entries WHERE collectionId = :collectionId")
    fun getEntryCount(collectionId: Long): Flow<Int>

    @Query("SELECT * FROM collection_entries WHERE mediaItemId = :id")
    fun getByMediaItemId(id: Long): Flow<List<CollectionEntryEntity>>

    @Query("DELETE FROM collection_entries WHERE mediaItemId = :id")
    fun deleteByMediaItemId(id: Long)
}