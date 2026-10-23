package com.farywave.memehive.ui.model

import androidx.annotation.StringRes
import java.util.UUID

/** A distinct error occurrence, even when its message matches a previous failure. */
data class UiError(
    @StringRes val messageResId: Int,
    val id: String = UUID.randomUUID().toString(),
)
