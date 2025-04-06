package io.github.mdsadiqueinam.qamus.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.data.database.QamusDatabase
import io.github.mdsadiqueinam.qamus.data.repository.auth.AuthManager
import io.github.mdsadiqueinam.qamus.data.repository.drive.DriveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for backup and restore operations.
 * Follows Single Responsibility Principle by delegating authentication and drive operations.
 */
@Singleton
class BackupRestoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val driveManager: DriveManager,
    private val database: QamusDatabase,
) {

    companion object {
        private const val TAG = "BackupRestoreRepository"
        private const val DATABASE_NAME = "qamus_database"
    }

    /**
     * Observe the current user state
     */
    fun observeUserState(): Flow<FirebaseUser?> = authManager.observeUserState()

    /**
     * Sign in the user
     */
    suspend fun signIn(activity: Context) = authManager.signIn(activity)

    /**
     * Sign out the user
     */
    suspend fun signOut() = authManager.signOut()

    /**
     * Backup the database to Google Drive
     */
    fun backupDatabase(): Flow<DataTransferState> = callbackFlow {
        try {
            // Get the database file
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            if (!databaseFile.exists()) {
                throw IOException("Database file does not exist")
            }

            // Perform the backup operation
            driveManager.backupFile(
                filePath = databaseFile.path,
                fileName = "qamus_backup.db",
                onProgress = { progress, bytesTransferred ->
                    trySend(
                        DataTransferState.Uploading(
                            progress, bytesTransferred, DataTransferState.TransferType.BACKUP
                        )
                    )
                }
            )

            trySend(DataTransferState.Success)
        } catch (e: Exception) {
            Log.w(TAG, "Error backing up database", e)
            trySend(DataTransferState.Error(e.message ?: "Unknown error occurred on backup", DataTransferState.TransferType.BACKUP, e))
        }
        awaitClose {
            Log.d(TAG, "Closing backup flow")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Restore the database from Google Drive
     */
    fun restoreDatabase(): Flow<DataTransferState> = callbackFlow {
        try {
            // Get the database file path
            val databaseFile = context.getDatabasePath(DATABASE_NAME)

            // Close the database connection
            database.close()

            // Perform the restore operation
            driveManager.restoreFile(
                fileName = "qamus_backup.db",
                outputPath = databaseFile.path,
                onProgress = { progress, bytesTransferred ->
                    trySend(
                        DataTransferState.Uploading(
                            progress, bytesTransferred, DataTransferState.TransferType.RESTORE
                        )
                    )
                }
            )

            trySend(DataTransferState.Success)
        } catch (e: Exception) {
            Log.w(TAG, "Error restoring database", e)
            trySend(DataTransferState.Error(e.message ?: "Unknown error occurred on restore", DataTransferState.TransferType.RESTORE, e))
        }
        awaitClose {
            Log.d(TAG, "Closing restore flow")
        }
    }.flowOn(Dispatchers.IO)

}

sealed class DataTransferState {
    enum class TransferType {
        BACKUP, RESTORE
    }

    data class Uploading(val progress: Int, val bytes: Long, val type: TransferType) : DataTransferState()
    data object Success : DataTransferState()
    data class Error(
        val message: String, val type: TransferType, val exception: Exception? = null
    ) : DataTransferState()
}
