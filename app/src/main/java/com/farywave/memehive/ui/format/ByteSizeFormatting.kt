package com.farywave.memehive.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.farywave.memehive.R

@Composable
fun formatByteSize(bytes: Long): String {
    val kibibyte = 1024.0
    val mebibyte = kibibyte * 1024
    val gibibyte = mebibyte * 1024

    return when {
        bytes >= gibibyte ->
            "${(bytes / gibibyte).toInt()} ${stringResource(R.string.unit_gibibytes)}"
        bytes >= mebibyte ->
            "${(bytes / mebibyte).toInt()} ${stringResource(R.string.unit_mebibytes)}"
        bytes >= kibibyte ->
            "${(bytes / kibibyte).toInt()} ${stringResource(R.string.unit_kibibytes)}"
        else -> "$bytes ${stringResource(R.string.unit_bytes)}"
    }
}
