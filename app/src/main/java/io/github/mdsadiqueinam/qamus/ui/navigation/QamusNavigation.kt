package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import io.github.mdsadiqueinam.qamus.R
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
        navController = navController, startDestination = QamusDestinations.Main, modifier = modifier
    ) {
        // Main navigation graph

        // Home screen with BottomAppBar
        composable<QamusDestinations.Main> {
            HomeScreen(parentNavController = navController)
        }

        // Screens without BottomAppBar
        composable<QamusDestinations.AddEntry> {
            val addEntryViewModel = hiltViewModel<AddEntryViewModel>()
            AddEntryScreen(viewModel = addEntryViewModel)
        }

        composable<QamusDestinations.KalimaDetails> {
            val kalimaDetailsViewModel = hiltViewModel<KalimaDetailsViewModel>()
            KalimaDetailsScreen(viewModel = kalimaDetailsViewModel)
        }

    }
}

@Composable
fun HomeScreen(parentNavController: NavHostController) {

    // Create a new NavController for the nested navigation
    // This ensures that the ViewModelStore is properly set
    val navController = rememberNavController()

    // Set the ViewModelStore from the parent NavController's BackStackEntry
    val parentEntry = remember { parentNavController.getBackStackEntry(QamusDestinations.Main) }
    navController.setViewModelStore(parentEntry.viewModelStore)

    // Get current back stack entry to determine current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentHierarchy = navBackStackEntry?.destination?.hierarchy
    val routes = listOf(
        TopLevelRoute(
            label = R.string.home, icon = Icons.Filled.Home, route = QamusDestinations.Dashboard
        ), TopLevelRoute(
            label = R.string.dictionary, icon = Icons.Filled.Search, route = QamusDestinations.Dictionary
        ), TopLevelRoute(
            label = R.string.settings, icon = Icons.Filled.Settings, route = QamusDestinations.Settings
        )
    )

    Scaffold(
        bottomBar = {
            BottomAppBar(actions = {
                // Home/Dashboard navigation item
                routes.forEach { item ->
                    NavigationBarItem(icon = {
                        Icon(item.icon, contentDescription = stringResource(item.label))
                    }, selected = currentHierarchy?.any { it.hasRoute(item.route::class) } == true, onClick = {
                        navController.navigate(item.route) {
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
                }
            }, floatingActionButton = {
                // FAB to open AddEntryScreen
                FloatingActionButton(
                    onClick = {
                        parentNavController.navigate(QamusDestinations.AddEntry())
                    }, elevation = FloatingActionButtonDefaults.elevation()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Entry")
                }
            })
        }) { innerPadding ->
        // Content area with padding for the bottom bar
        NavHost(
            navController = navController,
            startDestination = QamusDestinations.Dashboard,
        ) {
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
