package io.github.mdsadiqueinam.qamus.data.repository

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.data.database.QamusDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for backing up and restoring database to/from Google Drive.
 */
@Singleton
class BackupRestoreRepository @Inject constructor(
    private val qamusDatabase: QamusDatabase,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BackupRestoreRepository"
        private const val BACKUP_FOLDER_NAME = "QamusBackups"
        private const val BACKUP_FILE_PREFIX = "qamus_backup_"
        private const val BACKUP_FILE_EXTENSION_DB = ".db"
        private const val MIME_TYPE_DB = "application/octet-stream"
        private const val MIME_TYPE_FOLDER = "application/vnd.google-apps.folder"
        private const val DATABASE_NAME = "qamus_database"
    }

    private val gson = Gson()

    /**
     * Create a Google Drive API client.
     *
     * @param accountName The Google account name to use for authentication
     * @return A configured Drive client
     */
    private fun getDriveService(accountName: String): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccountName = accountName
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Qamus")
            .build()
    }

    /**
     * Find or create the backup folder in Google Drive.
     *
     * @param driveService The Drive service to use
     * @return The ID of the backup folder
     */
    private fun findOrCreateBackupFolder(driveService: Drive): String {
        // Check if the backup folder already exists
        val query = "name = '$BACKUP_FOLDER_NAME' and mimeType = '$MIME_TYPE_FOLDER' and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()

        val files = result.files

        // If folder exists, return its ID
        if (files != null && files.isNotEmpty()) {
            return files[0].id
        }

        // If folder doesn't exist, create it
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            name = BACKUP_FOLDER_NAME
            mimeType = MIME_TYPE_FOLDER
        }

        val folder = driveService.files().create(folderMetadata)
            .setFields("id")
            .execute()

        return folder.id
    }

    /**
     * Backup the entire database file to Google Drive.
     *
     * @param accountName The Google account name to use for authentication
     * @return The ID of the created backup file, or null if backup failed
     */
    suspend fun backupToGoogleDrive(accountName: String): String? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accountName)

            // Get the database file
            val databaseFile = context.getDatabasePath(DATABASE_NAME)

            if (!databaseFile.exists()) {
                Log.e(TAG, "Database file does not exist at ${databaseFile.absolutePath}")
                return@withContext null
            }

            // Create a temporary file to store the backup
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION_DB"
            val backupFile = File(context.cacheDir, backupFileName)

            // Copy the database file to the temporary location
            databaseFile.inputStream().use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Find or create the backup folder in Google Drive
            val folderId = findOrCreateBackupFolder(driveService)

            // Create file metadata
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = backupFileName
                parents = listOf(folderId)
            }

            // Create file content
            val fileContent = FileContent(MIME_TYPE_DB, backupFile)

            // Upload file to Google Drive
            val uploadedFile = driveService.files().create(fileMetadata, fileContent)
                .setFields("id")
                .execute()

            // Delete the temporary file
            backupFile.delete()

            return@withContext uploadedFile.id
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database to Google Drive", e)
            return@withContext null
        }
    }

    /**
     * Restore the entire database from a Google Drive database file backup.
     *
     * @param accountName The Google account name to use for authentication
     * @param fileId The ID of the backup file to restore from
     * @return True if restore was successful, false otherwise
     */
    suspend fun restoreFromGoogleDrive(accountName: String, fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accountName)

            // Create a temporary file to store the downloaded database
            val tempFile = File(context.cacheDir, "temp_$DATABASE_NAME")

            // Download the file from Google Drive
            val outputStream = FileOutputStream(tempFile)
            driveService.files().get(fileId)
                .executeMediaAndDownloadTo(outputStream)
            outputStream.close()

            // Close the database
            qamusDatabase.close()

            // Get the database file path
            val databaseFile = context.getDatabasePath(DATABASE_NAME)

            // Delete the current database file
            if (databaseFile.exists()) {
                databaseFile.delete()
            }

            // Ensure the parent directory exists
            databaseFile.parentFile?.mkdirs()

            // Copy the downloaded database file to the database location
            tempFile.inputStream().use { input ->
                FileOutputStream(databaseFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Delete the temporary file
            tempFile.delete()

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database from Google Drive", e)
            return@withContext false
        }
    }
}
