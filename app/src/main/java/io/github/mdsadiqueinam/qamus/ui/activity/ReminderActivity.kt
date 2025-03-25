package io.github.mdsadiqueinam.qamus.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import io.github.mdsadiqueinam.qamus.data.model.Kalima
import io.github.mdsadiqueinam.qamus.ui.components.KalimaReminderContent
import io.github.mdsadiqueinam.qamus.ui.theme.QamusTheme
import io.github.mdsadiqueinam.qamus.ui.viewmodel.ReminderViewModel

/**
 * Activity for displaying Kalima reminders.
 * This activity can be shown when the device is locked or in use.
 */
@AndroidEntryPoint
class ReminderActivity : ComponentActivity() {

    private val viewModel: ReminderViewModel by viewModels()
    private lateinit var wakeLock: PowerManager.WakeLock

    companion object {
        private const val TAG = "ReminderActivity"
        private const val EXTRA_SHOW_WHEN_LOCKED = "show_when_locked"

        /**
         * Create an intent to start the ReminderActivity.
         *
         * @param context The context to create the intent from
         * @param showWhenLocked Whether to show the activity when the device is locked
         * @return An intent to start the ReminderActivity
         */
        fun createIntent(context: Context, showWhenLocked: Boolean = false): Intent {
            return Intent(context, ReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_SHOW_WHEN_LOCKED, showWhenLocked)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up wake lock
        val powerManager = getSystemService<PowerManager>()
        val showWhenLocked = intent.getBooleanExtra(EXTRA_SHOW_WHEN_LOCKED, false)

        // Configure window to show when locked and turn screen on
        if (showWhenLocked) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            // Acquire wake lock to keep the screen on
            wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Qamus:ReminderActivityWakeLock"
            )?.apply {
                if (!isHeld) {
                    acquire(5 * 60 * 1000L) // 5 minutes max
                }
            }!!
        }

        setContent {
            QamusTheme {
                val kalima by viewModel.kalima.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val error by viewModel.error.collectAsState()

                KalimaReminderContent(
                    kalima = kalima,
                    onClose = { finish() },
                    isLoading = isLoading,
                    error = error
                )
            }
        }
    }

    override fun onDestroy() {
        // Release wake lock if held
        if (::wakeLock.isInitialized && wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
    }
}
