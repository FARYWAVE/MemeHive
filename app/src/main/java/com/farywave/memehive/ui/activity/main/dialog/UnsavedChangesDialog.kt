package com.farywave.memehive.ui.activity.main.dialog

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.farywave.memehive.R
import com.farywave.memehive.ui.component.AppDialog
import com.farywave.memehive.ui.component.DialogAction
import com.farywave.memehive.ui.component.DialogActionType
import com.farywave.memehive.ui.theme.LocalAppColors
import kotlin.enums.enumEntries

@Composable
fun UnsavedChangesDialog(onSave: () -> Unit, onDiscard: () -> Unit, onDismissRequest: () -> Unit) {
    AppDialog(
        title = stringResource(R.string.title_unsaved_changes),
        actions = enumEntries<UnsavedChangesAction>().toTypedArray(),
        onDismissRequest = onDismissRequest,
        onAction = { action ->
            when (action) {
                UnsavedChangesAction.SAVE -> onSave()
                UnsavedChangesAction.DISCARD -> onDiscard()
            }
        },
    ) {
        Text(
            text = stringResource(R.string.message_unsaved_changes),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary,
        )
    }
}

enum class UnsavedChangesAction(
    @param:StringRes override val labelResId: Int,
    override val type: DialogActionType,
) : DialogAction {
    SAVE(R.string.action_save, DialogActionType.NORMAL),
    DISCARD(R.string.action_discard, DialogActionType.WARNING),
}
