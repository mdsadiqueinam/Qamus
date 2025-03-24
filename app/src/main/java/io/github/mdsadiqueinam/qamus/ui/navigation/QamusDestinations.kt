package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class QamusDestinations(val route: String) {
    
    /**
     * Dictionary screen - main screen showing the list of entries
     */
    object Dictionary : QamusDestinations("dictionary")
    
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