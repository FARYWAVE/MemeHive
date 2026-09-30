package com.farywave.memehive.ui.activity.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.farywave.memehive.core.DeviceTools
import com.farywave.memehive.ui.navigation.NavController
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                DeviceTools.showPickerNotification(
                    context = this,
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MemeHiveTheme {
                enableEdgeToEdge(
                    navigationBarStyle = SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb())
                )
                NavController { value ->
                    if (value) enablePicker()
                }
            }
        }
    }

    private fun enablePicker() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                DeviceTools.showPickerNotification(
                    context = this
                )

            } else {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }

        } else {

            DeviceTools.showPickerNotification(
                context = this
            )
        }
    }
}