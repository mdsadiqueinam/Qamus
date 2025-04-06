package io.github.mdsadiqueinam.qamus.worker

import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.mdsadiqueinam.qamus.data.model.Settings
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Error in backup scheduler", throwable)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    companion object {
        private const val TAG = "AutomaticBackupScheduler"
        private const val WORK_NAME = "automatic_backup_worker"

        // Intervals in hours
        private val FREQUENCY_INTERVALS = mapOf(
            Settings.AutomaticBackupFrequency.DAILY to 24L,
            Settings.AutomaticBackupFrequency.WEEKLY to 7 * 24L,
            Settings.AutomaticBackupFrequency.MONTHLY to 30 * 24L
        )
    }

    /**
     * Start monitoring settings changes and schedule the worker accordingly.
     */
    fun startScheduling() {
        scope.launch {
            settingsRepository.settings
                .map { it.automaticBackupFrequency } // Only extract the frequency
                .distinctUntilChanged() // Only react to changes in frequency
                .catch { e -> Log.e(TAG, "Error collecting settings", e) }
                .collect { frequency ->
                    try {
                        if (frequency == Settings.AutomaticBackupFrequency.OFF) {
                            Log.d(TAG, "Automatic backup disabled")
                            stopScheduling()
                        } else {
                            val intervalHours = FREQUENCY_INTERVALS[frequency] ?: return@collect
                            Log.d(TAG, "Scheduling backup: $frequency (every $intervalHours hours)")
                            scheduleWorker(intervalHours)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling backup frequency change", e)
                    }
                }
        }
    }

    /**
     * Schedule the worker to run at the specified interval.
     */
    private fun scheduleWorker(intervalHours: Long) {
        try {
            val workerInfo = workManager.getWorkInfosForUniqueWork(WORK_NAME).get() ?: emptyList()

            // Check if the worker is already scheduled with the same interval
            val intervalMillis = TimeUnit.HOURS.toMillis(intervalHours)
            if (workerInfo.any { it.periodicityInfo?.repeatIntervalMillis == intervalMillis &&
                        it.state == androidx.work.WorkInfo.State.ENQUEUED }) {
                Log.d(TAG, "Worker already scheduled with interval: $intervalHours hours, ignoring")
                return
            }

            Log.d(TAG, "Scheduling backup worker with interval: $intervalHours hours")

            // Create the work request with constraints
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

            Log.d(TAG, "Automatic backup worker scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling backup worker", e)
        }
    }

    /**
     * Stop scheduling the worker.
     */
    fun stopScheduling() {
        try {
            workManager.cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Backup worker scheduling stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping backup worker scheduling", e)
        }
    }
}