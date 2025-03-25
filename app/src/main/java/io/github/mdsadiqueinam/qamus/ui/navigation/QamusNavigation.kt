package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.mdsadiqueinam.qamus.ui.screen.AddEntryScreen
import io.github.mdsadiqueinam.qamus.ui.screen.DashboardScreen
import io.github.mdsadiqueinam.qamus.ui.screen.DictionaryScreen
import io.github.mdsadiqueinam.qamus.ui.screen.KalimaDetailsScreen
import io.github.mdsadiqueinam.qamus.ui.screen.SettingsScreen
import io.github.mdsadiqueinam.qamus.ui.viewmodel.AddEntryViewModel
import io.github.mdsadiqueinam.qamus.ui.viewmodel.DashboardViewModel
import io.github.mdsadiqueinam.qamus.ui.viewmodel.KalimaDetailsViewModel
import io.github.mdsadiqueinam.qamus.ui.viewmodel.KalimaatViewModel
import io.github.mdsadiqueinam.qamus.ui.viewmodel.NavHostViewModel
import io.github.mdsadiqueinam.qamus.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun QamusNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Get the NavHostViewModel to access the navigator
    val navHostViewModel = hiltViewModel<NavHostViewModel>()
    val navigator = navHostViewModel.navigator

    // Handle navigation events from the navigator
    LaunchedEffect(navigator, navController) {
        navigator.navigationActions.collectLatest { action ->
            navigator.handleNavigationAction(action, navController)
        }
    }
    NavHost(
        navController = navController,
        startDestination = QamusDestinations.Dashboard.route,
        modifier = modifier
    ) {
        composable(QamusDestinations.Dashboard.route) {
            val dashboardViewModel = hiltViewModel<DashboardViewModel>()

            DashboardScreen(
                viewModel = dashboardViewModel
            )
        }

        composable(QamusDestinations.Dictionary.route) {
            val kalimaatViewModel = hiltViewModel<KalimaatViewModel>()

            DictionaryScreen(
                viewModel = kalimaatViewModel
            )
        }

        composable(
            route = QamusDestinations.AddEntry.routeWithArgs,
            arguments = QamusDestinations.AddEntry.arguments
        ) {
            val addEntryViewModel = hiltViewModel<AddEntryViewModel>()

            AddEntryScreen(
                viewModel = addEntryViewModel
            )
        }

        composable(
            route = QamusDestinations.KalimaDetails.routeWithArgs,
            arguments = QamusDestinations.KalimaDetails.arguments
        ) {
            val kalimaDetailsViewModel = hiltViewModel<KalimaDetailsViewModel>()

            KalimaDetailsScreen(
                viewModel = kalimaDetailsViewModel
            )
        }

        composable(QamusDestinations.Settings.route) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()

            SettingsScreen(
                viewModel = settingsViewModel
            )
        }
    }
}
