package com.farywave.memehive.ui.navigation

import android.net.Uri

sealed class NavEvent {
    object ToHive : NavEvent()

    data class ToEditing(val mediaItemId: Long) : NavEvent()

    object Back : NavEvent()

    object NewCollectionDialog : NavEvent()

    data class ToEditCollectionDialog(val collectionName: String, val coverSrc: Uri?, val collectionId: Long) : NavEvent()

    object AboutAppDialog : NavEvent()
}