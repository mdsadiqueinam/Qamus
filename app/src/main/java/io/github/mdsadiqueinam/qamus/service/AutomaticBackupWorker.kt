package io.github.mdsadiqueinam.qamus.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.mdsadiqueinam.qamus.data.repository.BackupRestoreRepository
import io.github.mdsadiqueinam.qamus.data.repository.DataTransferState
import io.github.mdsadiqueinam.qamus.data.repository.SettingsRepository
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

    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "Starting automatic backup worker")

            // Get current settings
            val currentSettings = settingsRepository.settings.first()

            // Check if we can perform backup based on network connectivity
            if (!settingsRepository.canPerformAutomaticBackup()) {
                Log.d(TAG, "Cannot perform automatic backup due to network constraints")
                return Result.retry()
            }

            // Perform backup
            Log.d(TAG, "Performing automatic backup")
            val backupResult = backupRestoreRepository.backupDatabase().first { state ->
                state is DataTransferState.Success || state is DataTransferState.Error
            }

            return when (backupResult) {
                is DataTransferState.Success -> {
                    // Update last backup timestamp and version
                    settingsRepository.updateLastBackup(
                        Clock.System.now(),
                        currentSettings.lastBackupVersion + 1
                    )
                    Log.d(TAG, "Automatic backup completed successfully")
                    Result.success()
                }
                is DataTransferState.Error -> {
                    Log.w(TAG, "Error during automatic backup: ${backupResult.message}", backupResult.exception)
                    Result.retry()
                }
                else -> {
                    Log.w(TAG, "Unexpected state during automatic backup")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during automatic backup", e)
            return Result.retry()
        }
    }

    companion object {
        private const val TAG = "AutomaticBackup"
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
