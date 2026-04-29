package com.farywave.memehive.ui.model

import com.farywave.memehive.data.local.db.entity.TagEntity


data class Tag(val id: Long, val name: String) {
    fun toTagEntity() = TagEntity(id, name)
}