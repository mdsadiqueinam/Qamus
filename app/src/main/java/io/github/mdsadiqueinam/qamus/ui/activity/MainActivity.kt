package io.github.mdsadiqueinam.qamus.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavHost
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

    QamusNavHost(
        navController = navController, modifier = Modifier
    )
}
