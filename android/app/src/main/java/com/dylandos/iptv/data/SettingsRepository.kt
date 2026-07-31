package com.dylandos.iptv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Immutable snapshot of user-configurable playback settings. */
data class AppSettings(
    val timeshiftEnabled: Boolean = false,
    val timeshiftBufferMinutes: Int = 30,
    val hlsFailoverMs: Long = 4000L,
    val defaultPlayer: String = "AUTO",        // AUTO | VLC | MPV
    val preferHwDecoding: Boolean = true,
    val audioPassthrough: Boolean = false,
    val autoRecordEnabled: Boolean = false,
    val rememberPosition: Boolean = true,
    val epgRefreshHours: Int = 6,
    val showNativeBars: Boolean = false
)

/** File-scoped single instance of the settings DataStore. */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Centralized, single-instance DataStore-backed settings holder. This is the
 * Single Source of Truth for all toggles consumed by the media engines and UI.
 */
@Singleton
class SettingsRepository @Inject constructor(
    context: Context
) {
    private val store: DataStore<Preferences> = context.applicationContext.settingsDataStore

    private object Keys {
        val TIMESHIFT = booleanPreferencesKey("timeshift_enabled")
        val TIMESHIFT_MINUTES = intPreferencesKey("timeshift_buffer_minutes")
        val HLS_FAILOVER_MS = stringPreferencesKey("hls_failover_ms")
        val DEFAULT_PLAYER = stringPreferencesKey("default_player")
        val HW_DECODING = booleanPreferencesKey("prefer_hw_decoding")
        val AUDIO_PASSTHROUGH = booleanPreferencesKey("audio_passthrough")
        val AUTO_RECORD = booleanPreferencesKey("auto_record")
        val REMEMBER_POSITION = booleanPreferencesKey("remember_position")
        val EPG_REFRESH_HOURS = intPreferencesKey("epg_refresh_hours")
        val NATIVE_BARS = booleanPreferencesKey("native_bars")
    }

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            timeshiftEnabled = prefs[Keys.TIMESHIFT] ?: false,
            timeshiftBufferMinutes = prefs[Keys.TIMESHIFT_MINUTES] ?: 30,
            hlsFailoverMs = (prefs[Keys.HLS_FAILOVER_MS] ?: "4000").toLongOrNull() ?: 4000L,
            defaultPlayer = prefs[Keys.DEFAULT_PLAYER] ?: "AUTO",
            preferHwDecoding = prefs[Keys.HW_DECODING] ?: true,
            audioPassthrough = prefs[Keys.AUDIO_PASSTHROUGH] ?: false,
            autoRecordEnabled = prefs[Keys.AUTO_RECORD] ?: false,
            rememberPosition = prefs[Keys.REMEMBER_POSITION] ?: true,
            epgRefreshHours = prefs[Keys.EPG_REFRESH_HOURS] ?: 6,
            showNativeBars = prefs[Keys.NATIVE_BARS] ?: false
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setTimeshiftEnabled(enabled: Boolean) = store.edit { it[Keys.TIMESHIFT] = enabled }

    suspend fun setTimeshiftMinutes(minutes: Int) = store.edit { it[Keys.TIMESHIFT_MINUTES] = minutes }

    suspend fun setHlsFailoverMs(ms: Long) = store.edit { it[Keys.HLS_FAILOVER_MS] = ms.toString() }

    suspend fun setDefaultPlayer(player: String) = store.edit { it[Keys.DEFAULT_PLAYER] = player }

    suspend fun setHwDecoding(enabled: Boolean) = store.edit { it[Keys.HW_DECODING] = enabled }

    suspend fun setAudioPassthrough(enabled: Boolean) = store.edit { it[Keys.AUDIO_PASSTHROUGH] = enabled }

    suspend fun setAutoRecord(enabled: Boolean) = store.edit { it[Keys.AUTO_RECORD] = enabled }

    suspend fun setRememberPosition(enabled: Boolean) = store.edit { it[Keys.REMEMBER_POSITION] = enabled }

    suspend fun setEpgRefreshHours(hours: Int) = store.edit { it[Keys.EPG_REFRESH_HOURS] = hours }
}
