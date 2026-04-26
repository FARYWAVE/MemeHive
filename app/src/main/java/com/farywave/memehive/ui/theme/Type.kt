package com.farywave.memehive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.res.fontResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.farywave.memehive.R


val BalsamiqSans = FontFamily(
    Font(R.font.balsamiq_sans_regular, FontWeight.Normal),
    Font(R.font.balsamiq_sans_bold, FontWeight.Bold)
)
val Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = BalsamiqSans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.01.em
    ),
    titleMedium = TextStyle(
        fontFamily = BalsamiqSans,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.01.em
    ),

    bodyMedium = TextStyle(
        fontFamily = BalsamiqSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.01.em
    ),

    labelMedium = TextStyle(
        fontFamily = BalsamiqSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.01.em
    ),
)