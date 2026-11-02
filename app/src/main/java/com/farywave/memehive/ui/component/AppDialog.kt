package com.farywave.memehive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun <T> AppDialog(
    actions: Array<T>,
    onDismissRequest: () -> Unit,
    title: String,
    onAction: (T) -> Unit,
    actionsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) where T : Enum<T>, T : DialogAction {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                dismissOnBackPress = actionsEnabled,
                dismissOnClickOutside = actionsEnabled,
            ),
    ) {
        val focusManager = LocalFocusManager.current
        FeedbackHost(overlay = false) {
            Surface(
                modifier =
                    Modifier.pointerInput(focusManager) {
                        detectTapGestures { focusManager.clearFocus() }
                    },
                color = LocalAppColors.current.backgroundPrimary,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 10.dp,
            ) {
                Column(
                    modifier =
                        Modifier.wrapContentSize().padding(horizontal = 25.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalAppColors.current.contentPrimary,
                        textAlign = TextAlign.Center,
                    )

                    content()

                    if (actions.isNotEmpty())
                        Row(
                            modifier = Modifier.wrapContentSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            actions.forEach { item ->
                                DialogButton(
                                    stringResource(item.labelResId),
                                    when (item.type) {
                                        DialogActionType.NORMAL ->
                                            LocalAppColors.current.accentPrimary
                                        DialogActionType.WARNING -> LocalAppColors.current.warning
                                        DialogActionType.DISMISS ->
                                            LocalAppColors.current.transparent
                                    },
                                    enabled = actionsEnabled,
                                ) {
                                    focusManager.clearFocus()
                                    onAction(item)
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    backgroundColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) =
    Box(
        modifier =
            Modifier.wrapContentSize()
                .background(backgroundColor, MaterialTheme.shapes.large)
                .padding(10.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary,
        )
    }

interface DialogAction {
    val labelResId: Int
    val type: DialogActionType
}

enum class DialogActionType {
    NORMAL,
    WARNING,
    DISMISS,
}
