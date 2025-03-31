package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.receiver.ScreenStateReceiver
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
            Log.d(TAG, appContext.getString(R.string.log_kalima_reminder_starting))

            // Send a broadcast to the ScreenStateReceiver which will handle showing
            // either the ReminderActivity or a notification based on device state
            val intent = Intent(appContext, ScreenStateReceiver::class.java).apply {
                action = ScreenStateReceiver.ACTION_SHOW_REMINDER
            }
            appContext.sendBroadcast(intent)

            return Result.success()
        } catch (e: Exception) {
            // Log the error and retry
            Log.w(TAG, appContext.getString(R.string.log_kalima_reminder_error), e)
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
