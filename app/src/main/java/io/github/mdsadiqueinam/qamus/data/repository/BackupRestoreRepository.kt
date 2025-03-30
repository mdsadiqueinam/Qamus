package io.github.mdsadiqueinam.qamus.data.repository

import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.credentials.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.R
import io.github.mdsadiqueinam.qamus.data.database.QamusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
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

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn = _isSignedIn.asStateFlow()
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

    init {
        // Initialize the sign-in state based on Firebase Auth
        _isSignedIn.value = firebaseAuth.currentUser != null
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
                _isSignedIn.value = true
            } else {
                // If sign in fails, display a message to the user
                Log.w(TAG, "signInWithCredential:failure", task.exception)
                _isSignedIn.value = false
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

    suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.signOut()
            val clearRequest = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(clearRequest)
            _isSignedIn.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
        }
    }

    suspend fun backupDatabase(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                return@withContext Result.failure(IllegalStateException("Not signed in"))
            }

            // Create backup folder if it doesn't exist
            val folderId = getOrCreateBackupFolder()

            // Get the database file
            val databaseFile = context.getDatabasePath(DATABASE_NAME)
            if (!databaseFile.exists()) {
                return@withContext Result.failure(IOException("Database file not found"))
            }

            // Create backup file metadata
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val backupFileName = "qamus_backup_$timestamp.db"

            val fileMetadata =
                File().setName(backupFileName).setParents(listOf(folderId)).setMimeType("application/octet-stream")

            // Upload the file
            val fileContent = java.io.File(databaseFile.path)
            val mediaContent = com.google.api.client.http.FileContent("application/octet-stream", fileContent)

            val uploadedFile =
                driveService!!.files().create(fileMetadata, mediaContent).setFields("id, name, createdTime, size")
                    .execute()

            Log.d(TAG, "Backup successful: ${uploadedFile.id}")
            Result.success(uploadedFile.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database", e)
            Result.failure(e)
        }
    }

    suspend fun restoreDatabase(backupFileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                return@withContext Result.failure(IllegalStateException("Not signed in"))
            }

            // Get the database file path
            val databaseFile = context.getDatabasePath(DATABASE_NAME)

            // Close the database connection
            QamusDatabase.getDatabase(context).close()

            // Download the backup file
            val outputStream = FileOutputStream(databaseFile)
            driveService!!.files().get(backupFileId).executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            Log.d(TAG, "Restore successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database", e)
            Result.failure(e)
        }
    }

    suspend fun listBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                return@withContext Result.failure(IllegalStateException("Not signed in"))
            }

            val folderId = getOrCreateBackupFolder()

            val result =
                driveService!!.files().list().setQ("'$folderId' in parents and mimeType='application/octet-stream'")
                    .setFields("files(id, name, createdTime, size)").execute()

            val backups = result.files.map { file ->
                BackupMetadata(
                    id = file.id, name = file.name, createdTime = file.createdTime.toStringRfc3339(), size = file.size
                )
            }

            Result.success(backups)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing backups", e)
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(backupFileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                return@withContext Result.failure(IllegalStateException("Not signed in"))
            }

            driveService!!.files().delete(backupFileId).execute()

            Log.d(TAG, "Delete successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup", e)
            Result.failure(e)
        }
    }

    private suspend fun getOrCreateBackupFolder(): String = withContext(Dispatchers.IO) {
        // Check if backup folder already exists
        val folderQuery = driveService!!.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER_NAME'").setSpaces("drive")
            .setFields("files(id)").execute()

        // If folder exists, return its ID
        if (folderQuery.files.isNotEmpty()) {
            return@withContext folderQuery.files[0].id
        }

        // Create folder if it doesn't exist
        val folderMetadata = File().setName(BACKUP_FOLDER_NAME).setMimeType("application/vnd.google-apps.folder")

        val folder = driveService!!.files().create(folderMetadata).setFields("id").execute()

        return@withContext folder.id
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
