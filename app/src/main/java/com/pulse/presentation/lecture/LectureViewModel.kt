package com.pulse.presentation.lecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
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
    private var errorListener: Player.Listener? = null

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
        val uri = android.net.Uri.parse(path)
        
        // Holistic check for URI accessibility before trying to play
        val isAccessible = if (path.startsWith("content://")) {
            try {
                val context = playerProvider.getContext() // Need to expose context or check via repository
                context.contentResolver.openInputStream(uri)?.close()
                true
            } catch (e: Exception) {
                logger.e("LectureVM", "URI not accessible: $path")
                false
            }
        } else true

        if (!isAccessible && path.startsWith("content://")) {
            // Future: Post error state to UI
            return
        }

        playerProvider.prepare(path, _lecture.value?.name ?: "Lecture")
        if (position > 0) {
            player.seekTo(position)
        }
        
        // Ensure playWhenReady is true for local files
        player.playWhenReady = true
        
        // Remove previous listener to prevent leaks on multiple prepares
        errorListener?.let { player.removeListener(it) }

        errorListener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                logger.e("LectureViewModel", "Playback Error: ${error.errorCodeName} - ${error.message} for path: $path")
                
                // If the error is network/HTTP related (usually URL expiration for Google Drive streams)
                val isNetworkError = error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED || 
                                     error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                
                if (isNetworkError && !path.startsWith("content://") && !path.startsWith("file://") && !path.startsWith("/")) {
                    logger.d("LectureViewModel", "Attempting to refresh stream URL due to network error...")
                    val currentPos = player.currentPosition
                    
                    viewModelScope.launch {
                        _lecture.value?.videoId?.let { vId ->
                            val freshUrl = getLectureStreamUrlUseCase(vId, true)
                            if (freshUrl != null && freshUrl != path) {
                                logger.d("LectureViewModel", "Found fresh URL, retrying playback.")
                                // Remove listener so we don't trigger again for the old path
                                errorListener?.let { player.removeListener(it) } 
                                preparePlayer(freshUrl, currentPos)
                                lastPreparedPath = freshUrl
                            }
                        }
                    }
                } else {
                    // Force a re-emit if local failed, to try fallback logic
                    lastPreparedPath = null
                }
            }
        }
        player.addListener(errorListener!!)
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
        errorListener?.let { player.removeListener(it) }
        errorListener = null
        playerProvider.stop()
        _lecture.value = null
        lastPreparedPath = null
    }

    override fun onCleared() {
        cleanup()
        super.onCleared()
    }
}
