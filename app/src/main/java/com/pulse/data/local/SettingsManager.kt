package com.pulse.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val SPLIT_RATIO_KEY = floatPreferencesKey("split_ratio")
        val AUTO_PIP_ENABLED_KEY = booleanPreferencesKey("auto_pip_enabled")
        val RESUME_PLAYBACK_KEY = booleanPreferencesKey("resume_playback")
        val BACKGROUND_PLAYBACK_KEY = booleanPreferencesKey("background_playback")
        val DEFAULT_SPEED_KEY = floatPreferencesKey("default_speed")
        val VIDEO_QUALITY_KEY = stringPreferencesKey("video_quality")
        val PDF_HORIZONTAL_ORIENTATION_KEY = booleanPreferencesKey("pdf_horizontal_orientation")
        val LAST_SYNC_TIME_KEY = longPreferencesKey("last_sync_time")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val CLOUD_SYNC_ENABLED_KEY = booleanPreferencesKey("cloud_sync_enabled")
        val DOUBLE_TAP_SEEK_OFFSET_KEY = longPreferencesKey("double_tap_seek_offset")
        val LONG_PRESS_SPEED_KEY = floatPreferencesKey("long_press_speed")
    }

    val splitRatioFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[SPLIT_RATIO_KEY] ?: 0.55f
        }

    val autoPipEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_PIP_ENABLED_KEY] ?: true }
    val resumePlaybackFlow: Flow<Boolean> = context.dataStore.data.map { it[RESUME_PLAYBACK_KEY] ?: true }
    val backgroundPlaybackFlow: Flow<Boolean> = context.dataStore.data.map { it[BACKGROUND_PLAYBACK_KEY] ?: true }
    val defaultSpeedFlow: Flow<Float> = context.dataStore.data.map { it[DEFAULT_SPEED_KEY] ?: 1.0f }
    val videoQualityFlow: Flow<String> = context.dataStore.data.map { it[VIDEO_QUALITY_KEY] ?: "Auto" }
    val pdfHorizontalOrientationFlow: Flow<Boolean> = context.dataStore.data.map { it[PDF_HORIZONTAL_ORIENTATION_KEY] ?: false }
    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { it[LAST_SYNC_TIME_KEY] ?: 0L }
    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[THEME_MODE_KEY] ?: "SYSTEM" }
    val cloudSyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[CLOUD_SYNC_ENABLED_KEY] ?: true }
    val doubleTapSeekOffsetFlow: Flow<Long> = context.dataStore.data.map { it[DOUBLE_TAP_SEEK_OFFSET_KEY] ?: 10000L }
    val longPressSpeedFlow: Flow<Float> = context.dataStore.data.map { it[LONG_PRESS_SPEED_KEY] ?: 2.0f }

    suspend fun saveSplitRatio(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPLIT_RATIO_KEY] = value
        }
    }

    suspend fun saveAutoPipEnabled(value: Boolean) { context.dataStore.edit { it[AUTO_PIP_ENABLED_KEY] = value } }
    suspend fun saveResumePlayback(value: Boolean) { context.dataStore.edit { it[RESUME_PLAYBACK_KEY] = value } }
    suspend fun saveBackgroundPlayback(value: Boolean) { context.dataStore.edit { it[BACKGROUND_PLAYBACK_KEY] = value } }
    suspend fun saveDefaultSpeed(value: Float) { context.dataStore.edit { it[DEFAULT_SPEED_KEY] = value } }
    suspend fun saveVideoQuality(value: String) { context.dataStore.edit { it[VIDEO_QUALITY_KEY] = value } }
    suspend fun savePdfHorizontalOrientation(value: Boolean) { context.dataStore.edit { it[PDF_HORIZONTAL_ORIENTATION_KEY] = value } }
    suspend fun saveLastSyncTime(value: Long) { context.dataStore.edit { it[LAST_SYNC_TIME_KEY] = value } }
    suspend fun saveThemeMode(value: String) { context.dataStore.edit { it[THEME_MODE_KEY] = value } }
    suspend fun saveCloudSyncEnabled(value: Boolean) { context.dataStore.edit { it[CLOUD_SYNC_ENABLED_KEY] = value } }
    suspend fun saveDoubleTapSeekOffset(value: Long) { context.dataStore.edit { it[DOUBLE_TAP_SEEK_OFFSET_KEY] = value } }
    suspend fun saveLongPressSpeed(value: Float) { context.dataStore.edit { it[LONG_PRESS_SPEED_KEY] = value } }
}
