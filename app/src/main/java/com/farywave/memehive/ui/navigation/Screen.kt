package com.farywave.memehive.ui.navigation

sealed class Screen(val route: String) {
    object Hive : Screen("hive")
    object Editing : Screen("editing/{mediaItemId}") {
        fun createRoute(mediaItemId: Long) = "editing/$mediaItemId"
    }

    object NewCollectionDialog : Screen("new_collection_dialog")
}