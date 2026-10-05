package com.farywave.memehive.data.local.db.mapper

import androidx.core.net.toUri
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.ui.model.Image
import java.io.File

fun Image.toEntity(sha256: String) = ImageEntity(
    id = id,
    path = uri.toString(),
    sha256 = sha256
)

fun ImageEntity.toModel() = Image(
    id = id,
    uri = File(path).toUri()
)
