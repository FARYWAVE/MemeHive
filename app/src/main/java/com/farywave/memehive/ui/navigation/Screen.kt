package com.farywave.memehive.ui.navigation

sealed class Screen(val route: String) {
    object Hive : Screen("hive")
    object Editing : Screen("editing/{mediaItemId}") {
        fun createRoute(mediaItemId: Long) = "editing/$mediaItemId"
    }

    object NewCollectionDialog : Screen("new_collection_dialog")

    object AboutAppDialog : Screen("about_app")

    object RenameCollectionDialog :
        Screen("rename_collection_dialog/{collectionName}/{collectionId}") {
        fun createRoute(collectionName: String, collectionId: Long) =
            "rename_collection_dialog/$collectionName/$collectionId"
    }
}