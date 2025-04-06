package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
fun QamusBottomNavBar() {
    val navController = rememberNavController()
    val navHostViewModel = hiltViewModel<NavHostViewModel>()
    val navigator = navHostViewModel.navigator

    // Get current back stack entry to determine current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle navigation events from the navigator
    LaunchedEffect(navigator, navController) {
        navigator.navigationActions.collectLatest { action ->
            navigator.handleNavigationAction(action, navController)
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
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
                        }
                    )

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
                        }
                    )

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
                        }
                    )
                },
                floatingActionButton = {
                    // FAB to open AddEntryScreen
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(QamusDestinations.AddEntry.createRoute())
                        },
                        elevation = FloatingActionButtonDefaults.elevation()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Entry")
                    }
                }
            )
        }
    ) { innerPadding ->
        // NavHost inside the Scaffold
        NavHost(
            navController = navController,
            startDestination = QamusDestinations.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
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

            composable(
                route = QamusDestinations.AddEntry.routeWithArgs,
                arguments = QamusDestinations.AddEntry.arguments
            ) {
                val addEntryViewModel = hiltViewModel<AddEntryViewModel>()
                AddEntryScreen(viewModel = addEntryViewModel)
            }

            composable(
                route = QamusDestinations.KalimaDetails.routeWithArgs,
                arguments = QamusDestinations.KalimaDetails.arguments
            ) {
                val kalimaDetailsViewModel = hiltViewModel<KalimaDetailsViewModel>()
                KalimaDetailsScreen(viewModel = kalimaDetailsViewModel)
            }
        }
    }
}

@Composable
fun QamusNavHost(
    navController: NavHostController = rememberNavController(),
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
