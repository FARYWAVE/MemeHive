package com.farywave.memehive.ui.simple_components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.LocalAppColors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleBottomSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(show) {
        if (show) sheetState.show()
        else sheetState.hide()
    }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }
            .collect { value ->
                if (value == SheetValue.Hidden) {
                    onDismiss()
                }
            }
    }

    if (show) {
        ModalBottomSheet(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            containerColor = LocalAppColors.current.backgroundPrimary,
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            dragHandle = { DragHandle() }
        ) {
            content()
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(LocalAppColors.current.backgroundPrimary)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(50.dp)
                .height(7.dp)
                .background(
                    LocalAppColors.current.contentSecondary,
                    MaterialTheme.shapes.large
                )
        )
    }
}