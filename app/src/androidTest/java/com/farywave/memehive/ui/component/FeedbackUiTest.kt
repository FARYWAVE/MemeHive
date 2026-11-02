package com.farywave.memehive.ui.component

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.MemeHiveTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FeedbackUiTest {
    @get:Rule val compose = createComposeRule()
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun unreadableImageShowsErrorPlaceholderAndClearingItRestoresEmptyPlaceholder() {
        val uri = mutableStateOf<Uri?>(Uri.parse("file:///missing-memehive-test-image.png"))
        compose.setContent {
            MemeHiveTheme {
                ImageWithPlaceholder(Modifier.width(240.dp), RoundedCornerShape(12.dp), uri.value)
            }
        }
        val error = context.getString(R.string.error_image_unavailable)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription(error).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(error).assertIsDisplayed()
        compose.runOnIdle { uri.value = null }
        compose
            .onNodeWithContentDescription(context.getString(R.string.label_no_image))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(error).assertDoesNotExist()
    }

    @Test
    fun snackbarActionWorksAndNextQueuedMessageIsShown() {
        val feedback = AppFeedback()
        var actions = 0
        compose.setContent {
            MemeHiveTheme { FeedbackHost(feedback) { Box(Modifier.fillMaxSize()) } }
        }
        compose.runOnIdle {
            feedback.show(
                R.string.error_notification_permission,
                R.string.action_settings,
                onAction = { actions++ },
            )
            feedback.show(R.string.success_collection_imported)
        }
        compose
            .onNodeWithText(context.getString(R.string.error_notification_permission))
            .assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.action_settings)).performClick()
        compose
            .onNodeWithText(context.getString(R.string.success_collection_imported))
            .assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, actions) }
    }
}
