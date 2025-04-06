package io.github.mdsadiqueinam.qamus.service

import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
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
 * Scheduler for the Kalima reminder worker.
 * This class is responsible for scheduling the worker to run at the interval
 * specified in the app settings.
 */
@Singleton
class KalimaReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val settingsRepository: SettingsRepository,
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Error in reminder scheduler", throwable)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)

    companion object {
        private const val TAG = "KalimaReminderScheduler"
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
            settingsRepository.settings
                .map { it.isReminderEnabled to it.reminderInterval }
                .distinctUntilChanged()
                .catch { e -> Log.e(TAG, "Error collecting settings", e) }
                .collect { (isEnabled, interval) ->
                    if (isEnabled) {
                        Log.d(TAG, "Settings updated, reminderInterval: $interval min")
                        scheduleWorker(interval)
                    } else {
                        Log.d(TAG, "Reminder is disabled, stopping scheduling")
                        stopScheduling()
                    }
                }
        }
    }

    /**
     * Schedule the worker to run at the specified interval.
     */
    private fun scheduleWorker(interval: Int) {
        try {
            val workerInfo = workManager.getWorkInfosForUniqueWork(WORK_NAME).get() ?: emptyList()

            // Ensure the interval is at least 15 minutes
            val repeatInterval = interval.toLong().coerceIn(MIN_PERIODIC_INTERVAL, MAX_PERIODIC_INTERVAL)
            val repeatIntervalMillis = TimeUnit.MINUTES.toMillis(repeatInterval)

            // Check if the worker is already scheduled with the same interval
            if (workerInfo.any { it.periodicityInfo?.repeatIntervalMillis == repeatIntervalMillis && it.state == androidx.work.WorkInfo.State.ENQUEUED }) {
                Log.d(TAG, "Worker already scheduled with interval: $repeatInterval minutes, ignoring")
                return
            }

            Log.d(TAG, "Scheduling worker with interval: $repeatInterval minutes")

            // Create the work request with constraints
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
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling worker", e)
        }
    }

    /**
     * Stop scheduling the worker.
     */
    fun stopScheduling() {
        try {
            workManager.cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Worker scheduling stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping worker scheduling", e)
        }
    }
}
