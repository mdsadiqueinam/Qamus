package io.github.mdsadiqueinam.qamus.data.repository


import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BackupRestoreRepository @Inject constructor(
    @ApplicationContext private val context: Context, private val firebaseAuth: FirebaseAuth
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

    fun observeUserState(): Flow<FirebaseUser?> {
        return callbackFlow {
            val authListener = FirebaseAuth.AuthStateListener {
                val currentUser = it.currentUser
                trySend(currentUser)
            }
            firebaseAuth.addAuthStateListener(authListener)
            awaitClose {
                firebaseAuth.removeAuthStateListener(authListener)
            }
        }
    }

    suspend fun signIn(activity: Context) {
        try {
            // Launch Credential Manager UI
            val result = credentialManager.getCredential(
                context = activity, request = request
            )

            // Extract credential from the result returned by Credential Manager
            handleSignIn(result.credential)
        } catch (e: Exception) {
            // Log the exception with detailed information
            Log.e(TAG, "Couldn't retrieve user's credentials: ${e.localizedMessage}")
        }
    }

    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Credential is not of type Google ID!")
        }
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success")
            } else {
                // If sign in fails, display a message to the user
                Log.w(TAG, "signInWithCredential:failure", task.exception)
            }
        }
    }

    private fun getGoogleDrive(): Drive? {
        return firebaseAuth.currentUser?.email?.let { email ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            ).apply {
                selectedAccount = Account(email, "google.com")
            }

            Drive.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Qamus").build()
        }
    }

    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
        }
    }

    fun backupDatabase(): Flow<DataTransferState> = callbackFlow {
        try {
            val listener = MediaHttpUploaderProgressListener {
                val progressPercentage = (it.progress * 100).toInt()
                Log.d(TAG, "Upload progress: $progressPercentage%")
                trySend(
                    DataTransferState.Uploading(
                        progressPercentage, DataTransferState.TransferType.BACKUP
                    )
                )
            }

            if (driveService == null) {
                trySend(
                    DataTransferState.Error(
                        "Not signed in", DataTransferState.TransferType.BACKUP
                    )
                )
                return@callbackFlow
            }

            // Create backup folder if it doesn't exist
            val folderId = getOrCreateBackupFolder()

            // Get the database file
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            if (!databaseFile.exists()) {
                trySend(
                    DataTransferState.Error(
                        "Database file not found", DataTransferState.TransferType.BACKUP
                    )
                )
                return@callbackFlow
            }

            deleteExistingBackup()

            // Create backup file metadata
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val backupFileName = "qamus_backup_$timestamp.db"

            val fileMetadata = File().setName(backupFileName).setParents(listOf(folderId))
                .setMimeType("application/octet-stream")

            // Upload the file and emit progress
            val file = java.io.File(databaseFile.path)
            val mediaContent = InputStreamContent(
                "application/octet-stream", BufferedInputStream(
                    FileInputStream(file)
                )
            )
            mediaContent.setLength(file.length())

            val uploadedFile = driveService!!.files().create(fileMetadata, mediaContent)
                .setFields("id, name, createdTime, size").apply {
                    mediaHttpUploader.setProgressListener(listener)
                }.execute()

            Log.d(TAG, "Backup successful: ${uploadedFile.id}")
            trySend(DataTransferState.Success)
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database", e)
            trySend(
                DataTransferState.Error(
                    "Error backing up database: ${e.message}",
                    DataTransferState.TransferType.BACKUP,
                    e
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun deleteExistingBackup() {
        // Delete existing backups
        val existingBackups = listBackupsInternal()
        if (existingBackups != null && existingBackups.isNotEmpty()) {
            existingBackups.forEach { backup ->
                Log.d(TAG, "Deleting previous backup: ${backup.id}")
                driveService!!.files().delete(backup.id).execute()
            }
        }
    }

    fun restoreDatabase(): Flow<DataTransferState> = callbackFlow {
        try {
            val listener = MediaHttpDownloaderProgressListener {
                val progressPercentage = (it.progress * 100).toInt()
                Log.d(TAG, "Download progress: $progressPercentage%")
                trySend(
                    DataTransferState.Uploading(
                        progressPercentage, DataTransferState.TransferType.RESTORE
                    )
                )
            }
            if (driveService == null) {
                trySend(
                    DataTransferState.Error(
                        "Not signed in", type = DataTransferState.TransferType.RESTORE
                    )
                )
                return@callbackFlow
            }

            // Find the latest backup
            val backups = listBackupsInternal()
            if (backups == null) {
                trySend(
                    DataTransferState.Error(
                        "Failed to list backups", type = DataTransferState.TransferType.RESTORE
                    )
                )
                return@callbackFlow
            }

            if (backups.isEmpty()) {
                trySend(
                    DataTransferState.Error(
                        "No backups found", type = DataTransferState.TransferType.RESTORE
                    )
                )
                return@callbackFlow
            }

            // Sort backups by creation time (newest first)
            val latestBackup = backups.maxByOrNull { it.createdTime }

            if (latestBackup == null) {
                trySend(
                    DataTransferState.Error(
                        "No valid backup found", type = DataTransferState.TransferType.RESTORE
                    )
                )
                return@callbackFlow
            }

            // Get the database file path
            val databaseFile = context.getDatabasePath(DATABASE_NAME)

            // Close the database connection
            QamusDatabase.getDatabase(context).close()

            // Download the backup file
            val outputStream = FileOutputStream(databaseFile)
            driveService!!.files().get(latestBackup.id).apply {
                mediaHttpDownloader.setProgressListener(listener)
            }.executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            Log.d(TAG, "Restore successful from backup: ${latestBackup.id}")
            trySend(DataTransferState.Success)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database", e)
            trySend(
                DataTransferState.Error(
                    "Error restoring database: ${e.message}",
                    type = DataTransferState.TransferType.RESTORE,
                    e
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun listBackupsInternal(): List<BackupMetadata>? {
        val folderId = getOrCreateBackupFolder()

        val result = driveService!!.files().list()
            .setQ("'$folderId' in parents and mimeType='application/octet-stream'")
            .setFields("files(id, name, createdTime, size)").execute()

        return result?.let {
            it.files.map { file ->
                BackupMetadata(
                    id = file.id,
                    name = file.name,
                    createdTime = file.createdTime.toStringRfc3339(),
                    size = file.size
                )
            }
        }
    }


    private fun getOrCreateBackupFolder(): String? {
        // Check if backup folder already exists
        val folderQuery = driveService!!.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER_NAME'")
            .setSpaces("drive").setFields("files(id)").execute()

        // If folder exists, return its ID
        if (folderQuery.files.isNotEmpty()) {
            return folderQuery.files[0].id
        }

        // Create folder if it doesn't exist
        val folderMetadata =
            File().setName(BACKUP_FOLDER_NAME).setMimeType("application/vnd.google-apps.folder")

        val folder = driveService!!.files().create(folderMetadata).setFields("id").execute()

        return folder.id
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

    data class Uploading(val progress: Int, val type: TransferType) : DataTransferState()
    object Success : DataTransferState()
    data class Error(
        val message: String, val type: TransferType, val exception: Exception? = null
    ) : DataTransferState()
}
