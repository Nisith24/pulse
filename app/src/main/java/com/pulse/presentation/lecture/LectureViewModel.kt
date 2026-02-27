package com.pulse.presentation.lecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.pulse.data.db.Lecture
import com.pulse.data.db.Note
import com.pulse.data.repository.LectureRepository
import com.pulse.domain.repository.INoteRepository
import com.pulse.domain.usecase.GetLectureStreamUrlUseCase
import com.pulse.domain.util.ILogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LectureViewModel(
    private val lectureId: String,
    private val repository: LectureRepository,
    private val noteRepository: INoteRepository,
    private val playerProvider: PlayerProvider,
    private val getLectureStreamUrlUseCase: GetLectureStreamUrlUseCase,
    private val logger: ILogger
) : ViewModel() {

    private val _lecture = MutableStateFlow<Lecture?>(null)
    val lecture: StateFlow<Lecture?> = _lecture.asStateFlow()
    
    val notes: Flow<List<Note>> = noteRepository.getNotes(lectureId)
    
    val player = playerProvider.player

    private var periodicSaveJob: Job? = null
    private var lastPreparedPath: String? = null

    init {
        // Reactive Metadata & Player Preparation
        viewModelScope.launch {
            repository.getLectureById(lectureId).collect { l ->
                _lecture.value = l
                l?.let { 
                    val currentPath = it.videoLocalPath ?: getLectureStreamUrlUseCase(it.videoId)
                    if (currentPath != null && currentPath != lastPreparedPath) {
                        preparePlayer(currentPath, it.lastPosition)
                        lastPreparedPath = currentPath
                    }
                }
            }
        }
        startPeriodicSave()
    }

    private fun preparePlayer(path: String, position: Long) {
        playerProvider.prepare(path)
        if (position > 0) {
            player.seekTo(position)
        }
        
        // Ensure playWhenReady is true for local files
        player.playWhenReady = true
        
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                logger.e("LectureViewModel", "Playback Error: ${error.message} for path: $path")
                // If local fails, try to fallback to stream if available
                lastPreparedPath = null 
            }
        }
        player.addListener(listener)
    }

    private fun startPeriodicSave() {
        periodicSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                saveProgress()
            }
        }
    }
    
    fun saveProgress() {
        val currentLecture = _lecture.value ?: return
        val pos = player.currentPosition
        val dur = player.duration
        if (dur <= 0) return // Don't save if player isn't ready
        
        viewModelScope.launch {
            repository.updateLectureProgress(currentLecture, pos, dur)
        }
    }

    fun addNote(text: String) {
        viewModelScope.launch {
            noteRepository.insertNote(
                Note(
                    lectureId = lectureId,
                    timestamp = player.currentPosition,
                    text = text
                )
            )
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNote(noteId)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _lecture.value?.let { l ->
            viewModelScope.launch {
                repository.updateLectureZoom(l.copy(speed = speed), l.lastPage) 
            }
        }
    }

    fun seekTo(timestamp: Long) {
        player.seekTo(timestamp)
    }

    fun updatePdfPage(page: Int) {
        _lecture.value?.let { l ->
            if (l.lastPage == page) return@let
            viewModelScope.launch {
                repository.updateLectureZoom(l, page)
            }
        }
    }

    fun updateLocalPdfPath(path: String) {
        _lecture.value?.let { l ->
            viewModelScope.launch {
                repository.updateLectureZoom(l.copy(pdfLocalPath = path), l.lastPage)
            }
        }
    }

    fun cleanup() {
        periodicSaveJob?.cancel()
        saveProgress()
        playerProvider.stop()
        _lecture.value = null
        lastPreparedPath = null
    }

    override fun onCleared() {
        cleanup()
        super.onCleared()
    }
}
