package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation actions that can be performed in the app
 */
sealed class NavigationAction {
    object NavigateBack : NavigationAction()
    data class NavigateTo(val destination: QamusDestinations, val route: String) : NavigationAction()
}

/**
 * Navigator class that handles navigation between screens
 * This class is injected into ViewModels to allow them to navigate between screens
 */
@Singleton
class QamusNavigator @Inject constructor() {

    private val _navigationActions = MutableSharedFlow<NavigationAction>()
    val navigationActions: SharedFlow<NavigationAction> = _navigationActions.asSharedFlow()

    /**
     * Navigate to the dictionary screen
     */
    suspend fun navigateToDictionary() {
        _navigationActions.emit(
            NavigationAction.NavigateTo(
                QamusDestinations.Dictionary,
                QamusDestinations.Dictionary.route
            )
        )
    }

    /**
     * Navigate to the add entry screen
     * @param entryId The ID of the entry to edit, or -1 to add a new entry
     */
    suspend fun navigateToAddEntry(entryId: Long = -1L) {
        _navigationActions.emit(
            NavigationAction.NavigateTo(
                QamusDestinations.AddEntry,
                QamusDestinations.AddEntry.createRoute(entryId)
            )
        )
    }

    /**
     * Navigate to the kalima details screen
     * @param entryId The ID of the entry to view
     */
    suspend fun navigateToKalimaDetails(entryId: Long) {
        _navigationActions.emit(
            NavigationAction.NavigateTo(
                QamusDestinations.KalimaDetails,
                QamusDestinations.KalimaDetails.createRoute(entryId)
            )
        )
    }

    /**
     * Navigate back to the previous screen
     */
    suspend fun navigateBack() {
        _navigationActions.emit(NavigationAction.NavigateBack)
    }

    /**
     * Handle navigation actions by updating the NavController
     * This function should be called from a composable that has access to the NavController
     * @param navController The NavController to use for navigation
     * @param actions The flow of navigation actions to handle
     */
    suspend fun handleNavigationAction(action: NavigationAction, navController: NavController) {
        when (action) {
            is NavigationAction.NavigateBack -> navController.popBackStack()
            is NavigationAction.NavigateTo -> navController.navigate(action.route)
        }
    }
}
