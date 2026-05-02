package com.farywave.memehive.ui.navigation

sealed class NavEvent {
    data class ToEditing(val mediaItemId: Long) : NavEvent()
    object ToHive : NavEvent()
    object Back : NavEvent()

    object NewCollectionDialog : NavEvent()
}