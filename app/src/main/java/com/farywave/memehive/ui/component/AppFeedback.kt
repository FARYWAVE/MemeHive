package com.farywave.memehive.ui.component

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update


class AppFeedback {
    private val queue = MutableStateFlow<List<Message>>(emptyList())
    internal val messages = queue.map { it.firstOrNull() }.filterNotNull().distinctUntilChanged()

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
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(feedback, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            feedback.messages.collect { message ->
                val result = host.showSnackbar(
                    message = context.getString(message.text),
                    actionLabel = message.action?.let(context::getString),
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
