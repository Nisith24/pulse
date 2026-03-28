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

    private val _drivePdfs = MutableStateFlow<List<com.pulse.data.services.btr.BtrFile>>(emptyList())
    val drivePdfs: StateFlow<List<com.pulse.data.services.btr.BtrFile>> = _drivePdfs.asStateFlow()

    private val _isLoadingDrivePdfs = MutableStateFlow(false)
    val isLoadingDrivePdfs: StateFlow<Boolean> = _isLoadingDrivePdfs.asStateFlow()

    // PDF download state (observed from repository — disk-first, no RAM buffering)
    val pdfDownloadState: StateFlow<com.pulse.data.repository.PdfDownloadState> = repository.pdfDownloadState

    // ── Folder PDF: smart selected PDF based on name or subject match ──
    private val _folderPdf = MutableStateFlow<com.pulse.data.services.btr.BtrFile?>(null)
    val folderPdf: StateFlow<com.pulse.data.services.btr.BtrFile?> = _folderPdf.asStateFlow()

    private val _folderPdfMatchType = MutableStateFlow<PdfMatchType>(PdfMatchType.NONE)
    val folderPdfMatchType: StateFlow<PdfMatchType> = _folderPdfMatchType.asStateFlow()

    /**
     * Single source of truth for annotation lookup key.
     * ALWAYS uses lecture.id — the only truly stable identifier.
     * Annotations are per-lecture, not per-file.
     */
    private fun resolveActivePdfId(lecture: Lecture): String {
        return if (lecture.pdfLocalPath == "blank_note") "blank_note" else lecture.id
    }

    val notes: Flow<List<Note>> = noteRepository.getNotes(lectureId)

    // Gate: visuals flow waits until migration completes to avoid querying stale pdfId values
    private val _migrationDone = MutableStateFlow(false)
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val visuals: Flow<List<NoteVisual>> = _migrationDone
        .filter { it } // Wait until migration is done
        .flatMapLatest {
            lecture.filterNotNull()
                .map { resolveActivePdfId(it) }
                .distinctUntilChanged()
                .flatMapLatest { pdfId ->
                    noteVisualRepository.getVisualsForFile(lectureId, pdfId)
                }
        }
    val player = playerProvider.player
    
    val pdfHorizontalOrientation: StateFlow<Boolean> = settingsManager.pdfHorizontalOrientationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var periodicSaveJob: Job? = null
    private var hasStartedPlayback = false

    init {
        // Migrate old annotations (saved under volatile pdfId) to use stable lecture.id
        // Must complete BEFORE the visuals flow starts querying
        viewModelScope.launch {
            noteVisualRepository.migrateToLectureId(lectureId)
            _migrationDone.value = true
        }
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
        if (!playerProvider.isSessionActive(lectureId) || _playerState.value is PlayerUiState.ERROR) {
            viewModelScope.launch {
                hasStartedPlayback = false
                if (_playerState.value is PlayerUiState.ERROR) {
                    playerProvider.stopSession(lectureId)
                }
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
        // If we already started AND the player is still healthy/preparing/ready, don't re-prepare
        if (hasStartedPlayback && player.playbackState != Player.STATE_IDLE) return
        
        try {
            hasStartedPlayback = true 
            _playerState.value = PlayerUiState.LOADING
            
            val hasVideo = !lecture.videoId.isNullOrEmpty() || !lecture.videoLocalPath.isNullOrEmpty()
            val hasPdf = !lecture.pdfId.isNullOrEmpty() || lecture.pdfLocalPath.isNotEmpty()

            if (hasVideo) {
                val result = getLectureStreamUrlUseCase(lecture.videoId)
                val url = lecture.videoLocalPath.takeIf { !it.isNullOrBlank() } ?: result?.first
                val token = result?.second

                if (url != null) {
                    hasStartedPlayback = true
                    val shouldResume = settingsManager.resumePlaybackFlow.first()
                    val defSpeed = settingsManager.defaultSpeedFlow.first()
                    
                    val actualSeek = if (shouldResume) lecture.lastPosition else 0L
                    val actualSpeed = if (lecture.speed != 1.0f) lecture.speed else defSpeed
                    
                    logger.d("LectureViewModel", "Requesting session for ${lecture.name} at $actualSeek with speed $actualSpeed")
                    
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
            logger.d("LectureViewModel", "Permission required for Drive")
            _playerState.value = PlayerUiState.PERMISSION_REQUIRED(e.intent)
        } catch (e: Exception) {
            logger.e("LectureViewModel", "Failed to process playback", e)
            _playerState.value = PlayerUiState.ERROR("Init failed: ${e.localizedMessage}")
        }
    }

    private fun observePlayerState() {
        viewModelScope.launch {
            playerProvider.playbackState.collect { state ->
                if (!playerProvider.isSessionActive(lectureId)) return@collect
                

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
                        logger.d("LectureViewModel", "Network/Auth error. Invalidating token and retrying...")
                        
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
                                fileId = lecture.videoId
                            )
                        } else {
                            _playerState.value = PlayerUiState.ERROR("Failed to refresh stream")
                        }
                    } catch (e: Exception) {
                        logger.e("LectureViewModel", "Token/URL refresh failed", e)
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

    fun addVisual(type: VisualType, data: String, page: Int, color: Int, width: Float, alpha: Float = 1f) {
        val currentPdfId = _lecture.value?.let { resolveActivePdfId(it) } ?: "blank_note"

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
                    strokeWidth = width,
                    alpha = alpha
                )
            )
        }
    }

    fun addVisualAtPos(type: VisualType, x: Float, y: Float, page: Int, color: Int, width: Float = 1f, alpha: Float = 1f) {
        val currentPdfId = _lecture.value?.let { resolveActivePdfId(it) } ?: "blank_note"
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
                    strokeWidth = width,
                    alpha = alpha
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

    /** Import a local PDF by copying it to internal storage for stable persistence */
    fun importLocalPdf(contentUri: android.net.Uri) {
        _lecture.value?.let { l ->
            viewModelScope.launch {
                // Copy to internal storage for a stable path that survives app restarts
                val stablePath = repository.copyLocalPdfToInternal(l.id, contentUri)
                if (stablePath == null) {
                    // Fallback: use the content URI directly (old behavior)
                    repository.updateLocalPdfPath(l, contentUri.toString())
                }
                // If copy succeeded, the repository already updated the lecture's pdfLocalPath
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
        logger.d("LectureViewModel", "Exiting session: $lectureId")
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
        logger.d("LectureViewModel", "ViewModel cleared for $lectureId")
        periodicSaveJob?.cancel()
        saveProgress()
        repository.syncPushOnExit(lectureId)
        if (!playerProvider.isMiniPlayerActive || playerProvider.miniPlayerLectureId != lectureId) {
            playerProvider.stopSession(lectureId)
        }
        super.onCleared()
    }

    fun downloadPdf() {
        val currentLecture = _lecture.value ?: return
        if (currentLecture.isPdfDownloaded) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.downloadLecturePdf(currentLecture)
        }
    }

    fun loadDrivePdfs(folderId: String? = null) {
        val resolvedFolderId = folderId ?: com.pulse.core.domain.util.Constants.DRIVE_FOLDER_ID
        viewModelScope.launch {
            _isLoadingDrivePdfs.value = true
            _drivePdfs.value = emptyList()
            _folderPdf.value = null
            _folderPdfMatchType.value = PdfMatchType.NONE
            try {
                val pdfs = repository.listPdfs(resolvedFolderId)
                _drivePdfs.value = pdfs

                // Smart Name Matching Logic
                val currentLecture = _lecture.value
                if (currentLecture != null && pdfs.isNotEmpty()) {
                    var bestScore = 0.0f
                    var bestMatch: com.pulse.data.services.btr.BtrFile? = null

                    // Step A: Find matching PDF by Lecture Name
                    for (pdf in pdfs) {
                        // Strip common extensions before matching
                        val cleanPdfName = pdf.name.replace(".pdf", "", ignoreCase = true)
                        val score = com.pulse.core.domain.util.StringUtils.similarityScore(currentLecture.name, cleanPdfName)
                        if (score > bestScore) {
                            bestScore = score
                            bestMatch = pdf
                        }
                    }

                    // Step B: If confidence is high (> 0.7), use it as Exact/Smart match
                    if (bestMatch != null && bestScore > 0.7f) {
                        _folderPdf.value = bestMatch
                        _folderPdfMatchType.value = PdfMatchType.EXACT
                        logger.d("LectureViewModel", "Smart matched PDF: ${bestMatch.name} with score $bestScore")
                    } else {
                        // Step D: Fallback to Subject Matching if no good name match is found
                        val subjectName = currentLecture.subject
                        if (!subjectName.isNullOrEmpty()) {
                            var bestSubjectScore = 0.0f
                            var bestSubjectMatch: com.pulse.data.services.btr.BtrFile? = null

                            for (pdf in pdfs) {
                                val cleanPdfName = pdf.name.replace(".pdf", "", ignoreCase = true)
                                val score = com.pulse.core.domain.util.StringUtils.similarityScore(subjectName, cleanPdfName)
                                if (score > bestSubjectScore) {
                                    bestSubjectScore = score
                                    bestSubjectMatch = pdf
                                }
                            }

                            if (bestSubjectMatch != null && bestSubjectScore > 0.6f) {
                                _folderPdf.value = bestSubjectMatch
                                _folderPdfMatchType.value = PdfMatchType.SUBJECT
                                logger.d("LectureViewModel", "Subject matched PDF: ${bestSubjectMatch.name} with score $bestSubjectScore")
                            } else {
                                // No safe match, leave folderPdf as null to force manual selection, or default to first if preferred
                                // As per instruction: "display a button to display the relevance subject name PDF... restrict to process"
                                // we will just keep it null so the UI prompts the user to open the picker.
                                _folderPdf.value = null
                                _folderPdfMatchType.value = PdfMatchType.NONE
                            }
                        }
                    }
                } else {
                    _folderPdf.value = pdfs.firstOrNull()
                    _folderPdfMatchType.value = PdfMatchType.NONE
                }
            } catch (e: Exception) {
                logger.e("LectureViewModel", "Failed to load Drive PDFs", e)
            } finally {
                _isLoadingDrivePdfs.value = false
            }
        }
    }

    fun attachDrivePdf(pdf: com.pulse.data.services.btr.BtrFile) {
        viewModelScope.launch {
            repository.resetPdfDownloadState()
            val lecture = _lecture.value ?: return@launch
            
            // If already cached on disk, just update DB (instant)
            val computedPath = repository.getLecturePdfPath(lecture.id, pdf)
            if (java.io.File(computedPath).exists()) {
                repository.attachDrivePdfToLecture(lecture.id, pdf)
                return@launch
            }

            // Download to disk with progress (shown in UI via pdfDownloadState)
            launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    repository.attachDrivePdfToLecture(lecture.id, pdf)
                } catch (e: Exception) {
                    logger.e("LectureViewModel", "PDF download failed", e)
                }
            }
        }
    }
}


sealed class PlayerUiState {
    object LOADING : PlayerUiState()
    object READY : PlayerUiState()
    data class PERMISSION_REQUIRED(val intent: android.content.Intent) : PlayerUiState()
    data class ERROR(val message: String) : PlayerUiState()
}

enum class PdfMatchType {
    EXACT, SUBJECT, NONE
}
