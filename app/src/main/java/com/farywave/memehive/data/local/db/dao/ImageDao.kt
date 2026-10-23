package com.farywave.memehive.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.ABORT
import androidx.room.Query
import com.farywave.memehive.data.local.db.entity.ImageEntity


@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE id = :imageId")
    suspend fun getById(imageId: Long): ImageEntity?

    @Query("SELECT * FROM images WHERE sha256 = :sha256")
    suspend fun getBySHA256(sha256: String): ImageEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM images WHERE name = :name)")
    suspend fun existsByName(name: String): Boolean

    @Query("""
        SELECT (
            SELECT COUNT(*) FROM collections WHERE cover_image_id = :imageId
        ) + (
            SELECT COUNT(*) FROM media_items WHERE image_id = :imageId
        ) AS total_count
    """)
    suspend fun getUsageCount(imageId: Long): Int

    @Insert(onConflict = ABORT)
    suspend fun insert(image: ImageEntity): Long

    @Query("DELETE FROM images WHERE id = :imageId")
    suspend fun delete(imageId: Long)
}
