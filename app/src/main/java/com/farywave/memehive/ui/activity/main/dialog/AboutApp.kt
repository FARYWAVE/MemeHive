package com.farywave.memehive.ui.activity.main.dialog

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farywave.memehive.R
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.core.ModelTools
import com.farywave.memehive.ui.component.LocalAppFeedback
import com.farywave.memehive.ui.simple_component.ActionType
import com.farywave.memehive.ui.simple_component.SimpleDialog
import com.farywave.memehive.ui.simple_component.SimpleDialogAction
import com.farywave.memehive.ui.theme.LocalAppColors
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlin.enums.enumEntries

@Composable
fun AboutApp(onDismissRequest: () -> Unit) {
    SimpleDialog(
        title = stringResource(R.string.action_about),
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
            enumEntries<AboutAppEntry>().forEach { entry ->
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

@Composable
private fun DirectorySize(directory: File) {
    val size by produceState<Long?>(initialValue = null, key1 = directory) {
        value = try {
            DeviceTools.dirSize(directory)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            android.util.Log.w("AboutApp", "Could not read directory size", failure)
            -1L
        }
    }
    Text(
        text = when (val bytes = size) {
            null -> stringResource(R.string.label_loading)
            -1L -> stringResource(R.string.label_unavailable)
            else -> ModelTools.formatBytes(bytes)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = LocalAppColors.current.contentPrimary
    )
}

enum class AboutAppEntry(@param:StringRes val title: Int, val content: @Composable () -> Unit) {
    VERSION(title = R.string.label_app_version, content = {
        val context = LocalContext.current
        Text(
            text = DeviceTools.getAppVersion(context),
            style = MaterialTheme.typography.bodyMedium,
            color = LocalAppColors.current.contentPrimary
        )
    }),

    STORAGE(title = R.string.label_storage, content = {
        val context = LocalContext.current
        DirectorySize(context.filesDir)
    }),

    CACHE(title = R.string.label_cache, content = {
        val context = LocalContext.current
        DirectorySize(context.cacheDir)
    }),

    SOURCE_CODE(title = R.string.label_source_code, content = {
        val context = LocalContext.current
        val feedback = LocalAppFeedback.current
        val link = stringResource(R.string.link_source_code)
        Box(Modifier.fillMaxWidth().wrapContentHeight(), Alignment.CenterStart) {
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .clickable { DeviceTools.openLink(context, link) { feedback.show(R.string.error_link_open) } },
                text = stringResource(R.string.label_github),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.accentPrimary
            )
        }
    }),

    DEVELOPER(title = R.string.label_developer, content = {
        val context = LocalContext.current
        val feedback = LocalAppFeedback.current
        val link = stringResource(R.string.link_developer)
        Box(Modifier.fillMaxWidth().wrapContentHeight(), Alignment.CenterStart) {
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .clickable { DeviceTools.openLink(context, link) { feedback.show(R.string.error_link_open) } },
                text = stringResource(R.string.label_developer_name),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAppColors.current.accentPrimary
            )
        }
    })
}

enum class AboutAppAction(@param:StringRes override val text: Int, override val type: ActionType) :
    SimpleDialogAction
