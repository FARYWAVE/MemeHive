package com.farywave.memehive.ui.main.screens.editing

import androidx.annotation.StringRes
import com.farywave.memehive.R
import com.farywave.memehive.ui.simple_components.ActionMenuOptions

enum class MoreActions(
    @StringRes override val labelId: Int,
    override val highlighted: Boolean = false
) : ActionMenuOptions {
    DUPLICATE(R.string.action_duplicate),
    ADD_TO_COLLECTION(R.string.action_add_to_collection),
    DELETE(R.string.action_delete, true),
}