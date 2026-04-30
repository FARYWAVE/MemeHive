package com.farywave.memehive.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farywave.memehive.ui.theme.LocalAppColors


@Composable
inline fun <reified T> SimpleActionMenu(
    crossinline onSelected: (T) -> Unit,
    trigger: @Composable (onClick: () -> Unit) -> Unit
) where T: Enum<T>, T: ActionMenuOptions{
    var expanded by remember { mutableStateOf(false) }
    val items = enumValues<T>()

    Box {
        trigger { expanded = true }

        DropdownMenu(
            modifier = Modifier
                .background(
                    LocalAppColors.current.backgroundSecondary,
                    MaterialTheme.shapes.medium
                )
                .shadow(9.dp, MaterialTheme.shapes.medium, clip = false),
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { option ->
                DropdownMenuItem(
                    text = { Text(
                            text = option.label,
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
    val label: String
    val highlighted: Boolean
}