package com.wiredog.vpn.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_config")

@Singleton
class AppConfigPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CONFIG_JSON = stringPreferencesKey("config_json")
        val LAST_FETCH_TIMESTAMP = longPreferencesKey("last_fetch_timestamp")
    }

    companion object {
        const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    suspend fun getCachedConfigJson(): String? {
        return context.appConfigDataStore.data.map { prefs ->
            prefs[Keys.CONFIG_JSON]
        }.first()
    }

    suspend fun getLastFetchTimestamp(): Long {
        return context.appConfigDataStore.data.map { prefs ->
            prefs[Keys.LAST_FETCH_TIMESTAMP] ?: 0L
        }.first()
    }

    suspend fun saveConfig(json: String) {
        context.appConfigDataStore.edit { prefs ->
            prefs[Keys.CONFIG_JSON] = json
            prefs[Keys.LAST_FETCH_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun isCacheFresh(): Boolean {
        val timestamp = getLastFetchTimestamp()
        return (System.currentTimeMillis() - timestamp) < MAX_CACHE_AGE_MS
    }
}
