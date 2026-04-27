package com.farywave.memehive.ui.model

import com.farywave.memehive.data.local.db.entity.CollectionEntity

data class Collection(
    val id: Int,
    var name: String,
    var logoId: Int?,
    val content: MutableList<Int>,
    var isSelected: Boolean = false
) {

    fun toCollectionEntity() = CollectionEntity(id, name, logoId, content)
}