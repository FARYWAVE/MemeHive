package com.farywave.memehive.ui.activity.main.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.farywave.memehive.R
import com.farywave.memehive.ui.simple_component.ActionType
import com.farywave.memehive.ui.simple_component.SimpleDialog
import com.farywave.memehive.ui.simple_component.SimpleDialogAction
import com.farywave.memehive.ui.theme.LocalAppColors
import kotlin.enums.enumEntries

@Composable
fun DiscardEditingConfirmation(onSave: () -> Unit, onDiscard: () -> Unit, onDismissRequest: () -> Unit) {
    SimpleDialog(
        title = stringResource(R.string.discard_changes_title),
        actions = enumEntries<DiscardEditingAction>().toTypedArray(),
        onDismissRequest = onDismissRequest,
        onAction = { action ->
            when (action) {
                DiscardEditingAction.SAVE -> onSave()
                DiscardEditingAction.DISCARD -> onDiscard()
            }
        }
    ) {
        Text(
            text = stringResource(R.string.discard_changes_message),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary
        )
    }
}

enum class DiscardEditingAction(@StringRes override val text: Int, override val type: ActionType) :
    SimpleDialogAction {
    SAVE(R.string.action_save, ActionType.NORMAL),
    DISCARD(R.string.discard_changes_confirm, ActionType.WARNING),
}
