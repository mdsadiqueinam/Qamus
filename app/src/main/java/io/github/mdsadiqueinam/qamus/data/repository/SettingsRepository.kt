package io.github.mdsadiqueinam.qamus.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    private val context: Context
) {
    // Define keys for preferences
    private object PreferencesKeys {
        val REMINDER_INTERVAL = longPreferencesKey("reminder_interval")
        val LAST_BACKUP_AT = stringPreferencesKey("last_backup_at")
        val LAST_BACKUP_VERSION = longPreferencesKey("last_backup_version")
    }

    /**
     * Get the settings as a Flow.
     */
    val settings: Flow<Settings> = context.settingsDataStore.data.map { preferences ->
        Settings(
            reminderInterval = preferences[PreferencesKeys.REMINDER_INTERVAL] ?: Settings.DEFAULT_REMINDER_INTERVAL,
            lastBackupAt = preferences[PreferencesKeys.LAST_BACKUP_AT]?.let { Instant.parse(it) },
            lastBackupVersion = preferences[PreferencesKeys.LAST_BACKUP_VERSION] ?: 0
        )
    }

    /**
     * Update the reminder interval.
     */
    suspend fun updateReminderInterval(interval: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_INTERVAL] = interval
        }
    }

    /**
     * Update the last backup information.
     */
    suspend fun updateLastBackup(timestamp: Instant, version: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_AT] = timestamp.toString()
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
}