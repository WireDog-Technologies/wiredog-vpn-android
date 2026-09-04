package com.wiredog.vpn.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.announcementDataStore: DataStore<Preferences> by preferencesDataStore(name = "announcements")

/**
 * Local record of which announcement ids the user has read. Not sensitive — plain
 * DataStore, mirroring the iOS client's `UserDefaults`-backed `BroadcastReadStore`.
 */
@Singleton
class AnnouncementPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val readIdsKey = stringSetPreferencesKey("read_message_ids")

    val readIds: Flow<Set<String>> =
        context.announcementDataStore.data.map { it[readIdsKey] ?: emptySet() }

    suspend fun setReadIds(ids: Set<String>) {
        context.announcementDataStore.edit { it[readIdsKey] = ids }
    }
}
