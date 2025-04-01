package io.github.mdsadiqueinam.qamus.service

import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scheduler for the automatic backup worker.
 * This class is responsible for scheduling the worker to run at the interval
 * specified in the app settings for automatic backup frequency.
 */
@Singleton
class AutomaticBackupScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "AutomaticBackupScheduler"
        private const val WORK_NAME = "automatic_backup_worker"
        
        // Intervals in hours
        private const val DAILY_INTERVAL = 24L
        private const val WEEKLY_INTERVAL = 7 * 24L
        private const val MONTHLY_INTERVAL = 30 * 24L
    }

    /**
     * Start monitoring settings changes and schedule the worker accordingly.
     */
    fun startScheduling() {
        Log.d(TAG, "Starting automatic backup scheduling")
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                when (settings.automaticBackupFrequency) {
                    Settings.AutomaticBackupFrequency.OFF -> {
                        Log.d(TAG, "Automatic backup is disabled, stopping scheduling")
                        stopScheduling()
                    }
                    else -> {
                        Log.d(TAG, "Settings updated, automaticBackupFrequency: ${settings.automaticBackupFrequency}")
                        scheduleWorker(settings.automaticBackupFrequency)
                    }
                }
            }
        }
    }

    /**
     * Schedule the worker to run at the specified interval based on frequency.
     */
    private fun scheduleWorker(frequency: Settings.AutomaticBackupFrequency) {
        // Determine the interval based on frequency
        val intervalHours = when (frequency) {
            Settings.AutomaticBackupFrequency.DAILY -> DAILY_INTERVAL
            Settings.AutomaticBackupFrequency.WEEKLY -> WEEKLY_INTERVAL
            Settings.AutomaticBackupFrequency.MONTHLY -> MONTHLY_INTERVAL
            Settings.AutomaticBackupFrequency.OFF -> {
                // If frequency is OFF, don't schedule
                stopScheduling()
                return
            }
        }

        Log.d(TAG, "Scheduling worker with frequency: $frequency (every $intervalHours hours)")

        // Create the work request
        val workRequest = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(
            intervalHours,
            TimeUnit.HOURS
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