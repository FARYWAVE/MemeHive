package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.farywave.memehive.data.local.db.entity.MediaItemTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(mediaItemTagEntity: MediaItemTagEntity): Long

    @Query("DELETE FROM media_item_tags WHERE tag_id = :tagId AND media_item_id = :mediaItemId")
    suspend fun delete(tagId: Long, mediaItemId: Long)

    @Query("""
        SELECT * FROM media_item_tags 
        WHERE tag_id = :tagId
    """)
    fun getByTag(tagId: Long): Flow<List<MediaItemTagEntity>>

    @Query("""
        DELETE FROM media_item_tags
        WHERE media_item_id = :mediaItemId
    """)
    suspend fun deleteByMediaItemID(mediaItemId: Long)

    @Query("""
        SELECT media_item_id FROM media_item_tags 
        WHERE tag_id in (:tagIDs)
    """)
    suspend fun getMediaItemIDsByTags(tagIDs: List<Long>): List<Long>
}