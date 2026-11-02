package com.farywave.memehive.ui.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTextReplacement
import com.farywave.memehive.ui.theme.MemeHiveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun typingAndUnfocusedExternalChangesStaySynchronized() {
        val query = mutableStateOf("initial")
        lateinit var focusManager: FocusManager
        compose.setContent {
            focusManager = LocalFocusManager.current
            MemeHiveTheme {
                SearchBar(hint = "Search", initialQuery = query.value) { query.value = it }
            }
        }
        val field = compose.onNode(hasSetTextAction())
        field.assertTextEquals("initial")
        field.performTextReplacement("funny")
        compose.waitUntil(5_000) { query.value == "funny" }
        // Focused fields intentionally protect active input from stale screen updates.
        // External values are applied once the user has left the field.
        compose.runOnIdle {
            focusManager.clearFocus()
            query.value = "cats"
        }
        field.assertTextEquals("cats")
        compose.runOnIdle { query.value = "" }
        field.assertTextEquals("")
        compose.runOnIdle { assertEquals("", query.value) }
    }
}
