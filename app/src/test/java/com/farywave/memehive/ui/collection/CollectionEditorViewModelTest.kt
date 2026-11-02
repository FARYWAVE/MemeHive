package com.farywave.memehive.ui.collection

import androidx.lifecycle.SavedStateHandle
import com.farywave.memehive.R
import com.farywave.memehive.domain.model.Collection
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionDraft
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank name keeps the form open without writing`() =
        runTest(dispatcher) {
            var calls = 0
            val editor = CollectionEditorViewModel(SavedStateHandle()) { _, _ -> calls++ }
            editor.open()
            editor.submit(CollectionDraft("   ", "content://cover"))
            runCurrent()
            assertEquals(0, calls)
            assertEquals(R.string.error_collection_name_required, editor.state.value?.errorResId)
            assertEquals("content://cover", editor.state.value?.initialDraft?.coverUri)
            assertFalse(editor.state.value!!.isSaving)
        }

    @Test
    fun `submits name and cover together and closes only after success`() =
        runTest(dispatcher) {
            val completion = CompletableDeferred<Unit>()
            val received = mutableListOf<Pair<Long?, CollectionDraft>>()
            val editor =
                CollectionEditorViewModel(SavedStateHandle()) { id, draft ->
                    received += id to draft
                    completion.await()
                }
            editor.open()
            editor.submit(CollectionDraft("  Cats / dogs ? #  ", "content://cover/1"))
            editor.submit(CollectionDraft("duplicate"))
            editor.dismiss()
            editor.open(Collection(id = 12, name = "Another"))
            runCurrent()
            assertEquals(
                listOf(null to CollectionDraft("Cats / dogs ? #", "content://cover/1")),
                received,
            )
            assertTrue(editor.state.value!!.isSaving)
            completion.complete(Unit)
            runCurrent()
            assertNull(editor.state.value)
        }

    @Test
    fun `edit submits the original id and supports removing a cover`() =
        runTest(dispatcher) {
            var received: Pair<Long?, CollectionDraft>? = null
            val editor =
                CollectionEditorViewModel(SavedStateHandle()) { id, draft ->
                    received = id to draft
                }
            editor.open(Collection(id = 42, name = "Before"))
            editor.submit(CollectionDraft("After", coverUri = null))
            runCurrent()
            assertEquals(42L to CollectionDraft("After", null), received)
            assertNull(editor.state.value)
        }

    @Test
    fun `failure retains the draft and a retry can succeed`() =
        runTest(dispatcher) {
            var calls = 0
            val editor =
                CollectionEditorViewModel(SavedStateHandle()) { _, _ ->
                    if (++calls == 1) error("storage unavailable")
                }
            val draft = CollectionDraft("Cats", "content://cover")
            editor.open()
            val session = editor.state.value!!.sessionId
            editor.submit(draft)
            runCurrent()
            assertEquals(draft, editor.state.value?.initialDraft)
            assertEquals(session, editor.state.value?.sessionId)
            assertEquals(R.string.error_collection_create, editor.state.value?.errorResId)
            assertFalse(editor.state.value!!.isSaving)
            editor.submit(draft)
            runCurrent()
            assertEquals(2, calls)
            assertNull(editor.state.value)
        }

    @Test
    fun `failed edits use an edit error and preserve their target`() =
        runTest(dispatcher) {
            val editor = CollectionEditorViewModel(SavedStateHandle()) { _, _ -> error("failed") }
            editor.open(Collection(id = 42, name = "Before"))
            editor.submit(CollectionDraft("After"))
            runCurrent()
            assertEquals(42L, editor.state.value?.collectionId)
            assertEquals(R.string.error_collection_update, editor.state.value?.errorResId)
        }

    @Test
    fun `cancel then reopen starts a new empty session without writing`() =
        runTest(dispatcher) {
            var calls = 0
            val editor = CollectionEditorViewModel(SavedStateHandle()) { _, _ -> calls++ }
            editor.open(Collection(id = 42, name = "Before"))
            val previous = editor.state.value!!.sessionId
            editor.dismiss()
            editor.open()
            runCurrent()
            assertEquals(0, calls)
            assertNull(editor.state.value?.collectionId)
            assertEquals(CollectionDraft(), editor.state.value?.initialDraft)
            assertNotEquals(previous, editor.state.value?.sessionId)
        }

    @Test
    fun `restoration reopens the same form without replaying a pending save`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val completion = CompletableDeferred<Unit>()
            val editor = CollectionEditorViewModel(handle) { _, _ -> completion.await() }
            editor.open(Collection(id = 7, name = "Before"))
            editor.submit(CollectionDraft("After", "content://cover"))
            runCurrent()
            val snapshot = handle.keys().associateWith { handle.get<Any?>(it) }
            var restoredCalls = 0
            val restored =
                CollectionEditorViewModel(SavedStateHandle(snapshot)) { _, _ -> restoredCalls++ }
            assertEquals(editor.state.value!!.sessionId, restored.state.value!!.sessionId)
            assertEquals(
                CollectionDraft("After", "content://cover"),
                restored.state.value?.initialDraft,
            )
            assertFalse(restored.state.value!!.isSaving)
            runCurrent()
            assertEquals(0, restoredCalls)
            completion.complete(Unit)
            runCurrent()
        }

    @Test
    fun `successful save is not reopened by restoration`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val editor = CollectionEditorViewModel(handle) { _, _ -> }
            editor.open()
            editor.submit(CollectionDraft("Cats"))
            runCurrent()
            val snapshot = handle.keys().associateWith { handle.get<Any?>(it) }
            assertNull(
                CollectionEditorViewModel(SavedStateHandle(snapshot)) { _, _ -> }.state.value
            )
        }
}
