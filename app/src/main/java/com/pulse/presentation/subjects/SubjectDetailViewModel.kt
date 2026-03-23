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
    val error: String? = null,
    val drivePdfs: List<com.pulse.data.services.btr.BtrFile> = emptyList(),
    val folderPdf: com.pulse.data.services.btr.BtrFile? = null
)

class SubjectDetailViewModel(
    private val btrAuthManager: IBtrAuthManager,
    private val lectureRepository: LectureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentSubject = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val lectures: StateFlow<List<Lecture>> = _currentSubject
        .filter { it.isNotEmpty() }
        .flatMapLatest { subject ->
            lectureRepository.getLecturesBySubject(subject).combine(_searchQuery) { list, query ->
                val sorted = list.sortedBy { it.name }
                if (query.isBlank()) sorted else sorted.filter { it.name.contains(query, ignoreCase = true) }
            }
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

            lectureRepository.syncSubjectFolder(folderId, subjectName)
                .onSuccess {
                    _uiState.update { state -> state.copy(isLoading = false) }
                }
                .onError { error, message ->
                    _uiState.update { state -> state.copy(isLoading = false, error = "Error loading content: $message") }
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

    fun markAsCompleted(lectureId: String) {
        viewModelScope.launch {
            lectureRepository.markAsCompleted(lectureId)
        }
    }

    fun startDownload(lecture: Lecture) {
        lectureRepository.startDownload(lecture)
    }

    fun loadDrivePdfs(folderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val pdfs = lectureRepository.listPdfs(folderId)
            _uiState.update { it.copy(isLoading = false, drivePdfs = pdfs, folderPdf = pdfs.firstOrNull()) }
        }
    }

    fun addDrivePdf(pdf: com.pulse.data.services.btr.BtrFile, subjectName: String) {
        viewModelScope.launch {
            val lectureId = pdf.id
            lectureRepository.addDriveLecture(
                id = lectureId,
                name = pdf.name,
                subject = subjectName,
                topic = subjectName,
                videoId = null,
                pdfId = pdf.id
            )
            // Trigger download
            val lecture = lectureRepository.getLectureById(lectureId).first()
            if (lecture != null) {
                lectureRepository.downloadLecturePdf(lecture)
            }
        }
    }
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
