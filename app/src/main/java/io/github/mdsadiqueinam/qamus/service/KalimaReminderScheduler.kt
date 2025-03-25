package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import io.github.mdsadiqueinam.qamus.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for the Kalima reminder worker.
 * This class is responsible for scheduling the worker to run at the interval
 * specified in the app settings.
 */
@Singleton
class KalimaReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "KalimaReminder"
        private const val WORK_NAME = "kalima_reminder_worker"
        private const val MIN_PERIODIC_INTERVAL = 15L
        private const val MAX_PERIODIC_INTERVAL = 180L
    }

    /**
     * Start monitoring settings changes and schedule the worker accordingly.
     */
    fun startScheduling() {
        Log.d(TAG, "Starting Kalima reminder scheduling")
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                if (settings.isReminderEnabled) {
                    Log.d(TAG, "Settings updated, reminderInterval: ${settings.reminderInterval}min")
                    scheduleWorker(settings.reminderInterval)
                } else {
                    stopScheduling()
                }
            }
        }
    }

    /**
     * Schedule the worker to run at the specified interval.
     * This will only schedule the worker if the SYSTEM_ALERT_WINDOW permission is granted.
     */
    private fun scheduleWorker(interval: Int) {
        Log.d(TAG, "Scheduling worker with interval: $interval minutes")

        // Check if we have the permission to draw overlays
        if (!PermissionUtils.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot schedule Kalima reminder worker: SYSTEM_ALERT_WINDOW permission not granted")
            return
        }

        // Ensure the interval is at least 15 minutes
        val repeatInterval = interval.toLong().coerceIn(MIN_PERIODIC_INTERVAL, MAX_PERIODIC_INTERVAL)

        // Create the work request
        val workRequest = PeriodicWorkRequestBuilder<KalimaReminderWorker>(
            repeatInterval,
            TimeUnit.MINUTES
        ).build()

        // Schedule the work
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        Log.d(TAG, "Worker scheduled successfully")
    }

    /**
     * Stop scheduling the worker.
     */
    fun stopScheduling() {
        workManager.cancelUniqueWork(WORK_NAME)
    }
}
