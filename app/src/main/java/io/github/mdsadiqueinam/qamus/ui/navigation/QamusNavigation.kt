package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
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

    NavHost(
        navController = navController, startDestination = QamusDestinations.Main.route, modifier = modifier
    ) {
        // Main navigation graph

        // Home screen with BottomAppBar
        composable(route = QamusDestinations.Main.route) {
            HomeScreen(navController = navController)
        }

        // Screens without BottomAppBar
        composable(
            route = QamusDestinations.AddEntry.routeWithArgs, arguments = QamusDestinations.AddEntry.arguments
        ) {
            val addEntryViewModel = hiltViewModel<AddEntryViewModel>()
            AddEntryScreen(viewModel = addEntryViewModel)
        }

        composable(
            route = QamusDestinations.KalimaDetails.routeWithArgs, arguments = QamusDestinations.KalimaDetails.arguments
        ) {
            val kalimaDetailsViewModel = hiltViewModel<KalimaDetailsViewModel>()
            KalimaDetailsScreen(viewModel = kalimaDetailsViewModel)
        }

    }
}

@Composable
fun NavGraphBuilder.HomeScreen(navController: NavHostController) {

    // Get current back stack entry to determine current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            BottomAppBar(actions = {
                // Home/Dashboard navigation item
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    selected = currentRoute == QamusDestinations.Dashboard.route,
                    onClick = {
                        navController.navigate(QamusDestinations.Dashboard.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    })

                // Dictionary/Search navigation item
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Search, contentDescription = "Dictionary") },
                    selected = currentRoute == QamusDestinations.Dictionary.route,
                    onClick = {
                        navController.navigate(QamusDestinations.Dictionary.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })

                // Settings navigation item
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    selected = currentRoute == QamusDestinations.Settings.route,
                    onClick = {
                        navController.navigate(QamusDestinations.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }, floatingActionButton = {
                // FAB to open AddEntryScreen
                FloatingActionButton(
                    onClick = {
                        navController.navigate(QamusDestinations.AddEntry.createRoute())
                    }, elevation = FloatingActionButtonDefaults.elevation()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Entry")
                }
            })
        }) { innerPadding ->
        // Content area with padding for the bottom bar
        navigation (
            startDestination = QamusDestinations.Dashboard.route,
            route = QamusDestinations.Home.route,
        ) {
            composable(QamusDestinations.Dashboard.route) {
                val dashboardViewModel = hiltViewModel<DashboardViewModel>()
                DashboardScreen(viewModel = dashboardViewModel)
            }

            composable(QamusDestinations.Dictionary.route) {
                val kalimaatViewModel = hiltViewModel<KalimaatViewModel>()
                DictionaryScreen(viewModel = kalimaatViewModel)
            }

            composable(QamusDestinations.Settings.route) {
                val settingsViewModel = hiltViewModel<SettingsViewModel>()
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
