package com.farywave.memehive.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "media_item_tags",
    primaryKeys = ["mediaItemId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MediaItemTagEntity(
    val mediaItemId: Long,
    val tagId: Long
)