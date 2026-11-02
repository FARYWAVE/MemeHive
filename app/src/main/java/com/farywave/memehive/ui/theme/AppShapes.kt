package com.farywave.memehive.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = CircleShape,
        extraLarge = RoundedCornerShape(28.dp),
    )

val EditingFieldVerticalPadding = 7.dp

/** Matches the caption field's capsule radius, including font scaling and its padding. */
@Composable
fun editingFieldShape(): CornerBasedShape {
    val textHeight =
        rememberTextMeasurer()
            .measure(
                text = "H",
                style = MaterialTheme.typography.bodyMedium,
            )
            .size
            .height
    val corner =
        CornerSize(
            with(LocalDensity.current) { (textHeight / 2).toDp() } + EditingFieldVerticalPadding
        )
    return MaterialTheme.shapes.medium.copy(
        topStart = corner,
        topEnd = corner,
        bottomStart = corner,
        bottomEnd = corner,
    )
}
