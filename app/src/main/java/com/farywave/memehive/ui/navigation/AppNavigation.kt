package com.farywave.memehive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.farywave.memehive.ui.activity.main.screen.editing.EditingRoute
import com.farywave.memehive.ui.activity.main.screen.hive.HiveRoute

@Composable
fun AppNavigation(onEnableNotification: (Boolean) -> Unit) {
    val controller = rememberNavController()
    NavHost(navController = controller, startDestination = HiveDestination) {
        composable<HiveDestination> { entry ->
            HiveRoute(
                onEnableNotification = onEnableNotification,
                onOpenMedia = { id ->
                    if (controller.currentBackStackEntry == entry &&
                        entry.lifecycle.currentState == Lifecycle.State.RESUMED
                    ) {
                        controller.navigate(EditingDestination(id)) { launchSingleTop = true }
                    }
                },
            )
        }
        composable<EditingDestination> { entry ->
            val destination = entry.toRoute<EditingDestination>()
            EditingRoute(
                mediaItemId = destination.mediaItemId ?: 0L,
                onClose = {
                    if (controller.currentBackStackEntry == entry) controller.popBackStack()
                },
            )
        }
    }
}
