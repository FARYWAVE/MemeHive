package com.farywave.memehive.ui.activity.main.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.component.AppCheckbox
import com.farywave.memehive.ui.component.AppDialog
import com.farywave.memehive.ui.component.DialogAction
import com.farywave.memehive.ui.component.DialogActionType
import com.farywave.memehive.ui.component.ImageWithPlaceholder
import com.farywave.memehive.ui.theme.LocalAppColors
import kotlin.enums.enumEntries

@Composable
fun DeleteCollectionDialog(
    collection: Collection,
    onDelete: (deleteContents: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var deleteContents by rememberSaveable { mutableStateOf(false) }

    AppDialog(
        title = stringResource(R.string.message_delete_collection),
        actions = enumEntries<DeleteCollectionAction>().toTypedArray(),
        onDismissRequest = onDismissRequest,
        onAction = { action ->
            when (action) {
                DeleteCollectionAction.DELETE -> onDelete(deleteContents)
                DeleteCollectionAction.CANCEL -> onDismissRequest()
            }
        },
    ) {
        Column(
            modifier = Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ImageWithPlaceholder(
                modifier = Modifier.fillMaxWidth(0.7f),
                shape = MaterialTheme.shapes.medium,
                imageUri = collection.cover?.uri,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleMedium,
                color = LocalAppColors.current.accentPrimary,
            )

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppCheckbox(
                    checked = deleteContents,
                    onClick = { deleteContents = !deleteContents },
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text =
                        stringResource(R.string.label_delete_contents, collection.mediaItemCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalAppColors.current.contentPrimary,
                )
            }
        }
    }
}

enum class DeleteCollectionAction(
    @param:StringRes override val labelResId: Int,
    override val type: DialogActionType,
) : DialogAction {
    DELETE(R.string.action_delete, DialogActionType.WARNING),
    CANCEL(R.string.action_cancel, DialogActionType.DISMISS),
}
