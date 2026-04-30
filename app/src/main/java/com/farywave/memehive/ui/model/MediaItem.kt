package com.farywave.memehive.ui.model

import android.net.Uri
import com.farywave.memehive.data.local.db.entity.MediaItemEntity
import java.io.File

data class MediaItem(
    var id: Long,
    var src: File?,
    var name: String?,
    var description: String?,
    val tags: List<Tag>,
    var isSelected: Boolean = false,
) {

    fun toMediaItemEntity() = MediaItemEntity(id, src?.toString(), name, description)
}