package com.farywave.memehive.ui.activity.main.dialog.collection_editor

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.farywave.memehive.ui.component.AppDialog
import com.farywave.memehive.ui.component.AppTextField
import com.farywave.memehive.ui.component.DialogAction
import com.farywave.memehive.ui.component.DialogActionType
import com.farywave.memehive.ui.component.ErrorNotice
import com.farywave.memehive.ui.component.ImagePicker
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun CollectionEditorDialog(
    state: CollectionEditorState?,
    onSubmit: (CollectionDraft) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (state == null) return
    key(state.sessionId) {
        var name by rememberSaveable { mutableStateOf(state.initialDraft.name) }
        var coverUri by rememberSaveable { mutableStateOf(state.initialDraft.coverUri) }
        val isNew = state.collectionId == null
        AppDialog(
            title =
                stringResource(
                    if (isNew) R.string.action_create_collection
                    else R.string.action_edit_collection
                ),
            actions =
                arrayOf(
                    if (isNew) CollectionEditorAction.CREATE else CollectionEditorAction.SAVE,
                    CollectionEditorAction.CANCEL,
                ),
            actionsEnabled = !state.isSaving,
            onDismissRequest = onDismissRequest,
            onAction = { action ->
                if (action == CollectionEditorAction.CANCEL) onDismissRequest()
                else onSubmit(CollectionDraft(name, coverUri))
            },
        ) {
            ImagePicker(
                modifier = Modifier.fillMaxWidth(0.7f),
                shape = MaterialTheme.shapes.medium,
                imageUri = coverUri?.toUri(),
                enabled = !state.isSaving,
                onImageSelected = { uri -> coverUri = uri?.toString() },
            )
            AppTextField(
                initialValue = name,
                onValueChange = { name = it },
                debounceMillis = 0L,
                enabled = !state.isSaving,
                modifier =
                    Modifier.fillMaxWidth()
                        .background(
                            LocalAppColors.current.backgroundSecondary,
                            MaterialTheme.shapes.large,
                        )
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                hint = stringResource(R.string.hint_collection_name),
            )

            state.errorResId?.let { ErrorNotice(it) }
        }
    }
}

private enum class CollectionEditorAction(
    @param:StringRes override val labelResId: Int,
    override val type: DialogActionType = DialogActionType.NORMAL,
) : DialogAction {
    CREATE(R.string.action_create),
    SAVE(R.string.action_save),
    CANCEL(R.string.action_cancel, DialogActionType.DISMISS),
}
