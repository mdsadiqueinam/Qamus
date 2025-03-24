package io.github.mdsadiqueinam.qamus.service

import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Scheduler for the Kalima reminder worker.
 * This class is responsible for scheduling the worker to run at the interval
 * specified in the app settings.
 */
@Singleton
class KalimaReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val settingsRepositoryProvider: Provider<SettingsRepository>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "KalimaReminder"
        private const val WORK_NAME = "kalima_reminder_worker"
    }

    /**
     * Start monitoring settings changes and schedule the worker accordingly.
     */
    fun startScheduling() {
        Log.d(TAG, "Starting Kalima reminder scheduling")
        scope.launch {
            settingsRepositoryProvider.get().settings.collectLatest { settings ->
                Log.d(TAG, "Settings updated, reminderInterval: ${settings.reminderInterval}ms")
                scheduleWorker(settings.reminderInterval)
            }
        }
    }

    /**
     * Schedule the worker to run at the specified interval.
     */
    private fun scheduleWorker(interval: Int) {
        Log.d(TAG, "Scheduling worker with interval: $interval minutes")

        // Create the work request
        val workRequest = PeriodicWorkRequestBuilder<KalimaReminderWorker>(
            interval.toLong(),
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
