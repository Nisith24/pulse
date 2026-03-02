package com.pulse.presentation.lecture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.pulse.core.data.db.Lecture
import com.pulse.core.data.db.Note
import com.pulse.data.repository.LectureRepository
import com.pulse.core.domain.repository.INoteRepository
import com.pulse.domain.usecase.GetLectureStreamUrlUseCase
import com.pulse.core.domain.util.ILogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.util.Log

import com.pulse.data.local.SettingsManager

import com.pulse.core.data.repository.NoteVisualRepository
import com.pulse.core.data.db.NoteVisual
import com.pulse.core.data.db.VisualType

class LectureViewModel(
    private val lectureId: String,
    private val repository: LectureRepository,
    private val noteRepository: INoteRepository,
    private val noteVisualRepository: NoteVisualRepository,
    private val playerProvider: PlayerProvider,
    private val getLectureStreamUrlUseCase: GetLectureStreamUrlUseCase,
    private val logger: ILogger,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _lecture = MutableStateFlow<Lecture?>(null)
    val lecture: StateFlow<Lecture?> = _lecture.asStateFlow()

    private val _playerState: MutableStateFlow<PlayerUiState> = MutableStateFlow(PlayerUiState.LOADING)
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    val notes: Flow<List<Note>> = noteRepository.getNotes(lectureId)
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val visuals: Flow<List<NoteVisual>> = lecture.filterNotNull()
        .map { it.pdfLocalPath.ifEmpty { it.pdfId ?: it.id } }
        .distinctUntilChanged()
        .flatMapLatest { pdfId ->
            noteVisualRepository.getVisualsForFile(lectureId, pdfId)
        }
    val player = playerProvider.player
    
    val pdfHorizontalOrientation: StateFlow<Boolean> = settingsManager.pdfHorizontalOrientationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var periodicSaveJob: Job? = null
    private var hasStartedPlayback = false

    init {
        observePlayerState()
        loadLectureAndPlay()
        startPeriodicSave()
    }

    private fun loadLectureAndPlay() {
        viewModelScope.launch {
            // Step 1: Get the lecture meta-data (blocking collection for initial data)
            repository.getLectureById(lectureId).take(1).collect { initialLecture ->
                _lecture.value = initialLecture
                if (initialLecture != null) {
                    processPlayback(initialLecture)
                } else {
                    _playerState.value = PlayerUiState.ERROR("Lecture not found")
                }
            }
            
            // Step 2: Keep metadata in sync without re-triggering playback
            repository.getLectureById(lectureId).drop(1).collect { updatedLecture ->
                _lecture.value = updatedLecture
            }
        }
    }

    fun playSessionIfNeeded() {
        val currentLecture = _lecture.value ?: return
        if (!playerProvider.isSessionActive(lectureId)) {
            viewModelScope.launch {
                hasStartedPlayback = false
                processPlayback(currentLecture)
            }
        } else {
             // Production fix: Force play if it was pending and we are ready
            if (!player.isPlaying && player.playbackState == Player.STATE_READY) {
                player.play()
            }
        }
    }

    private suspend fun processPlayback(lecture: Lecture) {
        if (hasStartedPlayback) return
        
        try {
            // Internal state is LOADING until ExoPlayer reports STATE_READY
            _playerState.value = PlayerUiState.LOADING
            
            val hasVideo = lecture.videoId != null || (lecture.videoLocalPath != null && lecture.videoLocalPath != "")
            val hasPdf = lecture.pdfId != null || (lecture.pdfLocalPath.isNotEmpty() && lecture.pdfLocalPath != "")

            if (hasVideo) {
                val result = getLectureStreamUrlUseCase(lecture.videoId)
                val url = lecture.videoLocalPath ?: result?.first
                val token = result?.second

                if (url != null) {
                    hasStartedPlayback = true
                    val shouldResume = settingsManager.resumePlaybackFlow.first()
                    val defSpeed = settingsManager.defaultSpeedFlow.first()
                    
                    val actualSeek = if (shouldResume) lecture.lastPosition else 0L
                    val actualSpeed = if (lecture.speed != 1.0f) lecture.speed else defSpeed
                    
                    Log.d("LectureViewModel", "Requesting session for ${lecture.name} at $actualSeek with speed $actualSpeed")
                    
                    playerProvider.prepareSession(
                        sessionId = lectureId,
                        url = url,
                        token = token,
                        title = lecture.name,
                        seekTo = actualSeek,
                        speed = actualSpeed,
                        fileId = lecture.videoId
                    )
                } else {
                    _playerState.value = PlayerUiState.ERROR("Video stream not available")
                }
            } else if (hasPdf) {
                // Standalone PDF mode: Set to READY immediately as there is no video to load
                _playerState.value = PlayerUiState.READY
            } else {
                _playerState.value = PlayerUiState.ERROR("No media available in this lecture")
            }
        } catch (e: com.pulse.data.services.btr.PulseAuthException.PermissionRequired) {
            Log.w("LectureViewModel", "Permission required for Drive")
            _playerState.value = PlayerUiState.PERMISSION_REQUIRED(e.intent)
        } catch (e: Exception) {
            Log.e("LectureViewModel", "Failed to process playback", e)
            _playerState.value = PlayerUiState.ERROR("Init failed: ${e.localizedMessage}")
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            playerProvider.playbackState.collect { state ->
                if (!playerProvider.isSessionActive(lectureId)) return@collect
                
                Log.d("LectureViewModel", "ExoPlayer State: $state")
                when (state) {
                    Player.STATE_BUFFERING -> _playerState.value = PlayerUiState.LOADING
                    Player.STATE_READY -> {
                        _playerState.value = PlayerUiState.READY
                        if (player.playWhenReady && !player.isPlaying) {
                            player.play()
                        }
                    }
                    Player.STATE_ENDED -> _playerState.value = PlayerUiState.READY
                    Player.STATE_IDLE -> {
                        // Only error if we aren't intentionally stopping
                        if (player.playerError != null) {
                            _playerState.value = PlayerUiState.ERROR(player.playerError?.message ?: "Idle Error")
                        }
                    } 
                }
            }
        }

        viewModelScope.launch {
            playerProvider.playerError.collect { error ->
                if (error == null || !playerProvider.isSessionActive(lectureId)) return@collect
                logger.e("LectureVM", "Playback error: ${error.errorCodeName} (${error.errorCode})")

                val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED

                val lecture = _lecture.value
                if (isNetworkError && lecture?.videoId != null) {
                    val currentPos = player.currentPosition
                    try {
                        _playerState.value = PlayerUiState.LOADING
                        Log.d("LectureViewModel", "Network/Auth error detected. Invalidating token and retrying...")
                        
                        getLectureStreamUrlUseCase.invalidateToken()
                        
                        val freshResult = getLectureStreamUrlUseCase(lecture.videoId)
                        val freshUrl = freshResult?.first
                        val freshToken = freshResult?.second
                        
                        if (freshUrl != null) {
                            playerProvider.prepareSession(
                                sessionId = lectureId,
                                url = freshUrl,
                                token = freshToken,
                                title = lecture.name,
                                seekTo = currentPos,
                                speed = player.playbackParameters.speed,
                                fileId = lecture?.videoId
                            )
                        } else {
                            _playerState.value = PlayerUiState.ERROR("Failed to refresh stream")
                        }
                    } catch (e: Exception) {
                        Log.e("LectureViewModel", "Token/URL refresh failed", e)
                        _playerState.value = PlayerUiState.ERROR("Link expired. Sign in again.")
                    }
                } else {
                    _playerState.value = PlayerUiState.ERROR(error.message ?: "Player Error")
                }
            }
        }
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
        if (!playerProvider.isSessionActive(lectureId)) return
        val lecture = _lecture.value ?: return
        
        // Capture position and duration synchronously on the main thread
        val pos = player.currentPosition
        val dur = player.duration
        
        // Optimization: Don't save if in IDLE state or at extreme beginning
        if (player.playbackState == Player.STATE_IDLE || dur <= 1000) return

        // Use kotlinx.coroutines.NonCancellable to ensure DB save completes even if VM is cleared
        viewModelScope.launch(kotlinx.coroutines.NonCancellable) {
            repository.updateLectureProgress(lecture, pos, dur)
        }
    }

    fun addNote(text: String) {
        viewModelScope.launch {
            noteRepository.insertNote(
                Note(lectureId = lectureId, timestamp = player.currentPosition, text = text)
            )
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch { noteRepository.deleteNote(noteId) }
    }

    fun addVisual(type: VisualType, data: String, page: Int, color: Int, width: Float) {
        val currentPdfId = _lecture.value?.let { it.pdfLocalPath.ifEmpty { it.pdfId ?: it.id } } ?: lectureId
        viewModelScope.launch {
            noteVisualRepository.insert(
                NoteVisual(
                    lectureId = lectureId,
                    pdfId = currentPdfId,
                    timestamp = player.currentPosition,
                    pageNumber = page,
                    type = type,
                    data = data,
                    color = color,
                    strokeWidth = width
                )
            )
        }
    }

    fun addVisualAtPos(type: VisualType, x: Float, y: Float, page: Int, color: Int) {
        val currentPdfId = _lecture.value?.let { it.pdfLocalPath.ifEmpty { it.pdfId ?: it.id } } ?: lectureId
        viewModelScope.launch {
            noteVisualRepository.insert(
                NoteVisual(
                    lectureId = lectureId,
                    pdfId = currentPdfId,
                    timestamp = player.currentPosition,
                    pageNumber = page,
                    type = type,
                    data = "$x,$y",
                    color = color,
                    strokeWidth = 1f
                )
            )
        }
    }

    fun deleteVisual(id: Long) {
        viewModelScope.launch { noteVisualRepository.delete(id) }
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _lecture.value?.let { l ->
            viewModelScope.launch { repository.updateLectureSpeed(l, speed) }
        }
    }

    fun seekTo(timestamp: Long) {
        player.seekTo(timestamp)
    }



    fun updateLocalPdfPath(path: String) {
        _lecture.value?.let { l ->
            viewModelScope.launch { 
                // Initialize page count for blank note
                if (path == "blank_note" && l.pdfPageCount == 0) {
                    repository.updatePageCount(l.id, 5)
                }
                repository.updateLocalPdfPath(l, path) 
            }
        }
    }

    fun updatePdfState(page: Int) {
        _lecture.value?.let { l ->
            viewModelScope.launch {
                repository.updatePdfState(l.id, page, l.pdfIsHorizontal)
            }
        }
    }

    fun updatePdfOrientation(isHorizontal: Boolean) {
        _lecture.value?.let { l ->
            viewModelScope.launch {
                repository.updatePdfState(l.id, l.lastPdfPage, isHorizontal)
            }
        }
    }

    fun addPage() {
        _lecture.value?.let { l ->
            viewModelScope.launch {
                repository.updatePageCount(l.id, l.pdfPageCount + 10)
            }
        }
    }

    fun onExit() {
        Log.d("LectureViewModel", "Exiting session: $lectureId")
        saveProgress()
        repository.syncPushOnExit(lectureId)
        playerProvider.stopSession(lectureId)
    }

    fun savePdfOrientation(horizontal: Boolean) {
        viewModelScope.launch {
            settingsManager.savePdfHorizontalOrientation(horizontal)
        }
    }

    override fun onCleared() {
        Log.d("LectureViewModel", "ViewModel cleared for $lectureId")
        periodicSaveJob?.cancel()
        saveProgress()
        repository.syncPushOnExit(lectureId)
        if (!playerProvider.isMiniPlayerActive || playerProvider.miniPlayerLectureId != lectureId) {
            playerProvider.stopSession(lectureId)
        }
        super.onCleared()
    }
}

sealed class PlayerUiState {
    object LOADING : PlayerUiState()
    object READY : PlayerUiState()
    data class PERMISSION_REQUIRED(val intent: android.content.Intent) : PlayerUiState()
    data class ERROR(val message: String) : PlayerUiState()
}
