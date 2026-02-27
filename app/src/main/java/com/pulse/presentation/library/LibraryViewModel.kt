package com.pulse.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.data.repository.LectureRepository
import com.pulse.data.local.FileStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab {
    HOME, BTR
}

class LibraryViewModel(
    private val repository: LectureRepository,
    private val fileStorage: FileStorageManager
) : ViewModel() {

    private val _currentTab = MutableStateFlow(LibraryTab.HOME)
    val currentTab = _currentTab.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _authIntent = MutableStateFlow<android.content.Intent?>(null)
    val authIntent = _authIntent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val driveLectures = repository.driveLectures.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val localLectures = repository.localLectures.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setTab(tab: LibraryTab) {
        _currentTab.value = tab
    }

    fun syncDrive() {
        viewModelScope.launch {
            _error.value = null
            _authIntent.value = null
            _isLoading.value = true
            try {
                repository.sync()
            } catch (e: com.pulse.data.drive.PulseAuthException) {
                when (e) {
                    is com.pulse.data.drive.PulseAuthException.PermissionRequired -> {
                        _error.value = "Google Drive access requires permission."
                        _authIntent.value = e.intent
                    }
                    is com.pulse.data.drive.PulseAuthException.UserNotSignedIn -> {
                        _error.value = "Please sign in to Google Drive."
                    }
                    is com.pulse.data.drive.PulseAuthException.Fatal -> {
                        _error.value = "Authentication error: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Sync failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addLocalLecture(name: String, videoPath: String?, pdfPath: String?) {
        viewModelScope.launch {
            repository.addLocalLecture(name, videoPath, pdfPath)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                repository.clearCache()
            } catch (e: Exception) {
                _error.value = "Failed to clear cache: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
        _authIntent.value = null
    }

    fun deleteLocalLecture(id: String) {
        viewModelScope.launch {
            repository.deleteLecture(id)
        }
    }
}
