package io.github.mdsadiqueinam.qamus.data.repository

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.data.database.QamusDatabase
import io.github.mdsadiqueinam.qamus.data.model.BackupInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock.System
import java.io.File
import java.io.FileOutputStream
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
     * Delete existing backup files with the specified prefix in Google Drive.
     *
     * @param driveService The Drive service to use
     * @param folderId The ID of the folder containing the backups
     */
    private suspend fun deleteExistingBackups(driveService: Drive, folderId: String) = withContext(Dispatchers.IO) {
        try {
            // Query for files with the backup prefix in the specified folder
            val query = "'$folderId' in parents and name contains '$BACKUP_FILE_PREFIX' and trashed = false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val files = result.files
            if (files != null && files.isNotEmpty()) {
                Log.d(TAG, "Found ${files.size} existing backup files to delete")

                // Delete each file
                for (file in files) {
                    Log.d(TAG, "Deleting backup file: ${file.name}")
                    driveService.files().delete(file.id).execute()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting existing backups", e)
        }
    }

    /**
     * Backup the entire database file to Google Drive.
     * First deletes any existing backups with the same prefix.
     *
     * @param accountName The Google account name to use for authentication
     * @return The ID of the created backup file, or null if backup failed
     */
    suspend fun backupToGoogleDrive(accountName: String): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accountName)

            // Get the database file
            val databaseFile = context.getDatabasePath(DATABASE_NAME)

            if (!databaseFile.exists()) {
                Log.e(TAG, "Database file does not exist at ${databaseFile.absolutePath}")
                return@withContext null
            }

            // Find or create the backup folder in Google Drive
            val folderId = findOrCreateBackupFolder(driveService)

            // Delete existing backups
            deleteExistingBackups(driveService, folderId)

            // Create a temporary file to store the backup
            val backupAt = System.now()
            val timestamp = backupAt.toEpochMilliseconds()
            val backupFileName = "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION_DB"
            val backupFile = File(context.cacheDir, backupFileName)

            // Copy the database file to the temporary location
            databaseFile.inputStream().use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

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

            // Return the ID and the timestamp of the backup in pair
            return@withContext BackupInfo(id = uploadedFile.id, backupAt = backupAt)
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up database to Google Drive", e)
            return@withContext null
        }
    }

    /**
     * Find the latest backup file with the specified prefix in Google Drive.
     *
     * @param driveService The Drive service to use
     * @return The ID of the latest backup file, or null if no backup files were found
     */
    private suspend fun findLatestBackup(driveService: Drive): String? = withContext(Dispatchers.IO) {
        try {
            // Find the backup folder
            val folderId = findOrCreateBackupFolder(driveService)

            // Query for files with the backup prefix in the specified folder, ordered by creation time descending
            val query = "'$folderId' in parents and name contains '$BACKUP_FILE_PREFIX' and trashed = false"
            val result = driveService.files().list()
                .setQ(query)
                .setOrderBy("createdTime desc")
                .setSpaces("drive")
                .setFields("files(id, name, createdTime)")
                .setPageSize(1)  // We only need the most recent one
                .execute()

            val files = result.files
            if (files != null && files.isNotEmpty()) {
                val latestFile = files[0]
                Log.d(TAG, "Found latest backup file: ${latestFile.name}")
                return@withContext latestFile.id
            }

            Log.d(TAG, "No backup files found")
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding latest backup", e)
            return@withContext null
        }
    }

    /**
     * Restore the entire database from the latest Google Drive database file backup.
     *
     * @param accountName The Google account name to use for authentication
     * @return True if restore was successful, false otherwise
     */
    suspend fun restoreFromGoogleDrive(accountName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accountName)

            // Find the latest backup file
            val fileId = findLatestBackup(driveService)

            if (fileId == null) {
                Log.e(TAG, "No backup files found to restore")
                return@withContext false
            }

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
