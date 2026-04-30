package com.farywave.memehive.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farywave.memehive.ui.model.Collection

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val coverId: Int?,
) {
    fun toCollection() = Collection(id, name, coverId)
}