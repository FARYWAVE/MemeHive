package com.farywave.memehive.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.farywave.memehive.domain.model.Image
import com.farywave.memehive.ui.activity.main.screen.editing.EditingRoute
import com.farywave.memehive.ui.activity.main.screen.hive.HiveRoute

@Composable
fun AppNavigation(
    initialUri: Uri?,
    onEnableNotification: (Boolean) -> Unit,
    onCopyImage: (Image) -> Unit,
) {
    val controller = rememberNavController()
    NavHost(
        navController = controller,
        startDestination = if (initialUri != null) EditingDestination() else HiveDestination,
    ) {
        composable<HiveDestination> { entry ->
            HiveRoute(
                onEnableNotification = onEnableNotification,
                onOpenMedia = { id ->
                    if (
                        controller.currentBackStackEntry == entry &&
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
                initialUri = initialUri,
                onCopyImage = onCopyImage,
                mediaItemId = destination.mediaItemId ?: 0L,
                onClose = {
                    if (controller.currentBackStackEntry == entry && !controller.popBackStack()) {
                        controller.navigate(HiveDestination) { launchSingleTop = true }
                    }
                },
            )
        }
    }
}
