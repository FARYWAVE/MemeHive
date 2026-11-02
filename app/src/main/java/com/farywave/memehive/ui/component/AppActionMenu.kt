package com.farywave.memehive.ui.component

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.farywave.memehive.ui.theme.LocalAppColors

@Composable
inline fun <reified T> AppActionMenu(
    modifier: Modifier = Modifier,
    options: List<T> = enumValues<T>().toList(),
    properties: PopupProperties = PopupProperties(focusable = true),
    crossinline onSelected: (T) -> Unit,
    trigger: @Composable (onClick: () -> Unit) -> Unit,
) where T : Enum<T>, T : ActionMenuOption {
    var expanded by remember { mutableStateOf(false) }

    Box {
        trigger { expanded = true }

        DropdownMenu(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            containerColor = LocalAppColors.current.backgroundSecondary,
            tonalElevation = 0.dp,
            shadowElevation = 9.dp,
            expanded = expanded,
            properties = properties,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelResId),
                            color =
                                if (option.isDestructive) LocalAppColors.current.warning
                                else LocalAppColors.current.contentPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

interface ActionMenuOption {
    val labelResId: Int
    val isDestructive: Boolean
}
