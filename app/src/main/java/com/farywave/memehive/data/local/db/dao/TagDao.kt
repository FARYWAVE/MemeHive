package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.farywave.memehive.data.local.db.entity.TagEntity

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tagEntity: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun delete(tagId: Long)

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT id FROM tags WHERE name in (:names)")
    suspend fun getIdsByNames(names: List<String>): List<Long>

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun getByName(name: String): TagEntity?

    @Query("DELETE FROM tags WHERE id NOT IN (SELECT DISTINCT tag_id FROM media_item_tags)")
    suspend fun deleteOrphanedTags()
}