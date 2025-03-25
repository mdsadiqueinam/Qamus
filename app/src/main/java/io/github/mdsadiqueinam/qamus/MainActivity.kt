package io.github.mdsadiqueinam.qamus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
