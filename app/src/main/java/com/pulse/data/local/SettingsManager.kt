package com.pulse.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
}
