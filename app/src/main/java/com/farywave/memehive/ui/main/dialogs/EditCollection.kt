package com.farywave.memehive.ui.main.dialogs

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.farywave.memehive.R
import com.farywave.memehive.ui.components.ImagePicker
import com.farywave.memehive.ui.simple_components.ActionType
import com.farywave.memehive.ui.simple_components.SimpleDialog
import com.farywave.memehive.ui.simple_components.SimpleDialogAction
import com.farywave.memehive.ui.simple_components.SimpleTextField
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun EditCollection(
    collectionName: String,
    collectionCover: String?,
    onDismissRequest: () -> Unit,
    onConfirm: (name: String?, cover: Uri?) -> Unit
) {
    val name = remember { mutableStateOf<String?>(collectionName) }
    val coverSrc = remember { mutableStateOf(collectionCover?.toUri()) }

    SimpleDialog(
        title = stringResource(R.string.action_edit_collection),
        actions = enumValues<EditCollectionAction>(),
        onDismissRequest = onDismissRequest,
        onAction = { action ->
            when (action) {
                EditCollectionAction.RENAME -> onConfirm(name.value, coverSrc.value)
                EditCollectionAction.CANCEL -> onDismissRequest()
            }
        },
    ) {
        ImagePicker(
            modifier = Modifier.fillMaxWidth(0.7f),
            initialSrc = coverSrc.value,
            shape = MaterialTheme.shapes.medium as RoundedCornerShape,
        ) {
            coverSrc.value = it
        }

        SimpleTextField(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .background(
                    LocalAppColors.current.backgroundSecondary,
                    shape = MaterialTheme.shapes.large
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            hint = stringResource(R.string.new_collection_name_hint),
            initialValue = name.value,
            onValueChange = { name.value = it }
        )
    }
}


enum class EditCollectionAction(
    @StringRes override val text: Int,
    override val type: ActionType
) :
    SimpleDialogAction {
    RENAME(R.string.action_rename, ActionType.NORMAL),
    CANCEL(R.string.button_cancel, ActionType.DISMISS)
}