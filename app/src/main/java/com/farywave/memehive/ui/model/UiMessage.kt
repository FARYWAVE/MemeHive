package com.farywave.memehive.ui.model

import androidx.annotation.StringRes
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Every occurrence has an identity, including repeated messages with the same text. */
data class UiMessage(@param:StringRes val messageResId: Int, val id: String = UUID.randomUUID().toString())

class UiMessageQueue {
    private val pending = ArrayDeque<UiMessage>()
    private val _current = MutableStateFlow<UiMessage?>(null)
    val current = _current.asStateFlow()

    @Synchronized
    fun show(@StringRes message: Int) {
        pending.addLast(UiMessage(message))
        _current.value = pending.first()
    }

    @Synchronized
    fun dismiss(id: String) {
        if (pending.firstOrNull()?.id != id) return
        pending.removeFirst()
        _current.value = pending.firstOrNull()
    }
}
