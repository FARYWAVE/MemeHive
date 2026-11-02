package com.farywave.memehive.ui.activity.main.dialog

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.farywave.memehive.R
import com.farywave.memehive.ui.component.AppDialog
import com.farywave.memehive.ui.component.DialogAction
import com.farywave.memehive.ui.component.DialogActionType
import kotlin.enums.enumEntries

@Composable
fun DeleteSelectedMediaItemsDialog(
    selectedCount: Int,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AppDialog(
        title =
            pluralStringResource(
                R.plurals.delete_items_confirmation,
                selectedCount,
                selectedCount,
            ),
        actions = enumEntries<DeleteSelectedMediaItemsAction>().toTypedArray(),
        onDismissRequest = onDismissRequest,
        onAction = { action ->
            when (action) {
                DeleteSelectedMediaItemsAction.DELETE -> onDelete()
                DeleteSelectedMediaItemsAction.CANCEL -> onDismissRequest()
            }
        },
    ) {}
}

enum class DeleteSelectedMediaItemsAction(
    @param:StringRes override val labelResId: Int,
    override val type: DialogActionType,
) : DialogAction {
    DELETE(R.string.action_delete, DialogActionType.WARNING),
    CANCEL(R.string.action_cancel, DialogActionType.DISMISS),
}
