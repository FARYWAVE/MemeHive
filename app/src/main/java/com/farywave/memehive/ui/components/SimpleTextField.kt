package com.farywave.memehive.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun SimpleTextField(
    modifier: Modifier = Modifier,
    hint: String,
    initialValue: String?,
    onValueChange: (String) -> Unit,
    callbackDelay: Long = 300L,
) {
    val value = rememberTextFieldState(initialValue ?: "")
    var isFocused by remember { mutableStateOf(false) }
    val lastCallback = remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        if (lastCallback.longValue + callbackDelay < System.currentTimeMillis()) {
            snapshotFlow { value.text }
                .collect { text ->
                    onValueChange(text.toString())
                }
        }
        lastCallback.longValue = System.currentTimeMillis()
    }

    Box(modifier = modifier.wrapContentHeight()) {
        if (value.text.isEmpty() && !isFocused) Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentSecondary,
        )
        BasicTextField(
            state = value,
            modifier = Modifier
                .padding(0.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = LocalAppColors.current.contentPrimary),
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorBrush = SolidColor(LocalAppColors.current.accentPrimary)
        )
    }
}