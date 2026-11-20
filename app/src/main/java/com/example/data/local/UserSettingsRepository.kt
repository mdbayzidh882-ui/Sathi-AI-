package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.OfflinePackState
import com.example.model.OfflinePackVariant
import com.example.model.PlanTier
import com.example.model.SathiVoice
import com.example.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sathiDataStore: DataStore<Preferences> by preferencesDataStore(name = "sathi_prefs")

class UserSettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val VOICE = stringPreferencesKey("voice")
        val PLAN_TIER = stringPreferencesKey("plan_tier")
        val ACTIVE_CODE_LABEL = stringPreferencesKey("active_code_label")
        val FAILED_CODE_ATTEMPTS = intPreferencesKey("failed_code_attempts")
        val CODE_LOCKOUT_UNTIL = longPreferencesKey("code_lockout_until")
        val OFFLINE_PACK_STATE = stringPreferencesKey("offline_pack_state")
        val OFFLINE_PACK_VARIANT = stringPreferencesKey("offline_pack_variant")
        val OFFLINE_PROGRESS = intPreferencesKey("offline_progress")
        val OFFLINE_WIFI_ONLY = booleanPreferencesKey("offline_wifi_only")
        val OFFLINE_MODE_FORCED = booleanPreferencesKey("offline_mode_forced")
        val USER_NAME = stringPreferencesKey("user_name")
        val SPEECH_SPEED = androidx.datastore.preferences.core.floatPreferencesKey("speech_speed")
        val LOUD_CINEMA_VOICE = booleanPreferencesKey("loud_cinema_voice")
    }

    val settingsFlow: Flow<UserSettings> = context.sathiDataStore.data.map { prefs ->
        UserSettings(
            language = enumValues<AppLanguage>().firstOrNull {
                it.name == prefs[Keys.LANGUAGE]
            } ?: AppLanguage.BENGALI,
            voice = enumValues<SathiVoice>().firstOrNull {
                it.name == prefs[Keys.VOICE]
            } ?: SathiVoice.FEMALE_MISTY,
            planTier = enumValues<PlanTier>().firstOrNull {
                it.name == prefs[Keys.PLAN_TIER]
            } ?: PlanTier.FREE,
            activeCodeLabel = prefs[Keys.ACTIVE_CODE_LABEL] ?: "",
            failedCodeAttempts = prefs[Keys.FAILED_CODE_ATTEMPTS] ?: 0,
            codeLockoutUntilMillis = prefs[Keys.CODE_LOCKOUT_UNTIL] ?: 0L,
            offlinePackState = enumValues<OfflinePackState>().firstOrNull {
                it.name == prefs[Keys.OFFLINE_PACK_STATE]
            } ?: OfflinePackState.NOT_DOWNLOADED,
            offlinePackVariant = enumValues<OfflinePackVariant>().firstOrNull {
                it.name == prefs[Keys.OFFLINE_PACK_VARIANT]
            } ?: OfflinePackVariant.STANDARD_1_5_GB,
            offlineDownloadProgress = prefs[Keys.OFFLINE_PROGRESS] ?: 0,
            offlineWifiOnly = prefs[Keys.OFFLINE_WIFI_ONLY] ?: true,
            offlineModeForced = prefs[Keys.OFFLINE_MODE_FORCED] ?: false,
            userName = prefs[Keys.USER_NAME] ?: "",
            speechSpeed = (prefs[Keys.SPEECH_SPEED] ?: 1.12f).coerceIn(0.85f, 1.50f),
            loudCinemaVoice = prefs[Keys.LOUD_CINEMA_VOICE] ?: true
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.sathiDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setVoice(voice: SathiVoice) {
        context.sathiDataStore.edit { it[Keys.VOICE] = voice.name }
    }

    suspend fun setPlanTier(tier: PlanTier, codeLabel: String) {
        context.sathiDataStore.edit {
            it[Keys.PLAN_TIER] = tier.name
            it[Keys.ACTIVE_CODE_LABEL] = codeLabel
            it[Keys.FAILED_CODE_ATTEMPTS] = 0
            it[Keys.CODE_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun recordFailedCodeAttempt(attempts: Int, lockoutUntilMillis: Long) {
        context.sathiDataStore.edit {
            it[Keys.FAILED_CODE_ATTEMPTS] = attempts
            it[Keys.CODE_LOCKOUT_UNTIL] = lockoutUntilMillis
        }
    }

    suspend fun updateOfflinePack(
        state: OfflinePackState,
        progress: Int,
        variant: OfflinePackVariant? = null
    ) {
        context.sathiDataStore.edit {
            it[Keys.OFFLINE_PACK_STATE] = state.name
            it[Keys.OFFLINE_PROGRESS] = progress.coerceIn(0, 100)
            if (variant != null) {
                it[Keys.OFFLINE_PACK_VARIANT] = variant.name
            }
            if (state == OfflinePackState.NOT_DOWNLOADED) {
                it[Keys.OFFLINE_MODE_FORCED] = false
            }
        }
    }

    suspend fun setOfflineWifiOnly(wifiOnly: Boolean) {
        context.sathiDataStore.edit { it[Keys.OFFLINE_WIFI_ONLY] = wifiOnly }
    }

    suspend fun setOfflineModeForced(forced: Boolean) {
        context.sathiDataStore.edit { it[Keys.OFFLINE_MODE_FORCED] = forced }
    }

    suspend fun setUserName(name: String) {
        context.sathiDataStore.edit { it[Keys.USER_NAME] = name }
    }

    suspend fun setSpeechSpeed(speed: Float) {
        context.sathiDataStore.edit { it[Keys.SPEECH_SPEED] = speed.coerceIn(0.85f, 1.50f) }
    }

    suspend fun setLoudCinemaVoice(enabled: Boolean) {
        context.sathiDataStore.edit { it[Keys.LOUD_CINEMA_VOICE] = enabled }
    }

    suspend fun clearAllUserPreferences() {
        context.sathiDataStore.edit {
            it.remove(Keys.USER_NAME)
            it[Keys.PLAN_TIER] = PlanTier.FREE.name
            it[Keys.ACTIVE_CODE_LABEL] = ""
            it[Keys.FAILED_CODE_ATTEMPTS] = 0
            it[Keys.CODE_LOCKOUT_UNTIL] = 0L
        }
    }
}
