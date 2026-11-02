package com.farywave.memehive.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.domain.model.MediaItem
import com.farywave.memehive.ui.theme.MemeHiveTheme
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test

class MediaItemCardSizingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cardKeepsItsReservedHeightAfterLoadingAndRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("card-sizing", ".png", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(80, 160, Bitmap.Config.ARGB_8888)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val visible = mutableStateOf(true)
            val image = mutableStateOf(Image(uri = file.toUri(), width = 80, height = 160))
            val loaded = AtomicInteger()
            compose.setContent {
                MemeHiveTheme {
                    if (visible.value) {
                        Box(Modifier.width(120.dp).testTag("card")) {
                            SelectableMediaItemCard(
                                mediaItem = MediaItem(image = image.value),
                                isSelected = false,
                                onClick = {},
                                onLongClick = {},
                                onImageLoaded = { loaded.incrementAndGet() },
                            )
                        }
                    }
                }
            }
            compose.waitUntil(5_000) { loaded.get() > 0 }
            compose.onNodeWithTag("card").assertHeightIsEqualTo(240.dp)
            compose.runOnIdle { visible.value = false }
            compose.runOnIdle { visible.value = true }
            compose.onNodeWithTag("card").assertHeightIsEqualTo(240.dp)
            compose.waitUntil(5_000) { loaded.get() > 1 }
            compose.onNodeWithTag("card").assertHeightIsEqualTo(240.dp)
            // A loaded legacy image must also retain the fallback, regardless of intrinsic size.
            compose.runOnIdle { image.value = image.value.copy(width = null, height = null) }
            compose.onNodeWithTag("card").assertHeightIsEqualTo(80.dp)
        } finally {
            file.delete()
        }
    }
}
