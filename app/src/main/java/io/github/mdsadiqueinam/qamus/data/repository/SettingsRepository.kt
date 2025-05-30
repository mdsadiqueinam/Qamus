package io.github.mdsadiqueinam.qamus.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for Context to create a DataStore instance
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for accessing and modifying application settings.
 * Follows Single Responsibility Principle by focusing only on settings management.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "SettingsRepository"
    }

    // Define keys for preferences
    private object PreferencesKeys {
        val REMINDER_INTERVAL = intPreferencesKey("reminder_interval")
        val LAST_BACKUP_AT = stringPreferencesKey("last_backup_at")
        val LAST_BACKUP_VERSION = longPreferencesKey("last_backup_version")
        val IS_REMINDER_ENABLED = booleanPreferencesKey("is_reminder_enabled")
        val AUTOMATIC_BACKUP_FREQUENCY = stringPreferencesKey("automatic_backup_frequency")
        val USE_MOBILE_DATA = booleanPreferencesKey("use_mobile_data")
    }

    /**
     * Get the settings as a Flow.
     * Results are cached and distinct until changed.
     * 
     * @return A Flow of Settings containing the current application settings
     */
    val settings: Flow<Settings> = context.settingsDataStore.data
        .catch { e -> 
            // Log error and emit default settings
            android.util.Log.e(TAG, "Error reading settings", e)
        }
        .map { preferences ->
            Settings(
                reminderInterval = preferences[PreferencesKeys.REMINDER_INTERVAL] ?: Settings.DEFAULT_REMINDER_INTERVAL,
                lastBackupAt = preferences[PreferencesKeys.LAST_BACKUP_AT]?.let { Instant.parse(it) },
                lastBackupVersion = preferences[PreferencesKeys.LAST_BACKUP_VERSION] ?: 0,
                isReminderEnabled = preferences[PreferencesKeys.IS_REMINDER_ENABLED] == true,
                automaticBackupFrequency = preferences[PreferencesKeys.AUTOMATIC_BACKUP_FREQUENCY]?.let { 
                    Settings.AutomaticBackupFrequency.entries.find { freq -> freq.value == it }
                } ?: Settings.AutomaticBackupFrequency.OFF,
                useMobileData = preferences[PreferencesKeys.USE_MOBILE_DATA] == true
            )
        }
        .distinctUntilChanged()

    /**
     * Update the reminder interval.
     * 
     * @param interval The new reminder interval in minutes
     */
    suspend fun updateReminderInterval(interval: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_INTERVAL] = interval
        }
    }

    /**
     * Update the last backup information.
     * 
     * @param timestamp The timestamp of the last backup
     * @param version The version number of the last backup
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

    /**
     * Enable or disable the reminder.
     * 
     * @param isEnabled Whether the reminder should be enabled
     */
    suspend fun setReminderEnabled(isEnabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_REMINDER_ENABLED] = isEnabled
        }
    }

    /**
     * Update the automatic backup frequency.
     * 
     * @param frequency The new automatic backup frequency
     */
    suspend fun updateAutomaticBackupFrequency(frequency: Settings.AutomaticBackupFrequency) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOMATIC_BACKUP_FREQUENCY] = frequency.value
        }
    }

    /**
     * Enable or disable using mobile data for automatic backup.
     * 
     * @param isEnabled Whether mobile data should be used for automatic backup
     */
    suspend fun setUseMobileData(isEnabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_MOBILE_DATA] = isEnabled
        }
    }

    /**
     * Check if automatic backup can be performed based on network connectivity.
     * 
     * @return true if backup can be performed, false otherwise
     */
    suspend fun canPerformAutomaticBackup(): Boolean {
        val settings = settings.first()
        if (!settings.useMobileData) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
        return true
    }
}
