package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.serialization.Serializable

/**
 * Sealed class representing all navigation destinations in the app
 */
@Serializable
sealed class QamusDestinations() {

    /**
     * Main navigation graph - contains screens without bottom bar
     */
    @Serializable
    data object Main : QamusDestinations()

    /**
     * Home navigation graph - contains screens with bottom bar
     */
    @Serializable
    data object Home : QamusDestinations()

    /**
     * Dashboard screen - main/home page of the app
     */
    @Serializable
    data object Dashboard : QamusDestinations()

    /**
     * Dictionary screen - screen showing the list of entries
     */
    @Serializable
    data object Dictionary : QamusDestinations()

    /**
     * Settings screen - screen for managing app settings
     */
    @Serializable
    data object Settings : QamusDestinations()

    /**
     * Add/Edit Entry screen with entryId parameter
     */
    @Serializable
    data class AddEntry(val id: Int = -1) : QamusDestinations()

    /**
     * Kalima Details screen with entryId parameter
     */
    @Serializable
    data class KalimaDetails(val id: Int) : QamusDestinations()
}
