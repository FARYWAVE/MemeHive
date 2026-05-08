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
import com.farywave.memehive.R
import com.farywave.memehive.ui.components.ImagePicker
import com.farywave.memehive.ui.simple_components.ActionType
import com.farywave.memehive.ui.simple_components.SimpleDialog
import com.farywave.memehive.ui.simple_components.SimpleDialogAction
import com.farywave.memehive.ui.simple_components.SimpleTextField
import com.farywave.memehive.ui.theme.LocalAppColors


@Composable
fun NewCollection(onDismissRequest: () -> Unit, onConfirm: (name: String?, coverSrc: Uri?) -> Unit) {
    val name = remember { mutableStateOf<String?>(null) }
    val coverSrc = remember { mutableStateOf<Uri?>(null) }

    SimpleDialog(
        title = stringResource(R.string.action_create_collection),
        actions = enumValues<NewCollectionAction>(),
        onDismissRequest = onDismissRequest,
        onAction = { action ->
            when (action) {
                NewCollectionAction.CREATE -> onConfirm(name.value, coverSrc.value)
                NewCollectionAction.CANCEL -> onDismissRequest()
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


enum class NewCollectionAction(@StringRes override val text: Int, override val type: ActionType) :
    SimpleDialogAction {
    CREATE(R.string.button_create, ActionType.NORMAL),
    CANCEL(R.string.button_cancel, ActionType.DISMISS)
}