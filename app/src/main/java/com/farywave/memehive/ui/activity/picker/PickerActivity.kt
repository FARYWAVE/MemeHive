package com.farywave.memehive.ui.activity.picker

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.farywave.memehive.MemeHiveApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme

class PickerActivity : ComponentActivity() {
    private var copying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MemeHiveTheme {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb()),
                    navigationBarStyle = SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb())
                )

                val defaultLabel = stringResource(R.string.hive_item)
                PickerScreen { item, onFailure ->
                    val image = item.image ?: return@PickerScreen
                    val label = item.caption.ifEmpty { defaultLabel }
                    copyToClipboard(label, image.id, onFailure)
                }
            }
        }
    }

    private fun copyToClipboard(label: String, imageId: Long, onFailure: () -> Unit) {
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
                finish()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("PickerActivity", "Could not copy image", error)
                onFailure()
            } finally {
                copying = false
            }
        }
    }
}
