package com.farywave.memehive.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [Index(value = ["image_id"])],
    foreignKeys =
        [
            ForeignKey(
                entity = ImageEntity::class,
                parentColumns = ["id"],
                childColumns = ["image_id"],
                onDelete = ForeignKey.SET_NULL,
            )
        ],
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "image_id") val imageId: Long?,
    @ColumnInfo(name = "caption") val caption: String,
    @ColumnInfo(name = "description") val description: String,
)
