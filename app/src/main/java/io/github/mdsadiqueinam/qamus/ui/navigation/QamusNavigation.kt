package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusDestinations
import io.github.mdsadiqueinam.qamus.ui.screen.AddEntryScreen
import io.github.mdsadiqueinam.qamus.ui.screen.DictionaryScreen
import io.github.mdsadiqueinam.qamus.ui.screen.KalimaDetailsScreen
import io.github.mdsadiqueinam.qamus.ui.viewmodel.AddEntryViewModel
import io.github.mdsadiqueinam.qamus.ui.viewmodel.KalimaDetailsViewModel
import io.github.mdsadiqueinam.qamus.ui.viewmodel.KalimaatViewModel

@Composable
fun QamusNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = QamusDestinations.Dictionary.route,
        modifier = modifier
    ) {
        composable(QamusDestinations.Dictionary.route) {
            val kalimaatViewModel = hiltViewModel<KalimaatViewModel>()

            DictionaryScreen(
                viewModel = kalimaatViewModel,
                onAddEntry = { navController.navigate(QamusDestinations.AddEntry.createRoute()) },
                onEditEntry = { entryId -> navController.navigate(QamusDestinations.AddEntry.createRoute(entryId)) },
                onViewDetails = { entryId -> navController.navigate(QamusDestinations.KalimaDetails.createRoute(entryId)) }
            )
        }

        composable(
            route = QamusDestinations.AddEntry.routeWithArgs,
            arguments = QamusDestinations.AddEntry.arguments
        ) {
            val addEntryViewModel = hiltViewModel<AddEntryViewModel>()

            AddEntryScreen(
                viewModel = addEntryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = QamusDestinations.KalimaDetails.routeWithArgs,
            arguments = QamusDestinations.KalimaDetails.arguments
        ) {
            val kalimaDetailsViewModel = hiltViewModel<KalimaDetailsViewModel>()

            KalimaDetailsScreen(
                viewModel = kalimaDetailsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditEntry = { entryId -> navController.navigate(QamusDestinations.AddEntry.createRoute(entryId)) },
                onViewDetails = { entryId -> navController.navigate(QamusDestinations.KalimaDetails.createRoute(entryId)) }
            )
        }
    }
}