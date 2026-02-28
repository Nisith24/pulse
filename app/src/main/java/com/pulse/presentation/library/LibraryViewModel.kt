package com.pulse.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.core.data.db.Lecture
import com.pulse.data.repository.LectureRepository
import com.pulse.data.local.FileStorageManager
import com.pulse.core.domain.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab {
    HOME, SERVICES
}

class LibraryViewModel(
    private val repository: LectureRepository,
    private val fileStorage: FileStorageManager,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    val connectivityStatus: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)


    private val _currentTab = MutableStateFlow(LibraryTab.HOME)
    val currentTab = _currentTab.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _authIntent = MutableStateFlow<android.content.Intent?>(null)
    val authIntent = _authIntent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isBtrViewActive = MutableStateFlow(false)
    val isBtrViewActive = _isBtrViewActive.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly = _showFavoritesOnly.asStateFlow()

    private fun List<com.pulse.core.data.db.Lecture>.filterAndSort(query: String, favOnly: Boolean): List<com.pulse.core.data.db.Lecture> {
        val filtered = (if (query.isBlank()) this
        else this.filter { it.name.contains(query, ignoreCase = true) })
            .filter { !favOnly || it.isFavorite }

        return filtered.sortedWith(compareBy<com.pulse.core.data.db.Lecture> { 
            // Extract the first sequence of digits as a number for sorting
            Regex("\\d+").find(it.name)?.value?.toIntOrNull() ?: Int.MAX_VALUE 
        }.thenBy { it.name })
    }

    val btrLectures = combine(repository.btrLectures, _searchQuery, _showFavoritesOnly) { lectures, query, favOnly ->
        lectures.filterAndSort(query, favOnly)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val localLectures = combine(repository.localLectures, _searchQuery, _showFavoritesOnly) { lectures, query, favOnly ->
        lectures.filterAndSort(query, favOnly)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTab(tab: LibraryTab) {
        _currentTab.value = tab
        // Reset sub-view when switching tabs
        if (tab != LibraryTab.SERVICES) {
            _isBtrViewActive.value = false
        }
    }

    fun setBtrViewActive(active: Boolean) {
        _isBtrViewActive.value = active
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun toggleFavorite(lectureId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(lectureId)
        }
    }


    fun syncBtr() {
        viewModelScope.launch {
            _error.value = null
            _authIntent.value = null
            _isLoading.value = true
            try {
                repository.sync()
            } catch (e: com.pulse.data.services.btr.PulseAuthException) {
                when (e) {
                    is com.pulse.data.services.btr.PulseAuthException.PermissionRequired -> {
                        _error.value = "Cloud access requires permission."
                        _authIntent.value = e.intent
                    }
                    is com.pulse.data.services.btr.PulseAuthException.UserNotSignedIn -> {
                        _error.value = "Please sign in to your Cloud account."
                    }
                    is com.pulse.data.services.btr.PulseAuthException.Fatal -> {
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
