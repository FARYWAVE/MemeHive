package com.farywave.memehive.ui.component

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.MemeHiveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppTextFieldTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun materialFieldsKeepPreviousHeights() {
        compose.setContent {
            MemeHiveTheme {
                Column(Modifier.width(240.dp)) {
                    for (lines in listOf(1, 3)) {
                        BasicTextField(
                            state = rememberTextFieldState("Sample"),
                            modifier = Modifier.testTag("old-$lines"),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            lineLimits =
                                if (lines == 1) TextFieldLineLimits.SingleLine
                                else TextFieldLineLimits.MultiLine(minHeightInLines = lines),
                        )
                        AppTextField(
                            modifier = Modifier.testTag("new-$lines"),
                            hint = "Hint",
                            minLines = lines,
                            initialValue = "Sample",
                            onValueChange = {},
                        )
                    }
                }
            }
        }
        for (lines in listOf(1, 3)) {
            val before =
                compose.onNodeWithTag("old-$lines").fetchSemanticsNode().boundsInRoot.height
            val after = compose.onNodeWithTag("new-$lines").fetchSemanticsNode().boundsInRoot.height
            assertEquals("Height for $lines lines", before, after, 1f)
        }
    }

    @Test
    fun delayedScreenEchoDoesNotReplaceFocusedInput() {
        val screenValue = mutableStateOf("Before")
        var reported = ""
        compose.setContent {
            MemeHiveTheme {
                AppTextField(
                    modifier = Modifier.testTag("field"),
                    hint = "Hint",
                    initialValue = screenValue.value,
                    onValueChange = { reported = it },
                )
            }
        }
        compose.onNode(hasSetTextAction()).performClick().performTextReplacement("Latest text")
        compose.runOnIdle { screenValue.value = "Late" }
        compose.onNode(hasSetTextAction()).assertTextEquals("Latest text")
        compose.runOnIdle { assertEquals("Latest text", reported) }
    }

    @Test
    fun composingWordHasNoUnderlineAndCanStillBeReplaced() {
        lateinit var view: View
        lateinit var connection: InputConnection
        compose.setContent {
            view = LocalView.current
            MemeHiveTheme {
                AppTextField(hint = "Hint", initialValue = "", onValueChange = {})
            }
        }
        val field = compose.onNode(hasSetTextAction())
        field.performClick()
        compose.runOnIdle {
            connection = requireNotNull(view.onCreateInputConnection(EditorInfo()))
            connection.setComposingText("typ", 1)
        }
        field.assertTextEquals("typ")
        val layouts = mutableListOf<TextLayoutResult>()
        field.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val spans = layouts.single().layoutInput.text.spanStyles
        // Inspect the final style used to render the composing word.
        val renderedStyle =
            spans
                .filter { it.start <= 0 && it.end >= 3 }
                .fold(SpanStyle()) { result, span -> result.merge(span.item) }
        assertEquals(TextDecoration.None, renderedStyle.textDecoration)
        compose.runOnIdle { connection.setComposingText("typing", 1) }
        field.assertTextEquals("typing")
        compose.runOnIdle { connection.commitText("typed", 1) }
        field.assertTextEquals("typed")
    }
}
