package com.farywave.memehive.ui.navigation

import android.net.Uri

sealed class NavEvent {
    data class ToEditing(val mediaItemId: Long) : NavEvent()
    object ToHive : NavEvent()
    object Back : NavEvent()

    object NewCollectionDialog : NavEvent()

    object AboutAppDialog : NavEvent()

    data class ToEditCollectionDialog(val collectionName: String, val coverSrc: Uri?, val collectionId: Long) : NavEvent()
}