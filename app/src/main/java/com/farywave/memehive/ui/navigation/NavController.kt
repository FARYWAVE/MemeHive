package com.farywave.memehive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.farywave.memehive.ui.main.dialogs.AboutApp
import com.farywave.memehive.ui.main.dialogs.NewCollection
import com.farywave.memehive.ui.main.dialogs.RenameCollection
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
            NavEvent.NewCollectionDialog -> navController.navigate(Screen.NewCollectionDialog.route)
            NavEvent.AboutAppDialog -> navController.navigate(Screen.AboutAppDialog.route)
            is NavEvent.ToRenameCollectionDialog -> navController.navigate(
                Screen.RenameCollectionDialog.createRoute(
                    event.collectionName,
                    event.collectionId
                )
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Hive.route
    ) {
        composable(Screen.Hive.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Hive.route)
            }
            Hive(
                parentEntry.savedStateHandle,
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
            val mediaItemId = backStackEntry.arguments?.getLong("mediaItemId") ?: -1L

            Editing(
                mediaItemId = mediaItemId,
                onNavigate = onNavigate
            )
        }

        dialog(Screen.NewCollectionDialog.route) {
            NewCollection(onDismissRequest = { navController.popBackStack() }) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("newCollectionName", it)

                navController.popBackStack()
            }
        }

        dialog(Screen.AboutAppDialog.route) {
            AboutApp(onDismissRequest = { navController.popBackStack() })
        }

        dialog(
            route = Screen.RenameCollectionDialog.route,
            arguments = listOf(
                navArgument("collectionName") {
                    type = NavType.StringType
                },
                navArgument("collectionId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
            val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: -1L
            RenameCollection(
                collectionName = collectionName,
                onDismissRequest = { navController.popBackStack() },
                onConfirm = { newName ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("renamedCollectionName", newName)

                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("renamedCollectionId", collectionId)

                    navController.popBackStack()
                }
            )
        }
    }
}