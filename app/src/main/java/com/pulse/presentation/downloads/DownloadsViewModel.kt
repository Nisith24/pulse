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

    val downloadedLectures: StateFlow<List<Lecture>> = repository.downloadedLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudOnlyLectures: StateFlow<List<Lecture>> = repository.cloudOnlyLectures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads = repository.activeDownloads

    fun startDownload(lecture: Lecture) {
        repository.startDownload(lecture)
    }

    fun cancelDownload(lectureId: String) {
        repository.cancelDownload(lectureId)
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
