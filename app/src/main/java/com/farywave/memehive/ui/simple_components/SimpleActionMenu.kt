package com.farywave.memehive.ui.simple_components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.LocalAppColors


@Composable
inline fun <reified T> SimpleActionMenu(
    crossinline onSelected: (T) -> Unit,
    trigger: @Composable (onClick: () -> Unit) -> Unit
) where T: Enum<T>, T: ActionMenuOptions {
    var expanded by remember { mutableStateOf(false) }
    val items = enumValues<T>()

    Box {
        trigger { expanded = true }

        DropdownMenu(
            shape = MaterialTheme.shapes.medium,
            containerColor = LocalAppColors.current.backgroundSecondary,
            tonalElevation = 0.dp,
            shadowElevation = 9.dp,
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { option ->
                DropdownMenuItem(
                    text = { Text(
                            text = stringResource(option.labelId),
                            color = if (option.highlighted)
                                LocalAppColors.current.warning
                            else LocalAppColors.current.contentPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        ) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

interface ActionMenuOptions {
    val labelId: Int
    val highlighted: Boolean
}