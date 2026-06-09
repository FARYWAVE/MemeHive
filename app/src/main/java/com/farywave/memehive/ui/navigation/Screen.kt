package com.farywave.memehive.ui.navigation

import android.net.Uri
import com.farywave.memehive.core.ModelTools

sealed class Screen(val route: String) {
    object Hive : Screen("hive")
    object Editing : Screen("editing/{mediaItemId}") {
        fun createRoute(mediaItemId: Long) = "editing/$mediaItemId"
    }

    object NewCollectionDialog : Screen("new_collection_dialog")

    object AboutAppDialog : Screen("about_app")

    object EditCollectionDialog :
        Screen("edit_collection_dialog/{collectionName}/{coverSrc}/{collectionId}") {
        fun createRoute(collectionName: String, coverSrc: Uri?, collectionId: Long) =
            "edit_collection_dialog/$collectionName/${ModelTools.encode(coverSrc?.toString())}/$collectionId"
    }

    object ActivateSubscriptionDialog : Screen("activate_subscription_dialog")
}