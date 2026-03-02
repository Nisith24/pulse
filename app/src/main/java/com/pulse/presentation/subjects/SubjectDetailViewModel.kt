package com.pulse.presentation.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.core.data.db.Lecture
import com.pulse.data.repository.LectureRepository
import com.pulse.domain.services.btr.IBtrAuthManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.pulse.core.domain.util.onError
import com.pulse.core.domain.util.onSuccess

data class SubjectDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class SubjectDetailViewModel(
    private val btrAuthManager: IBtrAuthManager,
    private val lectureRepository: LectureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    private val _currentSubject = MutableStateFlow("")

    val lectures: StateFlow<List<Lecture>> = _currentSubject
        .filter { it.isNotEmpty() }
        .flatMapLatest { subject ->
            lectureRepository.getLecturesBySubject(subject)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadFolder(folderId: String, subjectName: String) {
        _currentSubject.value = subjectName
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (!btrAuthManager.isSignedIn) {
                _uiState.update { it.copy(isLoading = false, error = "Sign in required to load content.") }
                return@launch
            }
            
            lectureRepository.syncSubjectFolder(folderId, subjectName)
                .onSuccess {
                    _uiState.update { state -> state.copy(isLoading = false) }
                }
                .onError { error, message ->
                    _uiState.update { state -> state.copy(isLoading = false, error = "Error loading folder: $message") }
                }
        }
    }

    fun toggleFavorite(lectureId: String) {
        viewModelScope.launch {
            lectureRepository.toggleFavorite(lectureId)
        }
    }

    fun deleteLecture(lecture: Lecture) {
        viewModelScope.launch {
            lectureRepository.deleteLecture(lecture.id)
            if (lecture.videoLocalPath?.isNotEmpty() == true) {
                lectureRepository.deleteOfflineVideo(lecture.id)
            }
        }
    }
}
