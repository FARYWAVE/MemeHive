package com.farywave.memehive.ui.picker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme

class PickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MemeHiveTheme {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb()),
                    navigationBarStyle = SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb())
                )

                val defaultLabel = stringResource(R.string.hive_item)
                PickerScreen { item ->
                    val uri = Uri.fromFile(item.src!!)
                    val label = item.caption.ifEmpty { defaultLabel }
                    copyToClipboard(label, uri)
                }
            }
        }
    }

    private fun copyToClipboard(label: String, uri: Uri) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newUri(contentResolver, label, uri)

        clipboard.setPrimaryClip(clip)
    }
}