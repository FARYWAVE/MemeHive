package com.farywave.memehive.ui.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTextReplacement
import com.farywave.memehive.ui.theme.MemeHiveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun typingAndExternalChangesStaySynchronized() {
        val query = mutableStateOf("initial")
        compose.setContent {
            MemeHiveTheme {
                SearchBar(hint = "Search", initialQuery = query.value) { query.value = it }
            }
        }
        val field = compose.onNode(hasSetTextAction())
        field.assertTextEquals("initial")
        field.performTextReplacement("funny")
        compose.waitUntil(5_000) { query.value == "funny" }
        compose.runOnIdle { query.value = "cats" }
        field.assertTextEquals("cats")
        compose.runOnIdle { query.value = "" }
        field.assertTextEquals("")
        compose.runOnIdle { assertEquals("", query.value) }
    }
}
