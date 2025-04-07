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
    data class NavigateTo(val destination: QamusDestinations) : NavigationAction()
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
    suspend fun navigate(destination: QamusDestinations) {
        _navigationActions.emit(
            NavigationAction.NavigateTo(destination)
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
    fun handleNavigationAction(action: NavigationAction, navController: NavController) {
        when (action) {
            is NavigationAction.NavigateBack -> navController.popBackStack()
            is NavigationAction.NavigateTo -> navController.navigate(action.destination)
        }
    }
}
