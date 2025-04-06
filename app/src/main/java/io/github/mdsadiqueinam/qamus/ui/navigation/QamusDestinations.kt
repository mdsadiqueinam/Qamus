package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class QamusDestinations(val route: String) {

    /**
     * Main navigation graph - contains screens without bottom bar
     */
    object Main : QamusDestinations("main")

    /**
     * Home navigation graph - contains screens with bottom bar
     */
    object Home : QamusDestinations("home")

    /**
     * Dashboard screen - main/home page of the app
     */
    object Dashboard : QamusDestinations("dashboard")

    /**
     * Dictionary screen - screen showing the list of entries
     */
    object Dictionary : QamusDestinations("dictionary")

    /**
     * Settings screen - screen for managing app settings
     */
    object Settings : QamusDestinations("settings")

    /**
     * Add/Edit Entry screen with entryId parameter
     */
    object AddEntry : QamusDestinations("add_entry") {
        const val entryIdArg = "entryId"
        val routeWithArgs = "$route/{$entryIdArg}"

        val arguments = listOf(
            navArgument(entryIdArg) {
                type = NavType.LongType
                defaultValue = -1L
            }
        )

        fun createRoute(entryId: Long = -1L) = "$route/$entryId"
    }

    /**
     * Kalima Details screen with entryId parameter
     */
    object KalimaDetails : QamusDestinations("kalima_details") {
        const val entryIdArg = "entryId"
        val routeWithArgs = "$route/{$entryIdArg}"

        val arguments = listOf(
            navArgument(entryIdArg) {
                type = NavType.LongType
            }
        )

        fun createRoute(entryId: Long) = "$route/$entryId"
    }
}
