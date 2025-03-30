package io.github.mdsadiqueinam.qamus.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.data.database.QamusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GoogleDriveBackupRepo"
private const val BACKUP_FOLDER_NAME = "QamusBackups"
private const val DATABASE_NAME = "qamus_database"

/**
 * Implementation of [BackupRestoreRepository] that uses Google Drive for backup and restore.
 */
@Singleton
class GoogleDriveBackupRestoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) : BackupRestoreRepository {

    private val _isSignedIn = MutableStateFlow(false)
    private var googleSignInClient: GoogleSignInClient? = null
    private var driveService: Drive? = null

    init {
        // Initialize the sign-in state based on Firebase Auth
        _isSignedIn.value = firebaseAuth.currentUser != null

        // Initialize Google Sign-In client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_WEB_CLIENT_ID") // Replace with your web client ID from Firebase console
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Initialize Drive service if user is already signed in
        if (_isSignedIn.value) {
            initializeDriveService()
        }
    }

    override fun isSignedIn(): Flow<Boolean> = _isSignedIn.asStateFlow()

    override fun signIn() {
        // This method should be called from an activity that can handle the sign-in intent
        // The activity should use the returned intent with an ActivityResultLauncher
        val signInIntent = googleSignInClient?.signInIntent
        signInIntent?.let {
            // The intent will be handled by the activity
            // We'll provide a method to handle the result
        }
    }

    /**
     * Handles the result of the Google Sign-In process.
     * This should be called from the activity that initiated the sign-in.
     */
    suspend fun handleSignInResult(data: Intent?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.await()
            firebaseAuthWithGoogle(account)
            initializeDriveService()
            _isSignedIn.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).await()
    }

    private fun initializeDriveService() {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return

        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Qamus")
            .build()
    }

    override suspend fun signOut(): Unit = withContext(Dispatchers.IO) {
        try {
            googleSignInClient?.signOut()?.await()
            firebaseAuth.signOut()
            driveService = null
            _isSignedIn.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
        }
    }

    override suspend fun backupDatabase(): Result<String> = withContext(Dispatchers.IO) {
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

            val fileMetadata = File()
                .setName(backupFileName)
                .setParents(listOf(folderId))
                .setMimeType("application/octet-stream")

            // Upload the file
            val fileContent = java.io.File(databaseFile.path)
            val mediaContent = com.google.api.client.http.FileContent("application/octet-stream", fileContent)

            val uploadedFile = driveService!!.files().create(fileMetadata, mediaContent)
                .setFields("id, name, createdTime, size")
                .execute()

            Log.d(TAG, "Backup successful: ${uploadedFile.id}")
            Result.success(uploadedFile.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database", e)
            Result.failure(e)
        }
    }

    override suspend fun restoreDatabase(backupFileId: String): Result<Unit> = withContext(Dispatchers.IO) {
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
            driveService!!.files().get(backupFileId)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            Log.d(TAG, "Restore successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database", e)
            Result.failure(e)
        }
    }

    override suspend fun listBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        try {
            if (driveService == null) {
                return@withContext Result.failure(IllegalStateException("Not signed in"))
            }

            val folderId = getOrCreateBackupFolder()

            val result = driveService!!.files().list()
                .setQ("'$folderId' in parents and mimeType='application/octet-stream'")
                .setFields("files(id, name, createdTime, size)")
                .execute()

            val backups = result.files.map { file ->
                BackupMetadata(
                    id = file.id,
                    name = file.name,
                    createdTime = file.createdTime.toStringRfc3339(),
                    size = file.getSize() ?: 0
                )
            }

            Result.success(backups)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing backups", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteBackup(backupFileId: String): Result<Unit> = withContext(Dispatchers.IO) {
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
            .setQ("mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER_NAME'")
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()

        // If folder exists, return its ID
        if (folderQuery.files.isNotEmpty()) {
            return@withContext folderQuery.files[0].id
        }

        // Create folder if it doesn't exist
        val folderMetadata = File()
            .setName(BACKUP_FOLDER_NAME)
            .setMimeType("application/vnd.google-apps.folder")

        val folder = driveService!!.files().create(folderMetadata)
            .setFields("id")
            .execute()

        return@withContext folder.id
    }
}
