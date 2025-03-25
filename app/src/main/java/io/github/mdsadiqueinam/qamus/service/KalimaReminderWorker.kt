package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Worker class for scheduling periodic reminders of random Kalima entries.
 * This worker is scheduled to run at intervals defined in the app settings.
 */
class KalimaReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "KalimaReminderWorker: Starting work")

            // Start the KalimaOverlayService which will get a random Kalima
            val intent = KalimaOverlayService.createIntent(appContext)
            appContext.startService(intent)

            return Result.success()
        } catch (e: Exception) {
            // Log the error and retry
            Log.e(TAG, "KalimaReminderWorker: Error during work", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "KalimaReminder"
    }

    /**
     * Factory class for creating KalimaReminderWorker instances with dependencies.
     */
    class Factory @Inject constructor() {
        fun create(appContext: Context, params: WorkerParameters): KalimaReminderWorker {
            return KalimaReminderWorker(
                appContext,
                params
            )
        }
    }
}
