package com.farywave.memehive.ui.main.dialogs

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.ui.components.ActionType
import com.farywave.memehive.ui.components.SimpleDialog
import com.farywave.memehive.ui.components.SimpleDialogAction
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun AboutApp(onDismissRequest: () -> Unit) {
    SimpleDialog(
        title = stringResource(R.string.action_app_info),
        actions = emptyArray<AboutAppAction>(),
        onDismissRequest = onDismissRequest,
        onAction = {},
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            enumValues<AboutAppEntry>().forEach { entry ->
                item {
                    Text(
                        text = stringResource(entry.title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalAppColors.current.contentSecondary
                    )
                }
                item { entry.content() }
            }
        }
    }
}


enum class AboutAppEntry(@StringRes val title: Int, val content: @Composable () -> Unit) {
    VERSION(title = R.string.about_app_version, content = {
        val context = LocalContext.current
        Text(
            text = DeviceTools.getAppVersion(context),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary
        )
    }),

    STORAGE(title = R.string.about_app_storage, content = {
        val context = LocalContext.current
        Text(
            text = DeviceTools.formatBytes(DeviceTools.dirSize(context.filesDir)),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary
        )
    }),

    CACHE(title = R.string.about_app_cache, content = {
        val context = LocalContext.current
        Text(
            text = DeviceTools.formatBytes(DeviceTools.dirSize(context.cacheDir)),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary
        )
    }),

    SOURCE_CODE(title = R.string.about_app_source_code, content = {
        val context = LocalContext.current
        val link = stringResource(R.string.link_source_code)
        Box(Modifier.fillMaxWidth().wrapContentHeight(), Alignment.CenterStart) {
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .clickable { DeviceTools.openLink(context, link) },
                text = stringResource(R.string.git_hub),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.accentPrimary
            )
        }
    }),

    DEVELOPER(title = R.string.about_app_created_by, content = {
        val context = LocalContext.current
        val link = stringResource(R.string.link_developer)
        Box(Modifier.fillMaxWidth().wrapContentHeight(), Alignment.CenterStart) {
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .clickable { DeviceTools.openLink(context, link) },
                text = stringResource(R.string.developer),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.accentPrimary
            )
        }
    })
}

enum class AboutAppAction(@StringRes override val text: Int, override val type: ActionType) :
    SimpleDialogAction