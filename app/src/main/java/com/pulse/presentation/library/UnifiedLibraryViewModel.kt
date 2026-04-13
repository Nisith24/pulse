package com.pulse.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.core.data.db.Lecture
import com.pulse.data.repository.LectureRepository
import com.pulse.domain.usecase.ManifestSyncUseCase
import com.pulse.domain.usecase.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UnifiedLibraryViewModel(
    private val repository: LectureRepository,
    private val manifestSyncUseCase: ManifestSyncUseCase
) : ViewModel() {

    val syncState: StateFlow<SyncState> = manifestSyncUseCase.syncState

    init {
        // Trigger initial sync on ViewModel creation
        viewModelScope.launch {
            manifestSyncUseCase()
        }
    }

    fun syncManifest() {
        viewModelScope.launch {
            manifestSyncUseCase()
        }
    }

    fun lecturesByCategory(category: String): StateFlow<List<Lecture>> =
        repository.getLecturesByCategory(category)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun lecturesBySubject(subject: String): StateFlow<List<Lecture>> =
        repository.getLecturesBySubject(subject)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun searchResults(query: String): StateFlow<List<Lecture>> =
        repository.searchManifestLectures(query)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val recentlyWatched: StateFlow<List<Lecture>> =
        repository.recentlyWatchedLectures
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val favorites: StateFlow<List<Lecture>> =
        repository.favoriteLectures
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}
