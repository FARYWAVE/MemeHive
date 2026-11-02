package com.farywave.memehive.ui.collection

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.R
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionDraft
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorDialog
import com.farywave.memehive.ui.activity.main.dialog.collection_editor.CollectionEditorState
import com.farywave.memehive.ui.theme.MemeHiveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CollectionEditorDialogTest {
    @get:Rule val compose = createComposeRule()

    private fun text(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun submitIncludesTheLatestNameAndCover() {
        var submitted: CollectionDraft? = null
        compose.setContent {
            MemeHiveTheme {
                CollectionEditorDialog(
                    state =
                        CollectionEditorState(
                            "new",
                            initialDraft = CollectionDraft(coverUri = "content://test/cover"),
                        ),
                    onSubmit = { submitted = it },
                    onDismissRequest = {},
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("Cats / dogs ? #")
        Espresso.closeSoftKeyboard()
        // Test the submitted snapshot independently of the device keyboard's window animation.
        compose.onNodeWithText(text(R.string.action_create)).performSemanticsAction(
            SemanticsActions.OnClick
        ) {
            it()
        }
        compose.runOnIdle {
            assertEquals(CollectionDraft("Cats / dogs ? #", "content://test/cover"), submitted)
        }
    }

    @Test
    fun localDraftSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        var submitted: CollectionDraft? = null
        restoration.setContent {
            MemeHiveTheme {
                CollectionEditorDialog(
                    state = CollectionEditorState("restored", 7L, CollectionDraft("Before")),
                    onSubmit = { submitted = it },
                    onDismissRequest = {},
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("Unsaved name")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasText(text(R.string.action_save)) and hasClickAction()).performClick()
        compose.runOnIdle { assertEquals(CollectionDraft("Unsaved name"), submitted) }
    }

    @Test
    fun savingDisablesFieldsAndActionsAndFailureAllowsRetry() {
        val state = mutableStateOf(CollectionEditorState("saving"))
        compose.setContent {
            MemeHiveTheme {
                CollectionEditorDialog(state.value, onSubmit = {}, onDismissRequest = {})
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("Keep my draft")
        compose.runOnIdle { state.value = state.value.copy(isSaving = true) }
        compose.onNodeWithText("Keep my draft").assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.action_create)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.action_cancel)).assertIsNotEnabled()
        compose.runOnIdle {
            state.value =
                state.value.copy(isSaving = false, errorResId = R.string.error_collection_create)
        }
        compose.onNodeWithText("Keep my draft").assertExists()
        compose.onNodeWithText(text(R.string.error_collection_create)).assertExists()
    }

    @Test
    fun removingCoverSubmitsNullWithoutLosingTheName() {
        var submitted: CollectionDraft? = null
        compose.setContent {
            MemeHiveTheme {
                CollectionEditorDialog(
                    state =
                        CollectionEditorState(
                            "edit",
                            7L,
                            CollectionDraft("Cats", "content://test/cover"),
                        ),
                    onSubmit = { submitted = it },
                    onDismissRequest = {},
                )
            }
        }
        compose.onNodeWithContentDescription(text(R.string.action_remove_image)).performClick()
        compose.onNode(hasText(text(R.string.action_save)) and hasClickAction()).performClick()
        compose.runOnIdle {
            assertEquals("Cats", submitted?.name)
            assertNull(submitted?.coverUri)
        }
    }
}
