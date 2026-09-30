package com.farywave.memehive.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_item_trigrams")
class MediaItemTrigramEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "trigram")
    val trigram: String,

    @ColumnInfo(name = "media_item_id")
    val mediaItemID: Long,
)