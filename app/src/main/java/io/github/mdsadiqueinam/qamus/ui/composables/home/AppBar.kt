package io.github.mdsadiqueinam.qamus.ui.composables.home

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusDestinations
import io.github.mdsadiqueinam.qamus.ui.navigation.Route

@Composable
fun HomeAppBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentHierarchy = navBackStackEntry?.destination?.hierarchy
    val homeRoutes = listOf(
        Route(
            label = R.string.home, icon = Icons.Filled.Home, route = QamusDestinations.Dashboard
        ), Route(
            label = R.string.dictionary, icon = Icons.Filled.Search, route = QamusDestinations.Dictionary
        ), Route(
            label = R.string.settings, icon = Icons.Filled.Settings, route = QamusDestinations.Settings
        )
    )

    val isHomeRoute = homeRoutes.any { currentHierarchy?.any { hierarchy -> hierarchy.hasRoute(it.route::class) } == true }

    if (isHomeRoute) {
        BottomAppBar(actions = {
            // Home/Dashboard navigation item
            homeRoutes.forEach { item ->
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
                    navController.navigate(QamusDestinations.AddEntry())
                }, elevation = FloatingActionButtonDefaults.elevation()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Entry")
            }
        })
    }
}