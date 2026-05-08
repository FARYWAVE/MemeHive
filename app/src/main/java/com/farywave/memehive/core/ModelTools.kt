package com.farywave.memehive.core

import android.net.Uri
import android.util.Log
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
            bytes >= gb -> "${(bytes / gb).toInt()} ${stringResource(R.string.gigabyte)}"
            bytes >= mb -> "${(bytes / mb).toInt()} ${stringResource(R.string.megabyte)}"
            bytes >= kb -> "${(bytes / kb).toInt()} ${stringResource(R.string.kilobyte)}"
            else -> "$bytes ${stringResource(R.string.__byte)}"
        }
    }

    fun encode(arg: String?) = Uri.encode(arg ?: "")
    fun decode(arg: String?) = Uri.decode(arg ?: "")

    fun quickLog(message: String) = Log.d("MH_DEBUG", message)
}