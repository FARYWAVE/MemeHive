package com.farywave.memehive.data.local.db.mapper

import android.net.Uri
import com.farywave.memehive.data.local.db.entity.ImageEntity
import com.farywave.memehive.domain.model.Image

fun ImageEntity.toModel(uri: Uri) = Image(id = id, uri = uri)
