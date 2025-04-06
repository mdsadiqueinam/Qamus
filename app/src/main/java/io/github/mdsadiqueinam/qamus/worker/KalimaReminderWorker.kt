package io.github.mdsadiqueinam.qamus.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.receiver.ScreenStateReceiver
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Worker class for scheduling periodic reminders of random Kalima entries.
 * This worker is scheduled to run at intervals defined in the app settings.
 */
class KalimaReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "KalimaReminder"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    override suspend fun doWork(): Result {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < MAX_RETRIES) {
            try {
                Log.d(TAG, appContext.getString(R.string.log_kalima_reminder_starting))

                // Send a broadcast to the ScreenStateReceiver which will handle showing
                // either the ReminderActivity or a notification based on device state
                val intent = Intent(appContext, ScreenStateReceiver::class.java).apply {
                    action = ScreenStateReceiver.ACTION_SHOW_REMINDER
                }
                appContext.sendBroadcast(intent)

                return Result.success(workDataOf("status" to "success"))

            } catch (e: Exception) {
                lastException = e
                retryCount++
                
                // Log the error
                Log.w(TAG, appContext.getString(R.string.log_kalima_reminder_error), e)
                
                // If we haven't reached max retries, wait before retrying
                if (retryCount < MAX_RETRIES) {
                    delay(RETRY_DELAY_MS * retryCount) // Exponential backoff
                }
            }
        }

        // If we've exhausted all retries, return failure
        return Result.failure(workDataOf(
            "error" to (lastException?.message ?: "Max retries exceeded")
        ))
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
