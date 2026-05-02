package com.farywave.memehive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun SimpleDialog(
    onDismissRequest: () -> Unit,
    title: String,
    actionButtonText: String,
    onAction: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(color = LocalAppColors.current.backgroundPrimary, shape = MaterialTheme.shapes.medium, tonalElevation = 10.dp) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 25.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalAppColors.current.contentPrimary
                )

                content()

                Row(
                    modifier = Modifier.wrapContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DialogButton(
                        actionButtonText,
                        LocalAppColors.current.accentPrimary
                    ) { onAction() }

                    DialogButton(
                        stringResource(R.string.button_cancel),
                        LocalAppColors.current.transparent
                    ) { onDismissRequest() }
                }
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, backgroundColor: Color, onClick: () -> Unit) =
    Box(
        modifier = Modifier
            .wrapContentSize()
            .background(backgroundColor, MaterialTheme.shapes.large)
            .padding(10.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary
        )
    }