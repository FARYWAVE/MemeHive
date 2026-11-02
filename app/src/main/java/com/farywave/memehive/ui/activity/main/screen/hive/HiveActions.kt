package com.farywave.memehive.ui.activity.main.screen.hive

import androidx.annotation.StringRes
import com.farywave.memehive.R
import com.farywave.memehive.ui.component.ActionMenuOption

enum class HiveCreateAction(
    @param:StringRes override val labelResId: Int,
    override val isDestructive: Boolean = false,
) : ActionMenuOption {
    CREATE_COLLECTION(R.string.action_create_collection),
    CREATE_MEDIA_ITEM(R.string.action_create_media),
}

enum class HiveMenuAction(
    @param:StringRes override val labelResId: Int,
    override val isDestructive: Boolean = false,
) : ActionMenuOption {
    ENABLE_PICKER(R.string.action_enable_picker),
    IMPORT_COLLECTION(R.string.action_import_collection),
    MASS_IMPORT(R.string.action_import_images),
    VIEW_APP_INFO(R.string.action_about),
}

enum class MediaSelectionAction(
    @param:StringRes override val labelResId: Int,
    override val isDestructive: Boolean = false,
) : ActionMenuOption {
    DUPLICATE(R.string.action_duplicate),
    ADD_TO_COLLECTION(R.string.action_add_to_collection),
    DELETE(R.string.action_delete, true),
}

enum class CollectionAction(
    @param:StringRes override val labelResId: Int,
    override val isDestructive: Boolean = false,
) : ActionMenuOption {
    EXPORT(R.string.action_export),
    EDIT(R.string.action_edit),
    DELETE(R.string.action_delete, true),
}
