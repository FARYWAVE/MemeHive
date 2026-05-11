package com.farywave.memehive.core.collection_transfer

import kotlinx.serialization.Serializable


@Serializable
data class TransferManifest(
    val appVersion: Int,
    val exportedAt: Long,
    val collection: TransferCollection,
    val mediaItems: List<TransferMediaItem>
)