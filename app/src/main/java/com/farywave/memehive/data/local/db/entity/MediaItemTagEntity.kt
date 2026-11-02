package com.farywave.memehive.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "media_item_tags",
    indices = [Index(value = ["tag_id"])],
    primaryKeys = ["media_item_id", "tag_id"],
    foreignKeys =
        [
            ForeignKey(
                entity = MediaItemEntity::class,
                parentColumns = ["id"],
                childColumns = ["media_item_id"],
                onDelete = ForeignKey.CASCADE,
            ),
            ForeignKey(
                entity = TagEntity::class,
                parentColumns = ["id"],
                childColumns = ["tag_id"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
)
data class MediaItemTagEntity(
    @ColumnInfo(name = "media_item_id") val mediaItemId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)
