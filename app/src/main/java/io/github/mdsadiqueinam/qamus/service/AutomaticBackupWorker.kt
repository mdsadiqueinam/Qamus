package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.mdsadiqueinam.qamus.data.repository.BackupRestoreRepository
import io.github.mdsadiqueinam.qamus.data.repository.DataTransferState
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * Worker class for performing automatic backups.
 * This worker is scheduled to run at intervals defined by the automatic backup frequency setting.
 */
class AutomaticBackupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    private val backupRestoreRepository: BackupRestoreRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AutomaticBackup"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    override suspend fun doWork(): Result {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < MAX_RETRIES) {
            try {
                Log.d(TAG, "Starting automatic backup worker")

                // Get current settings
                val currentSettings = settingsRepository.settings.first()

                // Check if we can perform backup based on network connectivity
                if (!settingsRepository.canPerformAutomaticBackup()) {
                    Log.d(TAG, "Cannot perform automatic backup due to network constraints")
                    return Result.retry()
                }

                // Perform backup with progress tracking
                var progress = 0
                var bytesTransferred = 0L
                
                val backupResult = backupRestoreRepository.backupDatabase().first { state ->
                    when (state) {
                        is DataTransferState.Uploading -> {
                            progress = state.progress
                            bytesTransferred = state.bytes
                            setProgress(workDataOf(
                                "progress" to progress,
                                "bytes_transferred" to bytesTransferred
                            ))
                            false
                        }
                        is DataTransferState.Success -> true
                        is DataTransferState.Error -> true
                    }
                }

                return when (backupResult) {
                    is DataTransferState.Success -> {
                        // Update last backup timestamp and version
                        settingsRepository.updateLastBackup(
                            Clock.System.now(),
                            currentSettings.lastBackupVersion + 1
                        )
                        Log.d(TAG, "Automatic backup completed successfully")
                        Result.success(workDataOf(
                            "status" to "success",
                            "progress" to 100,
                            "bytes_transferred" to bytesTransferred
                        ))
                    }
                    is DataTransferState.Error -> {
                        lastException = backupResult.exception
                        Log.w(TAG, "Error during automatic backup: ${backupResult.message}", backupResult.exception)
                        if (retryCount < MAX_RETRIES - 1) {
                            delay(RETRY_DELAY_MS * (retryCount + 1)) // Exponential backoff
                            retryCount++
                            continue
                        }
                        Result.failure(workDataOf(
                            "error" to (backupResult.message ?: "Unknown error"),
                            "progress" to progress,
                            "bytes_transferred" to bytesTransferred
                        ))
                    }
                    else -> {
                        Log.w(TAG, "Unexpected state during automatic backup")
                        Result.retry()
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Error during automatic backup", e)
                if (retryCount < MAX_RETRIES - 1) {
                    delay(RETRY_DELAY_MS * (retryCount + 1)) // Exponential backoff
                    retryCount++
                    continue
                }
                return Result.failure(workDataOf(
                    "error" to (e.message ?: "Unknown error")
                ))
            }
        }

        return Result.failure(workDataOf(
            "error" to (lastException?.message ?: "Max retries exceeded")
        ))
    }

    /**
     * Factory class for creating AutomaticBackupWorker instances with dependencies.
     */
    class Factory @Inject constructor(
        private val backupRestoreRepository: BackupRestoreRepository,
        private val settingsRepository: SettingsRepository
    ) {
        fun create(appContext: Context, params: WorkerParameters): AutomaticBackupWorker {
            return AutomaticBackupWorker(
                appContext,
                params,
                backupRestoreRepository,
                settingsRepository
            )
        }
    }
}
