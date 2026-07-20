package com.wiredog.vpn.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PROTOCOL = stringPreferencesKey("protocol")
        val AUTO_CONNECT_ENABLED = booleanPreferencesKey("auto_connect_enabled")
        val IPV6_ENABLED = booleanPreferencesKey("ipv6_enabled")
        val BLOCK_ADS_ENABLED = booleanPreferencesKey("block_ads_enabled")
        val BLOCK_MALWARE_ENABLED = booleanPreferencesKey("block_malware_enabled")
        val SPLIT_TUNNELING_ENABLED = booleanPreferencesKey("split_tunneling_enabled")
        val SPLIT_TUNNELING_MODE = stringPreferencesKey("split_tunneling_mode")
        val SPLIT_TUNNELING_APPS = stringSetPreferencesKey("split_tunneling_apps")
        val SPLIT_TUNNELING_IPS = stringSetPreferencesKey("split_tunneling_ips")
    }

    // Protocol (default: AmneziaWG)
    val protocol: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.PROTOCOL] ?: "AmneziaWG"
    }

    suspend fun setProtocol(protocol: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.PROTOCOL] = protocol
        }
    }

    // Auto-Connect
    val autoConnectEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.AUTO_CONNECT_ENABLED] ?: false
    }

    suspend fun setAutoConnectEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.AUTO_CONNECT_ENABLED] = enabled
        }
    }

    // IPv6
    val ipv6Enabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.IPV6_ENABLED] ?: true
    }

    suspend fun setIpv6Enabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.IPV6_ENABLED] = enabled
        }
    }

    // Block Ads (DNS filter)
    val blockAdsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.BLOCK_ADS_ENABLED] ?: true
    }

    suspend fun setBlockAdsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BLOCK_ADS_ENABLED] = enabled
        }
    }

    // Block Malware (DNS filter)
    val blockMalwareEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.BLOCK_MALWARE_ENABLED] ?: true
    }

    suspend fun setBlockMalwareEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BLOCK_MALWARE_ENABLED] = enabled
        }
    }

    // Split Tunneling
    val splitTunnelingEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SPLIT_TUNNELING_ENABLED] ?: false
    }

    suspend fun setSplitTunnelingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SPLIT_TUNNELING_ENABLED] = enabled
        }
    }

    val splitTunnelingMode: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SPLIT_TUNNELING_MODE] ?: "include"
    }

    suspend fun setSplitTunnelingMode(mode: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SPLIT_TUNNELING_MODE] = mode
        }
    }

    val splitTunnelingApps: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SPLIT_TUNNELING_APPS] ?: emptySet()
    }

    suspend fun setSplitTunnelingApps(apps: Set<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SPLIT_TUNNELING_APPS] = apps
        }
    }

    val splitTunnelingIps: Flow<Set<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.SPLIT_TUNNELING_IPS] ?: emptySet()
    }

    suspend fun setSplitTunnelingIps(ips: Set<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SPLIT_TUNNELING_IPS] = ips
        }
    }
}
