package com.dylandos.iptv.ui.navigation

import androidx.annotation.StringRes

/** Top-level destinations reachable from the collapsing navigation rail. */
enum class AppSection {
    LIVE_TV,
    MOVIES,
    SERIES,
    DVR,
    SETTINGS
}

/** A route on the app-level back stack. */
sealed class Destination {
    data object Section : Destination()
    data object ManageCategories : Destination()
    data object EpgGuide : Destination()
    data class Player(val channelId: Long) : Destination()
}

/** View state describing which rail item is expanded and what is selected. */
data class NavState(
    val section: AppSection = AppSection.LIVE_TV,
    val isRailExpanded: Boolean = false,
    val focusRestoreIndex: Int = 0
)
