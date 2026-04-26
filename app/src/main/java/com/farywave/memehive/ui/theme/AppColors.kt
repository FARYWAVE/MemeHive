package com.farywave.memehive.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class AppColors(
    val accentPrimary: Color,
    val accentSecondary: Color,

    val contentPrimary: Color,
    val contentSecondary: Color,

    val backgroundPrimary: Color,
    val backgroundSecondary: Color,

    val error: Color,
    val tint: Color,
    val transparent: Color
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}