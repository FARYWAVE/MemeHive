package com.farywave.memehive.ui.main.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.ui.components.ActionType
import com.farywave.memehive.ui.components.SimpleDialog
import com.farywave.memehive.ui.components.SimpleDialogAction
import com.farywave.memehive.ui.components.SimpleTextField
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme


@Composable
fun NewCollection(onDismissRequest: () -> Unit, onConfirm: (String?) -> Unit) {
    val name = remember { mutableStateOf<String?>(null) }

    SimpleDialog(
        title = stringResource(R.string.action_create_collection),
        actions = enumValues<NewCollectionAction>(),
        onDismissRequest = onDismissRequest,
        onAction = { action ->
            when (action) {
                NewCollectionAction.CREATE -> onConfirm(name.value)
                NewCollectionAction.CANCEL -> onDismissRequest()
            }
        },
    ) {
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