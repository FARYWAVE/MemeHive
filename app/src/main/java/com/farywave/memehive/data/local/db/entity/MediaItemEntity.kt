package com.farywave.memehive.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farywave.memehive.ui.model.MediaItem
import java.io.File

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val src: String?,
    val caption: String,
    val description: String,
) {
    fun toMediaItem() = MediaItem(id, src?.let { File(src) }, caption, description, mutableListOf())
}