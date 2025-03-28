package io.github.mdsadiqueinam.qamus.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mdsadiqueinam.qamus.data.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for Context to create a DataStore instance
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for accessing and modifying application settings.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Define keys for preferences
    private object PreferencesKeys {
        val REMINDER_INTERVAL = intPreferencesKey("reminder_interval")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_VERSION = longPreferencesKey("last_backup_version")
        val IS_REMINDER_ENABLED = booleanPreferencesKey("is_reminder_enabled")
        val GOOGLE_ACCOUNT = stringPreferencesKey("google_account")
    }

    /**
     * Get the settings as a Flow.
     */
    val settings: Flow<Settings> = context.settingsDataStore.data.map { preferences ->
        Settings(
            reminderInterval = preferences[PreferencesKeys.REMINDER_INTERVAL] ?: Settings.DEFAULT_REMINDER_INTERVAL,
            lastBackupAt = preferences[PreferencesKeys.LAST_BACKUP_AT]?.let { Instant.fromEpochMilliseconds(it) },
            lastBackupVersion = preferences[PreferencesKeys.LAST_BACKUP_VERSION] ?: 0,
            isReminderEnabled = preferences[PreferencesKeys.IS_REMINDER_ENABLED] == true,
            googleAccount = preferences[PreferencesKeys.GOOGLE_ACCOUNT]
        )
    }

    /**
     * Update the reminder interval.
     */
    suspend fun updateReminderInterval(interval: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_INTERVAL] = interval
        }
    }

    /**
     * Update the last backup information.
     */
    suspend fun updateLastBackup(timestamp: Instant, version: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_AT] = timestamp.toEpochMilliseconds()
            preferences[PreferencesKeys.LAST_BACKUP_VERSION] = version
        }
    }

    /**
     * Reset settings to default values.
     */
    suspend fun resetSettings() {
        context.settingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Enable or disable the reminder.
     */
    suspend fun setReminderEnabled(isEnabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_REMINDER_ENABLED] = isEnabled
        }
    }

    /**
     * Update the Google account used for backup/restore.
     */
    suspend fun updateGoogleAccount(accountName: String?) {
        context.settingsDataStore.edit { preferences ->
            if (accountName != null) {
                preferences[PreferencesKeys.GOOGLE_ACCOUNT] = accountName
            } else {
                preferences.remove(PreferencesKeys.GOOGLE_ACCOUNT)
            }
        }
    }
}
