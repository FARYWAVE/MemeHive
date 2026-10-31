package com.farywave.memehive.ui.component

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update


class AppFeedback {
    private val queue = MutableStateFlow<List<Message>>(emptyList())
    internal val messages = queue.mapNotNull { it.firstOrNull() }.distinctUntilChanged()

    internal fun dismiss(message: Message) {
        queue.update { it.filterNot { queued -> queued === message } }
    }

    fun show(@StringRes text: Int, @StringRes action: Int? = null, onAction: (() -> Unit)? = null,
             onDismiss: (() -> Unit)? = null) {
        val message = Message(text, action, onAction, onDismiss)
        queue.update { it + message }
    }

    fun attempt(@StringRes error: Int, operation: () -> Unit) {
        try {
            operation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w("AppFeedback", "Could not complete action", failure)
            show(error)
        }
    }

    internal class Message(@param:StringRes val text: Int, @param:StringRes val action: Int?,
                               val onAction: (() -> Unit)?, val onDismiss: (() -> Unit)?)
}

val LocalAppFeedback = staticCompositionLocalOf { AppFeedback() }

@Composable
fun FeedbackHost(
    feedback: AppFeedback = remember { AppFeedback() },
    overlay: Boolean = true,
    content: @Composable () -> Unit,
) {
    val host = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val configuration = LocalConfiguration.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(feedback, lifecycle, configuration) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            feedback.messages.collect { message ->
                val result = host.showSnackbar(
                    message = resources.getString(message.text),
                    actionLabel = message.action?.let(resources::getString),
                    withDismissAction = true,
                    duration = if (message.action != null) SnackbarDuration.Long else SnackbarDuration.Short,
                )
                feedback.dismiss(message)
                if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
                message.onDismiss?.invoke()
            }
        }
    }
    CompositionLocalProvider(LocalAppFeedback provides feedback) {
        if (overlay) {
            Box {
                content()
                FeedbackSnackbar(
                    host,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding(),
                )
            }
        } else {
            Column {
                content()
                FeedbackSnackbar(host)
            }
        }
    }
}

@Composable
private fun FeedbackSnackbar(host: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(host, modifier) {
        Snackbar(
            it,
            containerColor = LocalAppColors.current.backgroundSecondary,
            contentColor = LocalAppColors.current.contentPrimary,
            actionColor = LocalAppColors.current.accentPrimary,
        )
    }
}


@Composable
fun ErrorNotice(@StringRes message: Int, onRetry: (() -> Unit)? = null, @StringRes actionLabel: Int = R.string.action_retry) {
    Surface(color = LocalAppColors.current.warning.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_error), contentDescription = null,
                tint = LocalAppColors.current.warning, modifier = Modifier.size(24.dp))
            Text(stringResource(message), modifier = Modifier.weight(1f),
                color = LocalAppColors.current.contentPrimary, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) TextButton(onClick = onRetry) { Text(stringResource(actionLabel)) }
        }
    }
}
