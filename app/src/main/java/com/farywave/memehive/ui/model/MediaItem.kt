package com.farywave.memehive.ui.model

import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import com.farywave.memehive.data.local.db.entity.TagEntity

data class MediaItem(
    var id: Long,
    var url: String?,
    var name: String?,
    var description: String?,
    val tags: List<Tag>,
    var isSelected: Boolean = false,
) {

    fun toMediaItemEntity() = MediaItemEntity(id, url, name, description)
}