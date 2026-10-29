package com.farywave.memehive.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HiveDestination

@Serializable
data class EditingDestination(val mediaItemId: Long? = null)
