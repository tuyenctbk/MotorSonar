package com.soloprono.motorsonar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesManager private constructor(context: Context) {

    private val applicationContext = context.applicationContext

    companion object {
        val KEY_UNITS_OF_MEASUREMENT = stringPreferencesKey("units_of_measurement") // "Imperial" vs "Metric"
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DIAGNOSTIC_SENSITIVITY = stringPreferencesKey("diagnostic_sensitivity") // "Low", "Standard", "High"

        @Volatile
        private var INSTANCE: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val unitsOfMeasurement: Flow<String> = applicationContext.dataStore.data
        .map { preferences ->
            preferences[KEY_UNITS_OF_MEASUREMENT] ?: "Metric"
        }

    val notificationsEnabled: Flow<Boolean> = applicationContext.dataStore.data
        .map { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
        }

    val autoSyncEnabled: Flow<Boolean> = applicationContext.dataStore.data
        .map { preferences ->
            preferences[KEY_AUTO_SYNC] ?: true
        }

    val themeMode: Flow<String> = applicationContext.dataStore.data
        .map { preferences ->
            preferences[KEY_THEME_MODE] ?: "SYSTEM"
        }

    val diagnosticSensitivity: Flow<String> = applicationContext.dataStore.data
        .map { preferences ->
            preferences[KEY_DIAGNOSTIC_SENSITIVITY] ?: "Standard"
        }

    suspend fun setUnitsOfMeasurement(units: String) {
        applicationContext.dataStore.edit { preferences ->
            preferences[KEY_UNITS_OF_MEASUREMENT] = units
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        applicationContext.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        applicationContext.dataStore.edit { preferences ->
            preferences[KEY_AUTO_SYNC] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        applicationContext.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setDiagnosticSensitivity(sensitivity: String) {
        applicationContext.dataStore.edit { preferences ->
            preferences[KEY_DIAGNOSTIC_SENSITIVITY] = sensitivity
        }
    }
}
