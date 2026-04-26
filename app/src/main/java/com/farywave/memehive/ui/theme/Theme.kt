package com.farywave.memehive.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.farywave.memehive.R

@Composable
fun MemeHiveTheme(
    content: @Composable () -> Unit
) {
    val colors = AppColors(
        accentPrimary = colorResource(R.color.violet),
        accentSecondary = colorResource(R.color.green),
        contentPrimary = colorResource(R.color.white),
        contentSecondary = colorResource(R.color.light_gray),
        backgroundPrimary = colorResource(R.color.dark_gray),
        backgroundSecondary = colorResource(R.color.gray),
        error = colorResource(R.color.red),
        tint = colorResource(R.color.black).copy(alpha = 0.75f),
        transparent = Color.Transparent
    )

    CompositionLocalProvider(
        LocalAppColors provides colors
    ) {
        MaterialTheme(
            shapes = AppShapes,
            typography = Typography,
            content = content
        )
    }
}