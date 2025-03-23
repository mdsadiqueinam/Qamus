package io.github.mdsadiqueinam.qamus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import io.github.mdsadiqueinam.qamus.ui.screen.AddEntryScreen
import io.github.mdsadiqueinam.qamus.ui.screen.DictionaryScreen
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QamusTheme {
                QamusApp()
            }
        }
    }
}

@Composable
fun QamusApp() {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        QamusNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

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
            val kalimaatViewModel = hiltViewModel<io.github.mdsadiqueinam.qamus.ui.viewmodel.KalimaatViewModel>()

            DictionaryScreen(
                viewModel = kalimaatViewModel,
                onAddEntry = { navController.navigate("add_entry/-1") },
                onEditEntry = { entryId -> navController.navigate("add_entry/$entryId") }
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
            val addEntryViewModel = hiltViewModel<io.github.mdsadiqueinam.qamus.ui.viewmodel.AddEntryViewModel>()

            AddEntryScreen(
                viewModel = addEntryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
