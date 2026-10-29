package com.farywave.memehive.ui.model

import org.junit.Assert.*
import org.junit.Test

class UiMessageQueueTest {
    @Test fun repeatedFailuresAreDeliveredInOrderAndStaleDismissalsDoNotRemoveTheNextMessage() {
        val queue = UiMessageQueue()
        queue.show(1)
        val first = queue.current.value!!
        queue.show(1)
        queue.show(2)
        assertEquals(first, queue.current.value)
        queue.dismiss(first.id)
        val second = queue.current.value!!
        assertEquals(1, second.messageResId)
        assertNotEquals(first.id, second.id)
        queue.dismiss(first.id)
        assertEquals(second, queue.current.value)
        queue.dismiss(second.id)
        assertEquals(2, queue.current.value!!.messageResId)
        queue.dismiss(queue.current.value!!.id)
        assertNull(queue.current.value)
    }
}
