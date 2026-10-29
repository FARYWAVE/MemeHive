package com.farywave.memehive.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun EmptyHiveMessage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.media_library_empty_title),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary,
        )
        Text(
            text = stringResource(R.string.media_library_empty_message),
            style = MaterialTheme.typography.labelMedium,
            color = LocalAppColors.current.contentSecondary,
        )
    }
}