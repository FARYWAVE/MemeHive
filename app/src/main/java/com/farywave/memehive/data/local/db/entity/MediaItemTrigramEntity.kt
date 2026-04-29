package com.farywave.memehive.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_item_trigrams")
class MediaItemTrigramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trigram: String,
    val mediaItemId: Long,
)