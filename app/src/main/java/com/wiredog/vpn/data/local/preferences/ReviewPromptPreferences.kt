package com.wiredog.vpn.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reviewPromptDataStore: DataStore<Preferences> by preferencesDataStore(name = "review_prompt")

/**
 * Tracks eligibility for the "Enjoying WireDog VPN?" pre-prompt shown before requesting a
 * Play Store in-app review. Only users who tap 👍 ever see the native review flow; 👎 routes
 * to Report an Issue instead and the prompt can still be re-asked later.
 */
@Singleton
class ReviewPromptPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CONNECTION_COUNT = intPreferencesKey("connection_count")
        val ASK_COUNT = intPreferencesKey("ask_count")
        val LAST_SHOWN_AT = longPreferencesKey("last_shown_at")
        val RESPONDED_POSITIVELY = booleanPreferencesKey("responded_positively")
    }

    companion object {
        private const val REQUIRED_SUCCESSFUL_CONNECTIONS = 3
        private const val MAX_LIFETIME_ASKS = 3
        private const val COOLDOWN_MS = 90L * 24 * 60 * 60 * 1000
    }

    /**
     * Call every time a connection succeeds. Returns true if this is the Nth successful
     * connection and the pre-prompt should be shown now.
     */
    suspend fun recordSuccessfulConnection(): Boolean {
        val prefs = context.reviewPromptDataStore.data.first()

        if (prefs[Keys.RESPONDED_POSITIVELY] == true) return false

        val askCount = prefs[Keys.ASK_COUNT] ?: 0
        if (askCount >= MAX_LIFETIME_ASKS) return false

        val lastShownAt = prefs[Keys.LAST_SHOWN_AT] ?: 0L
        if (lastShownAt != 0L && System.currentTimeMillis() - lastShownAt < COOLDOWN_MS) return false

        val count = (prefs[Keys.CONNECTION_COUNT] ?: 0) + 1
        if (count < REQUIRED_SUCCESSFUL_CONNECTIONS) {
            context.reviewPromptDataStore.edit { it[Keys.CONNECTION_COUNT] = count }
            return false
        }

        context.reviewPromptDataStore.edit {
            it[Keys.CONNECTION_COUNT] = 0
            it[Keys.ASK_COUNT] = askCount + 1
            it[Keys.LAST_SHOWN_AT] = System.currentTimeMillis()
        }
        return true
    }

    /** Call when the user taps 👍. Suppresses the pre-prompt permanently. */
    suspend fun recordPositiveResponse() {
        context.reviewPromptDataStore.edit { it[Keys.RESPONDED_POSITIVELY] = true }
    }
}
