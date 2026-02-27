package com.pulse.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val SPLIT_RATIO_KEY = floatPreferencesKey("split_ratio")
    }

    val splitRatioFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[SPLIT_RATIO_KEY] ?: 0.55f
        }

    suspend fun saveSplitRatio(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPLIT_RATIO_KEY] = value
        }
    }
}
