package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.mdsadiqueinam.qamus.R
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
    data class AddEntry(val id: Long = -1) : QamusDestinations()

    /**
     * Kalima Details screen with entryId parameter
     */
    @Serializable
    data class KalimaDetails(val id: Long) : QamusDestinations()
}

data class TopLevelRoute<T : Any>(@StringRes val label: Int, val route: T, val icon: ImageVector)