package io.github.mdsadiqueinam.qamus.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.mdsadiqueinam.qamus.service.KalimaReminderScheduler
import io.github.mdsadiqueinam.qamus.ui.navigation.QamusNavHost
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var kalimaReminderScheduler: KalimaReminderScheduler

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, proceed with app functionality
        } else {
            // Permission denied, app will function without notifications
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule the Kalima reminder
        kalimaReminderScheduler.startScheduling()

        // Check and request notification permission
        checkNotificationPermission()

        setContent {
            QamusTheme {
                QamusApp()
            }
        }
    }

    /**
     * Checks if notification permission is granted and requests it if needed.
     * Only applicable for Android 13 (API 33) and above.
     */
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale if needed and then request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
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
