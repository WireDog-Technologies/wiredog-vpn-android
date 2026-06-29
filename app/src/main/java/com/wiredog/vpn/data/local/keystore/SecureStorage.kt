package com.wiredog.vpn.data.local.keystore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val securePrefs = createPrefs()

    private fun createPrefs() = try {
        buildEncryptedPrefs()
    } catch (_: Exception) {
        // Keystore is corrupted — wipe and recreate so the app stays functional
        File(context.filesDir.parent, "shared_prefs/$PREFS_FILE_NAME.xml").delete()
        buildEncryptedPrefs()
    }

    private fun buildEncryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthToken(token: String) {
        securePrefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return securePrefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun deleteAuthToken() {
        securePrefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    fun hasAuthToken(): Boolean {
        return getAuthToken() != null
    }

    fun saveLoginIdentifier(identifier: String) {
        securePrefs.edit().putString(KEY_LOGIN_IDENTIFIER, identifier).apply()
    }

    fun getLoginIdentifier(): String? {
        return securePrefs.getString(KEY_LOGIN_IDENTIFIER, null)
    }

    fun saveSessionId(sessionId: String) {
        securePrefs.edit().putString(KEY_SESSION_ID, sessionId).apply()
    }

    fun getSessionId(): String? {
        return securePrefs.getString(KEY_SESSION_ID, null)
    }

    fun clearSessionId() {
        securePrefs.edit().remove(KEY_SESSION_ID).apply()
    }

    fun clearAll() {
        securePrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "wiredog_secure_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_LOGIN_IDENTIFIER = "login_identifier"
        private const val KEY_SESSION_ID = "vpn_session_id"
    }
}
