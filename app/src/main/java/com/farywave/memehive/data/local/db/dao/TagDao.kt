package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.farywave.memehive.data.local.db.entity.TagEntity

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tagEntity: TagEntity): Long

    @Delete
    suspend fun deleteTag(tagEntity: TagEntity)

    @Query("SELECT * FROM TagEntity")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM TagEntity WHERE name = :name")
    suspend fun getTagByName(name: String): TagEntity?
}