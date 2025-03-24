package io.github.mdsadiqueinam.qamus.data.model

import kotlinx.datetime.Instant

/**
 * Data class representing the application settings.
 *
 * @property reminderInterval The interval in milliseconds between reminders
 * @property lastBackupAt The timestamp of the last backup
 * @property lastBackupVersion The version of the last backup
 */
data class Settings(
    val reminderInterval: Long = DEFAULT_REMINDER_INTERVAL,
    val lastBackupAt: Instant? = null,
    val lastBackupVersion: Long = 0
) {
    companion object {
        // Default reminder interval: 24 hours in milliseconds
        const val DEFAULT_REMINDER_INTERVAL: Long = 24 * 60 * 60 * 1000
    }
}