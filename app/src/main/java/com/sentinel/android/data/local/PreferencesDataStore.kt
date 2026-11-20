package com.sentinel.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sentinel.android.core.common.AppLanguage
import com.sentinel.android.core.common.ThemeMode
import com.sentinel.android.core.security.SentinelKeystoreCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sentinelDataStore by preferencesDataStore(name = "sentinel_preferences")

data class SentinelPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val biometricLockEnabled: Boolean = false,
    val sessionTimeoutMinutes: Int = 30,
    val lastAuthTimestampMillis: Long = 0L,
    val encryptedSessionNotePresent: Boolean = false
)

class PreferencesRepository(
    private val context: Context,
    private val crypto: SentinelKeystoreCrypto
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val SESSION_TIMEOUT = intPreferencesKey("session_timeout_minutes")
        val LAST_AUTH_TS = longPreferencesKey("last_auth_ts")
        val ENCRYPTED_DEVICE_TAG = stringPreferencesKey("encrypted_device_tag")
    }

    val preferencesFlow: Flow<SentinelPreferences> = context.sentinelDataStore.data.map { prefs ->
        val theme = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME_MODE] } ?: ThemeMode.DARK
        val lang = AppLanguage.entries.firstOrNull { it.name == prefs[Keys.LANGUAGE] } ?: AppLanguage.ENGLISH
        SentinelPreferences(
            themeMode = theme,
            language = lang,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            sessionTimeoutMinutes = prefs[Keys.SESSION_TIMEOUT] ?: 30,
            lastAuthTimestampMillis = prefs[Keys.LAST_AUTH_TS] ?: 0L,
            encryptedSessionNotePresent = !prefs[Keys.ENCRYPTED_DEVICE_TAG].isNullOrBlank()
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.sentinelDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.sentinelDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.sentinelDataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setSessionTimeoutMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 1440)
        context.sentinelDataStore.edit { it[Keys.SESSION_TIMEOUT] = clamped }
    }

    suspend fun recordAuthenticatedTimestamp(uid: String) {
        val encryptedTag = runCatching { crypto.encrypt("sentinel-session:$uid") }.getOrDefault("")
        context.sentinelDataStore.edit {
            it[Keys.LAST_AUTH_TS] = System.currentTimeMillis()
            if (encryptedTag.isNotEmpty()) {
                it[Keys.ENCRYPTED_DEVICE_TAG] = encryptedTag
            }
        }
    }

    suspend fun clearSessionVault() {
        context.sentinelDataStore.edit {
            it.remove(Keys.LAST_AUTH_TS)
            it.remove(Keys.ENCRYPTED_DEVICE_TAG)
        }
    }
}
