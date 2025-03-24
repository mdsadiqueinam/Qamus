package io.github.mdsadiqueinam.qamus.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
        startDestination = "dictionary",
        modifier = modifier
    ) {
        composable("dictionary") {
            val kalimaatViewModel = hiltViewModel<KalimaatViewModel>()

            DictionaryScreen(
                viewModel = kalimaatViewModel,
                onAddEntry = { navController.navigate("add_entry/-1") },
                onEditEntry = { entryId -> navController.navigate("add_entry/$entryId") },
                onViewDetails = { entryId -> navController.navigate("kalima_details/$entryId") }
            )
        }

        composable(
            route = "add_entry/{entryId}",
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            val addEntryViewModel = hiltViewModel<AddEntryViewModel>()

            AddEntryScreen(
                viewModel = addEntryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "kalima_details/{entryId}",
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.LongType
                }
            )
        ) {
            val kalimaDetailsViewModel = hiltViewModel<KalimaDetailsViewModel>()

            KalimaDetailsScreen(
                viewModel = kalimaDetailsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditEntry = { entryId -> navController.navigate("add_entry/$entryId") },
                onViewDetails = { entryId -> navController.navigate("kalima_details/$entryId") }
            )
        }
    }
}