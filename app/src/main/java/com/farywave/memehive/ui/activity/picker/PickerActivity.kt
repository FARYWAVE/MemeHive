package com.farywave.memehive.ui.activity.picker

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.farywave.memehive.MemeHiveApplication
import com.farywave.memehive.R
import com.farywave.memehive.ui.component.AppFeedback
import com.farywave.memehive.ui.component.FeedbackHost
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickerActivity : ComponentActivity() {
    private val feedback = AppFeedback()
    private var copying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MemeHiveTheme {
                FeedbackHost(feedback) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb()),
                        navigationBarStyle = SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb())
                    )

                    val defaultLabel = stringResource(R.string.label_media_item)
                    PickerRoute { item ->
                        val image = item.image ?: return@PickerRoute
                        val label = item.caption.ifEmpty { defaultLabel }
                        copyToClipboard(label, image.id)
                    }
                }
            }
        }
    }

    private fun copyToClipboard(label: String, imageId: Long) {
        if (copying) return
        copying = true
        lifecycleScope.launch {
            try {
                val useCases = (applicationContext as MemeHiveApplication).container.mediaItemUseCases
                val uri = useCases.getImageSharingUri(imageId)
                val clip = withContext(Dispatchers.IO) {
                    ClipData.newUri(contentResolver, label, uri)
                }
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(clip)
                feedback.show(R.string.success_image_copied, onDismiss = { finish() })

            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("PickerActivity", "Could not copy image", error)
                feedback.show(R.string.error_image_copy)
            } finally {
                copying = false
            }
        }
    }
}
