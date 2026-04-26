package com.farywave.memehive.data.local.db.entity

import com.farywave.memehive.ui.model.Collection

data class CollectionEntity(
    val id: Int,
    var name: String,
    var logoId: Int?,
    val content: MutableList<Int>
) {
    override fun equals(other: Any?): Boolean {
        return other is CollectionEntity && other.id == id
    }

    fun toCollection() = Collection(id, name, logoId, content)
}