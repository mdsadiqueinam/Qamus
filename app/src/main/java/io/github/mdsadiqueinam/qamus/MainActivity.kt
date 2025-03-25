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
import io.github.mdsadiqueinam.qamus.util.PermissionUtils

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check if permission was granted
        if (PermissionUtils.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Permission granted",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Permission denied. Some features may not work properly.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for required permissions
        checkAndRequestPermissions()

        setContent {
            QamusTheme {
                QamusApp()
            }
        }
    }

    /**
     * Checks if all required permissions are granted and requests them if not.
     */
    private fun checkAndRequestPermissions() {
        if (!PermissionUtils.areAllPermissionsGranted(this)) {
            // Request SYSTEM_ALERT_WINDOW permission
            if (!PermissionUtils.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "This app needs permission to display over other apps",
                    Toast.LENGTH_LONG
                ).show()

                // Create the intent to request overlay permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )

                // Launch the intent using the ActivityResultLauncher
                overlayPermissionLauncher.launch(intent)
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
