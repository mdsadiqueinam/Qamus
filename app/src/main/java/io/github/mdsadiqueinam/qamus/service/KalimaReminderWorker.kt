package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.mdsadiqueinam.qamus.data.repository.KalimaatRepository
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Worker class for scheduling periodic reminders of random Kalima entries.
 * This worker is scheduled to run at intervals defined in the app settings.
 */
class KalimaReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val kalimaatRepository: KalimaatRepository,
    private val overlayManager: KalimaOverlayManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "KalimaReminderWorker: Starting work")

            // Get a random Kalima entry
            val randomKalima = kalimaatRepository.getRandomEntry()

            // If we found a Kalima, show it in the overlay
            if (randomKalima != null) {
                Log.d(TAG, "KalimaReminderWorker: Found random kalima: ${randomKalima.huroof}")
                overlayManager.showKalimaOverlay(randomKalima)
                return Result.success()
            }

            // If no Kalima was found, retry later
            Log.w(TAG, "KalimaReminderWorker: No kalima found, will retry")
            return Result.retry()
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
    class Factory @Inject constructor(
        private val kalimaatRepository: KalimaatRepository,
        private val overlayManager: KalimaOverlayManager
    ) {
        fun create(appContext: Context, params: WorkerParameters): KalimaReminderWorker {
            return KalimaReminderWorker(
                appContext,
                params,
                kalimaatRepository,
                overlayManager
            )
        }
    }
}
