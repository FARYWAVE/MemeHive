package com.farywave.memehive.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.farywave.memehive.ui.main.screens.hive.Hive
import com.farywave.memehive.ui.main.screens.hive.HiveViewModel
import com.farywave.memehive.ui.theme.LocalAppColors
import com.farywave.memehive.ui.theme.MemeHiveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: HiveViewModel by viewModels()

        setContent {
            MemeHiveTheme {
                enableEdgeToEdge(
                    navigationBarStyle = SystemBarStyle.dark(LocalAppColors.current.backgroundPrimary.toArgb())
                )
                Hive(viewModel)
            }
        }
        viewModel.loadCollections()
        viewModel.loadMediaItems()
    }
}