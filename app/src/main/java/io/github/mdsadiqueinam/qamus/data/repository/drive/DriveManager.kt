package io.github.mdsadiqueinam.qamus.data.repository.drive

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.data.repository.auth.AuthManager
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Google Drive operations.
 * Follows Single Responsibility Principle by focusing only on Drive operations.
 */
@Singleton
class DriveManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) {
    companion object {
        private const val TAG = "DriveManager"
        private const val BACKUP_FOLDER_NAME = "QamusBackups"
    }

    /**
     * Get the Google Drive service
     */
    private fun getDriveService(): Drive {
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

    /**
     * Backup a file to Google Drive
     */
    fun backupFile(
        filePath: String,
        fileName: String,
        onProgress: (Int, Long) -> Unit
    ) {
        val driveService = getDriveService()
        
        // Create backup folder if it doesn't exist
        val folderId = getOrCreateBackupFolder(driveService)

        // Get existing backups
        val existingBackups = listBackups(driveService)

        // Create backup file metadata with timestamp
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val backupFileName = "${fileName.split(".")[0]}_$timestamp.${fileName.split(".")[1]}"

        val fileMetadata = File()
            .setName(backupFileName)
            .setParents(listOf(folderId))
            .setMimeType("application/octet-stream")

        // Upload the file and track progress
        val listener = MediaHttpUploaderProgressListener {
            val progressPercentage = (it.progress * 100).toInt()
            Log.d(TAG, "Upload progress: $progressPercentage%")
            onProgress(progressPercentage, it.numBytesUploaded)
        }

        val file = java.io.File(filePath)
        val mediaContent = InputStreamContent(
            "application/octet-stream", 
            BufferedInputStream(FileInputStream(file))
        )
        mediaContent.setLength(file.length())

        val uploadedFile = driveService.files()
            .create(fileMetadata, mediaContent)
            .setFields("id, name, createdTime, size")
            .apply {
                mediaHttpUploader.setProgressListener(listener)
            }
            .execute()

        Log.d(TAG, "Backup successful: ${uploadedFile.id}")

        // Delete existing backups after new backup
        if (existingBackups.isNotEmpty()) {
            existingBackups.forEach { backup ->
                Log.d(TAG, "Deleting previous backup: ${backup.id}")
                driveService.files().delete(backup.id).execute()
            }
        }
    }

    /**
     * Restore a file from Google Drive
     */
    fun restoreFile(
        fileName: String,
        outputPath: String,
        onProgress: (Int, Long) -> Unit
    ) {
        val driveService = getDriveService()
        
        // Find the latest backup
        val backups = listBackups(driveService)
        if (backups.isEmpty()) {
            throw IOException("No backups found")
        }

        // Sort backups by creation time (newest first)
        val latestBackup = backups.maxByOrNull { it.createdTime }
            ?: throw IOException("No valid backup found")

        // Download the backup file
        val listener = MediaHttpDownloaderProgressListener {
            val progressPercentage = (it.progress * 100).toInt()
            Log.d(TAG, "Download progress: $progressPercentage%")
            onProgress(progressPercentage, it.numBytesDownloaded)
        }

        val outputStream = FileOutputStream(outputPath)
        driveService.files()
            .get(latestBackup.id)
            .apply {
                mediaHttpDownloader.setProgressListener(listener)
            }
            .executeMediaAndDownloadTo(outputStream)
        outputStream.close()

        Log.d(TAG, "Restore successful from backup: ${latestBackup.id}")
    }

    /**
     * List all backups in the backup folder
     */
    private fun listBackups(driveService: Drive): List<BackupMetadata> {
        val folderId = getOrCreateBackupFolder(driveService)

        val result = driveService.files()
            .list()
            .setQ("'$folderId' in parents and mimeType='application/octet-stream'")
            .setFields("files(id, name, createdTime, size)")
            .execute()

        return result.files.map { file ->
            BackupMetadata(
                id = file.id,
                name = file.name,
                createdTime = file.createdTime.toStringRfc3339(),
                size = file.size
            )
        }
    }

    /**
     * Get or create the backup folder
     */
    private fun getOrCreateBackupFolder(driveService: Drive): String {
        // Check if backup folder already exists
        val folderQuery = driveService.files()
            .list()
            .setQ("mimeType='application/vnd.google-apps.folder' and name='$BACKUP_FOLDER_NAME'")
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()

        // If folder exists, return its ID
        if (folderQuery.files.isNotEmpty()) {
            return folderQuery.files[0].id
        }

        // Create folder if it doesn't exist
        val folderMetadata = File()
            .setName(BACKUP_FOLDER_NAME)
            .setMimeType("application/vnd.google-apps.folder")

        val folder = driveService.files()
            .create(folderMetadata)
            .setFields("id")
            .execute()

        return folder.id
    }
}

/**
 * Data class representing backup metadata
 */
data class BackupMetadata(
    val id: String,
    val name: String,
    val createdTime: String,
    val size: Int
) 