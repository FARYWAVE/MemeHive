package com.farywave.memehive.core.collection_transfer

import kotlinx.serialization.Serializable


@Serializable
data class TransferMediaItem (
    val src: String?,
    val caption: String,
    val description: String,
    val tags: List<String>
)