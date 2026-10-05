package com.farywave.memehive.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "image_id")
    val imageId: Long?,

    @ColumnInfo(name = "caption")
    val caption: String,

    @ColumnInfo(name = "description")
    val description: String,
)