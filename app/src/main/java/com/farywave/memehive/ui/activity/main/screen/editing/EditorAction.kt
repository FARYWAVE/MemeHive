package com.farywave.memehive.ui.activity.main.screen.editing

import androidx.annotation.StringRes
import com.farywave.memehive.R
import com.farywave.memehive.ui.component.ActionMenuOption

enum class EditorAction(
    @param:StringRes override val labelResId: Int,
    override val isDestructive: Boolean = false,
) : ActionMenuOption {
    COPY_TO_CLIPBOARD(R.string.action_copy_image),
    DUPLICATE(R.string.action_duplicate),
    ADD_TO_COLLECTION(R.string.action_add_to_collection),
    SET_AS_COVER(R.string.action_set_as_cover),
    DELETE(R.string.action_delete, true),
}
