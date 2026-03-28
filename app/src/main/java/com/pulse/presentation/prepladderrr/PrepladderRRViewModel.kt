package com.pulse.presentation.prepladderrr

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.core.data.db.Lecture
import com.pulse.data.repository.LectureRepository
import com.pulse.data.services.btr.BtrFile
import com.pulse.core.domain.util.Constants
import com.pulse.core.domain.util.onError
import com.pulse.core.domain.util.onSuccess
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DriveFolder(val id: String, val name: String)

data class PrepladderRRUiState(
    val isLoadingFolders: Boolean = false,
    val isLoadingVideos: Boolean = false,
    val error: String? = null,
    val subfolders: List<DriveFolder> = emptyList(),
    val drivePdfs: List<com.pulse.data.services.btr.BtrFile> = emptyList(),
    val folderPdf: com.pulse.data.services.btr.BtrFile? = null,
    val currentFolder: DriveFolder? = null
)

class PrepladderRRViewModel(
    private val lectureRepository: LectureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrepladderRRUiState())
    val uiState: StateFlow<PrepladderRRUiState> = _uiState.asStateFlow()

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

    init {
        loadSubfolders(forceSync = false)
    }

    private var subfoldersJob: kotlinx.coroutines.Job? = null

    fun loadSubfolders(forceSync: Boolean = true) {
        subfoldersJob?.cancel()
        subfoldersJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFolders = true, error = null) }
            try {
                lectureRepository.observeSubfolders(Constants.PREPLADDER_RR_FOLDER_ID, forceSync).collect { folders ->
                    val driveFolders = folders
                        .sortedBy { it.name }
                        .map { DriveFolder(id = it.id, name = it.name) }
                    Log.d("PrepladderRRVM", "Loaded ${driveFolders.size} subfolders")
                    _uiState.update { it.copy(isLoadingFolders = false, subfolders = driveFolders) }
                }
            } catch (e: Exception) {
                Log.e("PrepladderRRVM", "Failed to load subfolders", e)
                _uiState.update { it.copy(isLoadingFolders = false, error = "Failed to load folders: ${e.message}") }
            }
        }
    }

    fun openFolder(folder: DriveFolder) {
        _uiState.update { it.copy(currentFolder = folder, isLoadingVideos = false, error = null) }
        // Use folder name as the subject tag for DB queries (e.g. "RR_MEDICINE")
        val subjectTag = "RR_${folder.name}"
        _currentSubject.value = subjectTag

        viewModelScope.launch {
            // Unobtrusive sync, if network brings new items, StateFlow updates the UI automatically
            lectureRepository.syncSubjectFolder(folder.id, subjectTag)
                .onError { _, message ->
                    _uiState.update { state -> state.copy(error = "Sync error: $message") }
                }
        }
    }

    fun goBack() {
        _uiState.update { it.copy(currentFolder = null, error = null) }
        _currentSubject.value = ""
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

    private var pdfsJob: kotlinx.coroutines.Job? = null

    fun loadDrivePdfs(folderId: String) {
        pdfsJob?.cancel()
        pdfsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVideos = true) }
            try {
                lectureRepository.observePdfs(folderId, forceSync = true).collect { pdfs ->
                    _uiState.update { it.copy(isLoadingVideos = false, drivePdfs = pdfs, folderPdf = pdfs.firstOrNull()) }
                }
            } catch (e: Exception) {
                Log.e("PrepladderRRVM", "Failed to load PDFs", e)
                _uiState.update { it.copy(isLoadingVideos = false) }
            }
        }
    }

    fun addDrivePdf(pdf: com.pulse.data.services.btr.BtrFile) {
        viewModelScope.launch {
            val lectureId = pdf.id
            lectureRepository.addDriveLecture(
                id = lectureId,
                name = pdf.name,
                subject = "RR_${uiState.value.currentFolder?.name ?: "GENERAL"}",
                topic = uiState.value.currentFolder?.name ?: "GENERAL",
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
