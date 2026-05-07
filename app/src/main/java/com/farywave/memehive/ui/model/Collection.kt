package com.farywave.memehive.ui.model

import com.farywave.memehive.data.local.db.entity.CollectionEntity
import java.io.File

data class Collection(
    val id: Long,
    var name: String,
    var cover: File? = null,
    val mediaItemCount: Int,
) {

    fun toCollectionEntity() = CollectionEntity(id, name, cover?.path)
}