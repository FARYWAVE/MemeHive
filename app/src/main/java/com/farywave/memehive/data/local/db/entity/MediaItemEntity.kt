package com.farywave.memehive.data.local.db.entity

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farywave.memehive.ui.model.MediaItem
import androidx.core.net.toUri
import java.io.File

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: Long,
    val src: String?,
    val name: String?,
    val description: String?,
) {
    fun toMediaItem() = MediaItem(id, src?.let { File(src) }, name, description, mutableListOf())
}