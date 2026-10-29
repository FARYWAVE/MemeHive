package com.farywave.memehive.ui.component

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppFeedbackTest {
    @Test fun unacknowledgedMessagesSurviveCollectorReplacementAndIdenticalMessagesAreDistinct() = runTest {
        val feedback = AppFeedback()
        feedback.show(1)
        val first = feedback.messages.first()
        feedback.show(1)
        assertSame(first, feedback.messages.first())
        feedback.dismiss(first)
        val second = feedback.messages.first()
        assertNotSame(first, second)
        feedback.dismiss(first)
        assertSame(second, feedback.messages.first())
    }

    @Test fun cancelledActionsAreNotConvertedIntoErrors() {
        val failure = CancellationException("cancelled")
        try {
            AppFeedback().attempt(1) { throw failure }
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(failure, actual)
        }
    }
}
