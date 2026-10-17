package com.farywave.memehive.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_item_trigrams",
    indices = [
        Index(
            value = ["media_item_id", "trigram"],
            unique = true
        ),
        Index(value = ["trigram", "media_item_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class MediaItemTrigramEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "trigram")
    val trigram: String,

    @ColumnInfo(name = "media_item_id")
    val mediaItemId: Long,
)