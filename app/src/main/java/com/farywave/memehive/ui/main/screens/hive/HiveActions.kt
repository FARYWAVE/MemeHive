package com.farywave.memehive.ui.main.screens.hive

import androidx.annotation.StringRes
import com.farywave.memehive.R
import com.farywave.memehive.ui.simple_components.ActionMenuOptions

enum class CreateActions(
    @StringRes override val labelId: Int,
    override val highlighted: Boolean = false
) : ActionMenuOptions {
    CREATE_COLLECTION(R.string.action_create_collection),
    CREATE_MEDIA_ITEM(R.string.action_create_media_item)
}

enum class MoreActions(
    @StringRes override val labelId: Int,
    override val highlighted: Boolean = false
) : ActionMenuOptions {
    ACTIVATE_SUBSCRIPTION(R.string.action_activate_subscription),
    ENABLE_PICKER(R.string.action_enable_picker),
    IMPORT_COLLECTION(R.string.action_import_collection),
    MASS_IMPORT(R.string.action_mass_import),
    VIEW_APP_INFO(R.string.action_app_info),
}

enum class MassEditActions(
    @StringRes override val labelId: Int,
    override val highlighted: Boolean = false
) : ActionMenuOptions {
    DUPLICATE(R.string.action_duplicate),
    ADD_TO_COLLECTION(R.string.action_add_to_collection),
    DELETE(R.string.action_delete, true)
}

enum class CollectionActions(
    @StringRes override val labelId: Int,
    override val highlighted: Boolean = false
) : ActionMenuOptions {
    EXPORT(R.string.action_export),
    EDIT(R.string.action_edit),
    DELETE(R.string.action_delete, true)
}