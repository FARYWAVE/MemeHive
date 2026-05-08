package com.farywave.memehive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.farywave.memehive.core.ModelTools
import com.farywave.memehive.ui.main.dialogs.AboutApp
import com.farywave.memehive.ui.main.dialogs.NewCollection
import com.farywave.memehive.ui.main.dialogs.EditCollection
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
            is NavEvent.ToEditCollectionDialog -> navController.navigate(
                Screen.EditCollectionDialog.createRoute(
                    event.collectionName,
                    event.coverSrc,
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
            Hive(
                backStackEntry.savedStateHandle,
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
                savedStateHandle = backStackEntry.savedStateHandle,
                mediaItemId = mediaItemId,
                onNavigate = onNavigate
            )
        }

        dialog(Screen.NewCollectionDialog.route) {
            NewCollection(onDismissRequest = { navController.popBackStack() }) { name, coverSrc ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("newCollectionName", name)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("newCollectionSrc", coverSrc?.toString())

                navController.popBackStack()
            }
        }

        dialog(Screen.AboutAppDialog.route) {
            AboutApp(onDismissRequest = { navController.popBackStack() })
        }

        dialog(
            route = Screen.EditCollectionDialog.route,
            arguments = listOf(
                navArgument("collectionName") {
                    type = NavType.StringType
                },
                navArgument("coverSrc") {
                    type = NavType.StringType
                },
                navArgument("collectionId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val collectionName = backStackEntry.arguments?.getString("collectionName") ?: ""
            val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: -1L
            val coverSrc = ModelTools.decode(backStackEntry.arguments?.getString("coverSrc"))
            ModelTools.quickLog(coverSrc.toString())
            EditCollection(
                collectionName = collectionName,
                collectionCover = if (coverSrc.isNullOrEmpty()) null else coverSrc,
                onDismissRequest = { navController.popBackStack() },
                onConfirm = { newName, newCover ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("editedCollectionName", newName)

                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("editedCollectionCover", newCover?.toString())

                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("editedCollectionId", collectionId)

                    navController.popBackStack()
                }
            )
        }
    }
}