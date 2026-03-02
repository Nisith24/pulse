package com.pulse.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.data.local.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

class ThemeViewModel(private val settingsManager: SettingsManager) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settingsManager.themeModeFlow.map {
        try { ThemeMode.valueOf(it) } catch (e: Exception) { ThemeMode.SYSTEM }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            settingsManager.saveThemeMode(mode.name)
        }
    }
}
