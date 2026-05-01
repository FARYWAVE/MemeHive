package com.farywave.memehive.ui.model

import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import java.io.File

data class MediaItem(
    var id: Long,
    var src: File?,
    var caption: String?,
    var description: String?,
    val tags: List<String>,
    var isSelected: Boolean = false,
) {

    fun toMediaItemEntity() = MediaItemEntity(id, src?.toString(), caption, description)
}