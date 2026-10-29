package com.farywave.memehive.ui.collection

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.farywave.memehive.R
import com.farywave.memehive.ui.component.ImagePicker
import com.farywave.memehive.ui.simple_component.ActionType
import com.farywave.memehive.ui.simple_component.SimpleDialog
import com.farywave.memehive.ui.simple_component.SimpleDialogAction
import com.farywave.memehive.ui.simple_component.SimpleTextField
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun CollectionEditorDialog(
    state: CollectionEditorState?,
    onSubmit: (CollectionDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    if (state == null) return
    key(state.sessionId) {
        var name by rememberSaveable { mutableStateOf(state.initialDraft.name) }
        var coverUri by rememberSaveable { mutableStateOf(state.initialDraft.coverUri) }
        val isNew = state.collectionId == null
        SimpleDialog(
            title = stringResource(if (isNew) R.string.action_create_collection else R.string.action_edit),
            actions = arrayOf(if (isNew) CollectionAction.CREATE else CollectionAction.SAVE, CollectionAction.CANCEL),
            actionsEnabled = !state.isSaving,
            onDismissRequest = onDismiss,
            onAction = { action ->
                if (action == CollectionAction.CANCEL) onDismiss()
                else onSubmit(CollectionDraft(name, coverUri))
            },
        ) {
            ImagePicker(
                modifier = Modifier.fillMaxWidth(0.7f),
                shape = MaterialTheme.shapes.medium as RoundedCornerShape,
                initialSrc = coverUri?.toUri(),
                enabled = !state.isSaving,
                onImageSelected = { uri -> coverUri = uri?.toString() },
            )
            SimpleTextField(
                initialValue = name,
                onValueChange = { name = it },
                callbackDelay = 0L,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalAppColors.current.backgroundSecondary, MaterialTheme.shapes.large)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                hint = stringResource(R.string.new_collection_name_hint)
            )

            state.errorResId?.let { Text(stringResource(it), color = LocalAppColors.current.warning) }
        }
    }
}

private enum class CollectionAction(
    @param:StringRes override val text: Int,
    override val type: ActionType = ActionType.NORMAL,
) : SimpleDialogAction {
    CREATE(R.string.button_create),
    SAVE(R.string.action_edit),
    CANCEL(R.string.button_cancel, ActionType.DISMISS),
}
