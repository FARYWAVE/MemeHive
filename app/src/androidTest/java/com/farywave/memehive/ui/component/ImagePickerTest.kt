package com.farywave.memehive.ui.component

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.MemeHiveTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ImagePickerTest {
    @get:Rule val compose = createComposeRule()
    private val removeLabel get() = InstrumentationRegistry.getInstrumentation()
        .targetContext.getString(R.string.action_remove_image)

    @Test fun removeButtonDoesNotInheritOuterWidthAndClearsTheImage() {
        val image = mutableStateOf<Uri?>(Uri.parse("content://test/image"))
        var calls = 0
        compose.setContent {
            MemeHiveTheme {
                ImagePicker(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    shape = RoundedCornerShape(12.dp),
                    initialSrc = image.value,
                ) {
                    calls++
                    image.value = it
                }
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                compose.onNodeWithContentDescription(removeLabel).fetchSemanticsNode()
            }.isSuccess
        }
        compose.onNodeWithContentDescription(removeLabel).assertWidthIsEqualTo(24.dp).performClick()
        compose.runOnIdle {
            assertNull(image.value)
            assertEquals(1, calls)
        }
        compose.onNodeWithContentDescription(removeLabel).assertDoesNotExist()
    }

    @Test fun disabledPickerCannotRemoveTheImage() {
        var calls = 0
        compose.setContent {
            MemeHiveTheme {
                ImagePicker(
                    shape = RoundedCornerShape(12.dp),
                    initialSrc = Uri.parse("content://test/image"),
                    enabled = false,
                ) { calls++ }
            }
        }
        compose.onNodeWithContentDescription(removeLabel).assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, calls) }
    }
}
