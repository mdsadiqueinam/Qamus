package io.github.mdsadiqueinam.qamus.data.repository

import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.credentials.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.database.QamusDatabase
import io.github.mdsadiqueinam.qamus.data.repository.auth.AuthManager
import io.github.mdsadiqueinam.qamus.data.repository.drive.DriveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
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
        private const val BACKUP_FOLDER_NAME = "QamusBackups"
        private const val DATABASE_NAME = "qamus_database"
    }

    private val credentialManager = CredentialManager.create(context)
    private val driveService get() = getGoogleDrive()

    // Instantiate a Google sign-in request
    val googleIdOption = GetGoogleIdOption.Builder()
        // Your server's client ID, not your Android client ID.
        .setServerClientId(context.getString(R.string.default_web_client_id))
        // Show all available Google accounts, not just previously authorized ones
        .setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(true).setNonce("nonce").build()

    // Create the Credential Manager request
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

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

    private fun getGoogleDrive(): Drive {
        val drive = authManager.currentUser?.email?.let { email ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = Account(email, "google.com")
            }

            Drive.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Qamus").build()
        }

        return drive ?: throw IllegalStateException("Not signed in")
    }
}

/**
 * Data class representing metadata for a backup file.
 *
 * @property id The ID of the backup file on Google Drive
 * @property name The name of the backup file
 * @property createdTime The time when the backup was created
 * @property size The size of the backup file in bytes
 */
data class BackupMetadata(
    val id: String, val name: String, val createdTime: String, val size: Int
)

sealed class DataTransferState {
    enum class TransferType {
        BACKUP, RESTORE
    }

    data class Uploading(val progress: Int, val bytes: Long, val type: TransferType) : DataTransferState()
    object Success : DataTransferState()
    data class Error(
        val message: String, val type: TransferType, val exception: Exception? = null
    ) : DataTransferState()
}
