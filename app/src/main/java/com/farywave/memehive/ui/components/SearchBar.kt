package com.farywave.memehive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme

@Composable
fun SearchBar(hint: String, onQueryChange: (query: String) -> Unit) {
    val query = rememberTextFieldState()
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        snapshotFlow { query.text }
            .collect { text ->
                onQueryChange(text.toString())
            }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                LocalAppColors.current.backgroundSecondary,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = LocalAppColors.current.contentSecondary
        )
        Box(Modifier
            .weight(1f)
            .wrapContentHeight()) {
            if (query.text.isEmpty() && !isFocused) Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.contentSecondary,
            )
            BasicTextField(
                state = query,
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
}