package com.pulse.presentation.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.core.data.db.Lecture
import com.pulse.data.repository.LectureRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.pulse.data.repository.DownloadStatus

class DownloadsViewModel(
    private val repository: LectureRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val downloadedLectures: StateFlow<List<Lecture>> = combine(repository.downloadedLectures, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudOnlyLectures: StateFlow<List<Lecture>> = combine(repository.cloudOnlyLectures, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads = repository.activeDownloads

    fun startDownload(lecture: Lecture) {
        repository.startDownload(lecture)
    }

    fun cancelDownload(lectureId: String) {
        repository.cancelDownload(lectureId)
    }

    fun saveToDevice(lecture: Lecture) {
        viewModelScope.launch {
            try {
                // Convert BTR download to local: adds it to localLectures so it shows on HOME tab
                repository.addLocalLecture(lecture.name, lecture.videoLocalPath, lecture.pdfLocalPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteOffline(lectureId: String) {
        viewModelScope.launch {
            try {
                repository.deleteOfflineVideo(lectureId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadedLectures.value.forEach { lecture ->
                try { repository.deleteOfflineVideo(lecture.id) } catch (_: Exception) {}
            }
        }
    }
}
