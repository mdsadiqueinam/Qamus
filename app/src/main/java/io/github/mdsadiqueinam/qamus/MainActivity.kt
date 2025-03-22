package io.github.mdsadiqueinam.qamus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mdsadiqueinam.qamus.data.database.QamusDatabase
import io.github.mdsadiqueinam.qamus.data.repository.DictionaryRepository
import io.github.mdsadiqueinam.qamus.ui.screen.AddEntryScreen
import io.github.mdsadiqueinam.qamus.ui.screen.DictionaryScreen
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme
import io.github.mdsadiqueinam.qamus.ui.viewmodel.DictionaryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize the database
        val database = QamusDatabase.getDatabase(this)
        val repository = DictionaryRepository(database.dictionaryDao())
        val viewModelFactory = DictionaryViewModel.Factory(repository)

        setContent {
            QamusTheme {
                QamusApp(viewModelFactory)
            }
        }
    }
}

@Composable
fun QamusApp(viewModelFactory: DictionaryViewModel.Factory) {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        QamusNavHost(
            navController = navController,
            viewModelFactory = viewModelFactory,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun QamusNavHost(
    navController: NavHostController,
    viewModelFactory: DictionaryViewModel.Factory,
    modifier: Modifier = Modifier
) {
    val dictionaryViewModel: DictionaryViewModel = viewModel(factory = viewModelFactory)

    NavHost(
        navController = navController,
        startDestination = "dictionary",
        modifier = modifier
    ) {
        composable("dictionary") {
            DictionaryScreen(
                viewModel = dictionaryViewModel,
                onAddEntry = { navController.navigate("add_entry") }
            )
        }

        composable("add_entry") {
            AddEntryScreen(
                viewModel = dictionaryViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
