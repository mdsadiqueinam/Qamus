package io.github.mdsadiqueinam.qamus.data.model

import kotlinx.datetime.Instant

/**
 * Data class representing the application settings.
 *
 * @property reminderInterval The interval in minutes between reminders
 * @property lastBackupAt The timestamp of the last backup
 * @property lastBackupVersion The version of the last backup
 * @property isReminderEnabled Whether reminders are enabled
 * @property automaticBackupFrequency The frequency of automatic backups
 */
data class Settings(
    val reminderInterval: Int = DEFAULT_REMINDER_INTERVAL,
    val lastBackupAt: Instant? = null,
    val lastBackupVersion: Long = 0,
    val isReminderEnabled: Boolean = false,
    val automaticBackupFrequency: String = AUTOMATIC_BACKUP_OFF
) {
    companion object {
        // Default reminder interval: 60m minutes
        const val DEFAULT_REMINDER_INTERVAL: Int = 60

        // Automatic backup frequency options
        const val AUTOMATIC_BACKUP_OFF: String = "Off"
        const val AUTOMATIC_BACKUP_DAILY: String = "Daily"
        const val AUTOMATIC_BACKUP_WEEKLY: String = "Weekly"
        const val AUTOMATIC_BACKUP_MONTHLY: String = "Monthly"
    }
}
