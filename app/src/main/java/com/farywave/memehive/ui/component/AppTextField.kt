package com.farywave.memehive.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.LocalAppColors
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.debounce

@OptIn(kotlinx.coroutines.FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    modifier: Modifier = Modifier,
    hint: String,
    minLines: Int = 1,
    initialValue: String?,
    onValueChange: (String) -> Unit,
    debounceMillis: Long = 0L,
    enabled: Boolean = true,
) {
    val value = rememberTextFieldState(initialText = initialValue.orEmpty())
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    val textStyle = MaterialTheme.typography.bodyMedium
    val textMeasurer = rememberTextMeasurer()
    val singleLineHeight =
        with(LocalDensity.current) {
            textMeasurer.measure("H", style = textStyle).size.height.toDp()
        }

    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(initialValue) {
        if (!isFocused && value.text.toString() != initialValue.orEmpty()) {
            value.setTextAndPlaceCursorAtEnd(initialValue.orEmpty())
        }
    }

    LaunchedEffect(debounceMillis) {
        snapshotFlow { value.text.toString() }
            .debounce(debounceMillis.milliseconds)
            .collect { text ->
                latestOnValueChange(text)
            }
    }

    Box(modifier = modifier.wrapContentHeight(), contentAlignment = Alignment.TopStart) {
        if (value.text.isEmpty() && !isFocused)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.contentSecondary,
            )
        TextField(
            state = value,
            modifier =
                Modifier.fillMaxWidth()
                    .then(
                        if (minLines == 1) {
                            Modifier.height(singleLineHeight)
                        } else {
                            Modifier.heightIn(min = 1.dp)
                        }
                    )
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = LocalAppColors.current.contentPrimary
                ),
            lineLimits =
                if (minLines == 1) TextFieldLineLimits.SingleLine
                else TextFieldLineLimits.MultiLine(minHeightInLines = minLines),
            outputTransformation = {
                if (length > 0) {
                    addStyle(SpanStyle(textDecoration = TextDecoration.None), 0, length)
                }
            },
            contentPadding = PaddingValues(0.dp),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = LocalAppColors.current.accentPrimary,
                ),
            enabled = enabled,
        )
    }
}
