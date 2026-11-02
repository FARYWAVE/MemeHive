package com.farywave.memehive.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (show) {
        ModalBottomSheet(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            containerColor = LocalAppColors.current.backgroundPrimary,
            onDismissRequest = { onDismissRequest() },
            sheetState = sheetState,
            shape =
                MaterialTheme.shapes.extraLarge.copy(
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp),
                ),
            dragHandle = { DragHandle() },
        ) {
            content()
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .wrapContentHeight()
                .background(LocalAppColors.current.backgroundPrimary)
                .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.width(50.dp)
                .height(5.dp)
                .background(
                    LocalAppColors.current.contentSecondary,
                    MaterialTheme.shapes.large,
                )
        )
    }
}
