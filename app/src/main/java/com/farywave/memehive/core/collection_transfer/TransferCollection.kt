package com.farywave.memehive.core.collection_transfer

import kotlinx.serialization.Serializable


@Serializable
data class TransferCollection(
    val name: String,
    val coverSrc: String?
)