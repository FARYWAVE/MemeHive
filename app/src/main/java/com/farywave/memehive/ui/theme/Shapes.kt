package com.farywave.memehive.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val BeanShape = RoundedCornerShape(999.dp)

val SmallRounded = RoundedCornerShape(8.dp)
val MediumRounded = RoundedCornerShape(16.dp)
val LargeRounded = RoundedCornerShape(24.dp)


val AppShapes = Shapes(
    small = SmallRounded,
    medium = MediumRounded,
    large = LargeRounded,
    extraLarge = BeanShape
)