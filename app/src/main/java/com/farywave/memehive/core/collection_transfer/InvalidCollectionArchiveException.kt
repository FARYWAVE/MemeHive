package com.farywave.memehive.core.collection_transfer

/** The selected collection file is corrupt or does not contain a complete collection. */
class InvalidCollectionArchiveException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
