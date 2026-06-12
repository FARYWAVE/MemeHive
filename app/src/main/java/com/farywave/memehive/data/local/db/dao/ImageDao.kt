package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.ABORT
import androidx.room.Query
import com.farywave.memehive.data.local.db.entity.ImageEntity


@Dao
interface ImageDao {
    @Insert(onConflict = ABORT)
    suspend fun insert(image: ImageEntity): Long

    @Query("DELETE FROM images WHERE id = :imageId")
    suspend fun delete(imageId: Long)

    @Query("SELECT * FROM images WHERE id = :imageId")
    suspend fun get(imageId: Long): ImageEntity?
}