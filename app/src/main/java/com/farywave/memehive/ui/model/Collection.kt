package com.farywave.memehive.ui.model

import android.net.Uri
import com.farywave.memehive.data.local.db.entity.CollectionEntity
import java.io.File

data class Collection(
    val id: Long = 0,
    var name: String,
    var cover: Image? = null,
    val mediaItemCount: Int = 0,
) {
    fun toEntity() = CollectionEntity(
        name = name,
        coverImageId = cover?.id
    )
}