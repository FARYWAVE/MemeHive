package com.farywave.memehive.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.farywave.memehive.ui.model.Collection
import java.io.File

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val name: String,
    val coverSrc: String?,
) {
    fun toCollection() = Collection(id, name, coverSrc?.let { File(coverSrc) }, false, 0)
}