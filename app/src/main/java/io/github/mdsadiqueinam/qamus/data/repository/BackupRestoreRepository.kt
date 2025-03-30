package io.github.mdsadiqueinam.qamus.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for backing up and restoring the database.
 */
interface BackupRestoreRepository {
    /**
     * Checks if the user is signed in with Google.
     *
     * @return true if the user is signed in, false otherwise
     */
    fun isSignedIn(): Flow<Boolean>

    /**
     * Initiates the Google Sign-In process.
     * This should be called from an activity that can handle the sign-in intent.
     */
    fun signIn()

    /**
     * Signs out the current user.
     */
    suspend fun signOut()

    /**
     * Backs up the database to Google Drive.
     *
     * @return Result containing the backup file ID if successful, or an exception if failed
     */
    suspend fun backupDatabase(): Result<String>

    /**
     * Restores the database from Google Drive.
     *
     * @param backupFileId The ID of the backup file to restore from
     * @return Result indicating success or failure
     */
    suspend fun restoreDatabase(backupFileId: String): Result<Unit>

    /**
     * Lists all available backups on Google Drive.
     *
     * @return Result containing a list of backup file metadata if successful, or an exception if failed
     */
    suspend fun listBackups(): Result<List<BackupMetadata>>

    /**
     * Deletes a backup file from Google Drive.
     *
     * @param backupFileId The ID of the backup file to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteBackup(backupFileId: String): Result<Unit>
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
    val id: String,
    val name: String,
    val createdTime: String,
    val size: Long
)