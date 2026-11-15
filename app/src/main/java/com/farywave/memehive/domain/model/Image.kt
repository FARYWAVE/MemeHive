package com.farywave.memehive.domain.model

import android.net.Uri

data class Image(
    val id: Long = 0,
    val uri: Uri,
    val width: Int? = null,
    val height: Int? = null,
)

val Image?.aspectRatio: Float
    get() {
        val width = this?.width ?: return 1.5f
        val height = this.height ?: return 1.5f
        return if (width > 0 && height > 0) width.toFloat() / height else 1.5f
    }
