package com.farywave.memehive.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.farywave.memehive.R
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
fun AppCheckbox(modifier: Modifier = Modifier, checked: Boolean, onClick: () -> Unit) {
    Icon(
        modifier = modifier.clickable { onClick() },
        painter =
            painterResource(
                if (checked) R.drawable.ic_checkbox_selected else R.drawable.ic_checkbox_blank
            ),
        tint =
            if (checked) LocalAppColors.current.accentSecondary
            else LocalAppColors.current.contentSecondary,
        contentDescription = null,
    )
}
