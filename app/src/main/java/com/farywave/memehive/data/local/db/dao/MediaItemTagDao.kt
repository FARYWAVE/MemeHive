package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaItemTag(mediaItemTagEntity: MediaItemTagEntity): Long

    @Delete
    suspend fun deleteMediaItemTag(mediaItemTagEntity: MediaItemTagEntity)

    @Query("""
        SELECT * FROM media_item_tags 
        WHERE tagId = :tagId
    """)
    fun getAllMediaItemTags(tagId: Long): Flow<List<MediaItemTagEntity>>

    @Query("""
        SELECT tagId FROM media_item_tags
        WHERE mediaItemId = :mediaItemId
    """)
    suspend fun getTagIdsForMedia(mediaItemId: Long): List<Long>

    @Query("""
        DELETE FROM media_item_tags
        WHERE mediaItemId = :mediaItemId
    """)
    suspend fun deleteTagsForMedia(mediaItemId: Long)
}