package com.farywave.memehive.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.LocalAppColors


private val ripple = ripple(
    bounded = true,
    color = Color.White
)

@Composable
fun BasicIconButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    tint: Color = LocalAppColors.current.contentPrimary,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = icon, contentDescription = null, tint = tint)
    }
}