package com.farywave.memehive.ui.main.screens.editing

import androidx.annotation.StringRes
import com.farywave.memehive.R
import com.farywave.memehive.ui.components.ActionMenuOptions

enum class MoreActions(
    @StringRes override val labelId: Int,
    override val highlighted: Boolean = false
) : ActionMenuOptions {
    DUPLICATE(R.string.action_duplicate),
    ADD_TO_COLLECTION(R.string.action_add_to_collection),
    SET_AS_COLLECTION_COVER(R.string.action_set_as_cover),
    DELETE(R.string.action_delete, true),
}