package com.wiredog.vpn.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val favoriteServersKey = stringSetPreferencesKey("favorite_servers")
    private val recentServersKey = stringPreferencesKey("recent_servers")
    private val lastConnectedServerKey = stringPreferencesKey("last_connected_server")
    private val selectedServerKey = stringPreferencesKey("selected_server")
    private val latencyCacheKey = stringPreferencesKey("latency_cache")

    val favoriteServerIds: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[favoriteServersKey] ?: emptySet()
    }

    val recentServerIds: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[recentServersKey]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val lastConnectedServerId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[lastConnectedServerKey]
    }

    val selectedServerId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[selectedServerKey]
    }

    // Stored as "id1:42,id2:88,..."
    suspend fun getCachedLatencies(): Map<String, Int> {
        return dataStore.data.map { prefs ->
            prefs[latencyCacheKey]
                ?.split(",")
                ?.mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: return@mapNotNull null)
                    else null
                }
                ?.toMap()
                ?: emptyMap()
        }.first()
    }

    suspend fun saveLatencies(latencies: Map<String, Int>) {
        dataStore.edit { prefs ->
            prefs[latencyCacheKey] = latencies.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun toggleFavorite(serverId: String) {
        dataStore.edit { prefs ->
            val currentFavorites = prefs[favoriteServersKey] ?: emptySet()
            prefs[favoriteServersKey] = if (serverId in currentFavorites) {
                currentFavorites - serverId
            } else {
                currentFavorites + serverId
            }
        }
    }

    suspend fun addToRecent(serverId: String) {
        dataStore.edit { prefs ->
            val currentRecent = prefs[recentServersKey]
                ?.split(",")
                ?.filter { it.isNotEmpty() }
                ?.toMutableList()
                ?: mutableListOf()

            // Remove if already exists, add to front
            currentRecent.remove(serverId)
            currentRecent.add(0, serverId)

            // Keep only last 5
            val trimmed = currentRecent.take(5)
            prefs[recentServersKey] = trimmed.joinToString(",")
        }
    }

    suspend fun setLastConnectedServer(serverId: String) {
        dataStore.edit { prefs ->
            prefs[lastConnectedServerKey] = serverId
        }
    }

    suspend fun setSelectedServer(serverId: String) {
        dataStore.edit { prefs ->
            prefs[selectedServerKey] = serverId
        }
    }

    suspend fun clearSelectedServer() {
        dataStore.edit { prefs ->
            prefs.remove(selectedServerKey)
        }
    }
}
