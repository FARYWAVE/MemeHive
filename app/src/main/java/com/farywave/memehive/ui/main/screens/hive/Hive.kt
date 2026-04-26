package com.farywave.memehive.ui.main.screens.hive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.Typography

@Composable
fun Hive() {
    Scaffold(modifier = Modifier
        .fillMaxSize()
        .background(LocalAppColors.current.backgroundPrimary),
        topBar = { Toolbar() }
    ) { paddingValues ->

    }
}

@Composable
private fun Toolbar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(LocalAppColors.current.backgroundPrimary)
            .padding(horizontal = 25.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.app_name_p1),
            color = LocalAppColors.current.accentPrimary,
            style = Typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.app_name_p2),
            color = LocalAppColors.current.accentSecondary,
            style = Typography.headlineMedium
        )

        Spacer(Modifier.weight(1f))

        IconButton(onClick = {}) {
            Icon(
                painterResource(R.drawable.ic_search),
                tint = LocalAppColors.current.contentPrimary,
                contentDescription = "Search"
            )
        }
        IconButton(onClick = {}) {
            Icon(
                painterResource(R.drawable.ic_add),
                tint = LocalAppColors.current.contentPrimary,
                contentDescription = "Add"
            )
        }
        IconButton(onClick = {}) {
            Icon(
                painterResource(R.drawable.ic_menu),
                tint = LocalAppColors.current.contentPrimary,
                contentDescription = "Menu"
            )
        }
    }
}