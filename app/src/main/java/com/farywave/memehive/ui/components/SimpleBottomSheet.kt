package com.farywave.memehive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleBottomSheet(show: Boolean, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(show) }

    LaunchedEffect(showSheet) {
        if (showSheet) {
            sheetState.partialExpand()
        }
    }

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            showSheet = false
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .shadow(9.dp),
            onDismissRequest = { showSheet = false },
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


@Preview
@Composable
private fun Preview() {
    MemeHiveTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            SimpleBottomSheet(true) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Blue),
                ) {
                    Text("Hello from sheet", color = Color.White)
                }
            }
        }
    }
}