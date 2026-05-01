package com.farywave.memehive.ui.navigation

import android.R.attr.type
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.farywave.memehive.ui.main.screens.editing.Editing
import com.farywave.memehive.ui.main.screens.hive.Hive

@Composable
fun NavController() {
    val navController = rememberNavController()

    val onNavigate: (NavEvent) -> Unit = { event ->
        when (event) {
            NavEvent.ToHive -> navController.navigate(Screen.Hive.route)
            is NavEvent.ToEditing -> navController.navigate(Screen.Editing.createRoute(event.mediaItemId))
            NavEvent.Back -> navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Hive.route
    ) {
        composable(Screen.Hive.route) {
            Hive(
                onNavigate
            )
        }

        composable(
            route = Screen.Editing.route,
            arguments = listOf(
                navArgument("mediaItemId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            Editing(
                onNavigate = onNavigate
            )
        }
    }
}