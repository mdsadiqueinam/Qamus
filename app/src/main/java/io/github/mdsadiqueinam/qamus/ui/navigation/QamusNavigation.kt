package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mdsadiqueinam.qamus.ui.composables.home.HomeAppBar
import io.github.mdsadiqueinam.qamus.ui.screen.*
import io.github.mdsadiqueinam.qamus.ui.viewmodel.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun QamusNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navHostViewModel = hiltViewModel<NavHostViewModel>()
    val navigator = navHostViewModel.navigator

    // Handle navigation events from the navigator
    LaunchedEffect(navigator, navController) {
        navigator.navigationActions.collectLatest { action ->
            navigator.handleNavigationAction(action, navController)
        }
    }


    Scaffold(
        bottomBar = {
            HomeAppBar(navController)
        }) { innerPadding ->
        // Content area with padding for the bottom bar
        NavHost(
            navController = navController, startDestination = QamusDestinations.Dashboard, modifier = modifier
        ) {

            // Screens without BottomAppBar
            composable<QamusDestinations.AddEntry> {
                val addEntryViewModel = hiltViewModel<AddEntryViewModel>()
                AddEntryScreen(viewModel = addEntryViewModel)
            }

            composable<QamusDestinations.KalimaDetails> {
                val kalimaDetailsViewModel = hiltViewModel<KalimaDetailsViewModel>()
                KalimaDetailsScreen(viewModel = kalimaDetailsViewModel)
            }

            composable<QamusDestinations.Dashboard> {
                val dashboardViewModel = hiltViewModel<DashboardViewModel>()
                DashboardScreen(viewModel = dashboardViewModel)
            }

            composable<QamusDestinations.Dictionary> {
                val kalimaatViewModel = hiltViewModel<KalimaatViewModel>()
                DictionaryScreen(viewModel = kalimaatViewModel)
            }

            composable<QamusDestinations.Settings> {
                val settingsViewModel = hiltViewModel<SettingsViewModel>()
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}

