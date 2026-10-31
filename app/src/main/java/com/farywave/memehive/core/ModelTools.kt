package com.farywave.memehive.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.farywave.memehive.R

object ModelTools {
    @Composable
    fun formatBytes(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes >= gb -> "${(bytes / gb).toInt()} ${stringResource(R.string.unit_gibibytes)}"
            bytes >= mb -> "${(bytes / mb).toInt()} ${stringResource(R.string.unit_mebibytes)}"
            bytes >= kb -> "${(bytes / kb).toInt()} ${stringResource(R.string.unit_kibibytes)}"
            else -> "$bytes ${stringResource(R.string.unit_bytes)}"
        }
    }

}
