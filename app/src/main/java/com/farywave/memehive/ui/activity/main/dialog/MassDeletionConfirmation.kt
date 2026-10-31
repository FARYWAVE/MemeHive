package com.farywave.memehive.ui.activity.main.dialog

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.farywave.memehive.R
import com.farywave.memehive.ui.simple_component.ActionType
import com.farywave.memehive.ui.simple_component.SimpleDialog
import com.farywave.memehive.ui.simple_component.SimpleDialogAction
import com.farywave.memehive.ui.theme.LocalAppColors
import kotlin.enums.enumEntries

@Composable
fun MassDeletionConfirmation(
    selectedCount: Int,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    SimpleDialog(
        title = pluralStringResource(
            R.plurals.delete_items_confirmation,
            selectedCount,
            selectedCount
        ),
        actions = enumEntries<MassDeletionConfirmationAction>().toTypedArray(),
        onDismissRequest = onDismissRequest,
        onAction = { action ->
            when (action) {
                MassDeletionConfirmationAction.DELETE -> onDelete()
                MassDeletionConfirmationAction.CANCEL -> onDismissRequest()
            }
        }
    ) {}
}

enum class MassDeletionConfirmationAction(
    @param:StringRes override val text: Int,
    override val type: ActionType
) :
    SimpleDialogAction {
    DELETE(R.string.action_delete, ActionType.WARNING),
    CANCEL(R.string.action_cancel, ActionType.DISMISS),
}