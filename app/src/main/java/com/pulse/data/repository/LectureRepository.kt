package com.pulse.data.repository

import com.pulse.core.domain.util.HlcGenerator
import com.pulse.core.domain.util.ILogger
import com.pulse.core.domain.util.Constants
import com.pulse.core.data.db.Lecture
import com.pulse.core.data.db.CustomList
import com.pulse.core.data.db.CustomListLectureCrossRef
import com.pulse.data.db.LectureDao
import com.pulse.data.db.CustomListDao
import com.pulse.domain.services.btr.IBtrAuthManager
import com.pulse.domain.services.btr.IBtrService
import com.pulse.data.local.FileStorageManager
import com.pulse.domain.usecase.SyncLecturesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import com.pulse.data.services.btr.BtrFile
import com.pulse.data.db.DriveFileDao
import com.pulse.core.data.db.toDomainModel
import com.pulse.core.data.db.toEntity

sealed class DownloadStatus {
    data class Downloading(val progress: Float, val speed: String, val eta: String, val size: String) : DownloadStatus()
    object Done : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

/** PDF-specific download state exposed to the ViewModel/UI */
sealed class PdfDownloadState {
    object Idle : PdfDownloadState()
    data class Downloading(val progress: Float, val speedBytesPerSec: Long, val downloadedBytes: Long, val totalBytes: Long) : PdfDownloadState()
    object Done : PdfDownloadState()
    data class Error(val message: String) : PdfDownloadState()
}

class LectureRepository(
    private val lectureDao: LectureDao,
    private val btrService: IBtrService,
    private val authManager: IBtrAuthManager,
    private val fileStorage: FileStorageManager,
    private val syncLecturesUseCase: SyncLecturesUseCase,
    private val hlcGenerator: HlcGenerator,
    private val logger: ILogger,
    private val context: android.content.Context,
    private val syncManager: com.pulse.data.sync.FirestoreSyncManager,
    private val customListDao: CustomListDao,
    private val driveFileDao: DriveFileDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val activeDownloads = _activeDownloads.asStateFlow()
    private val downloadJobs = ConcurrentHashMap<String, Job>()

    // ── Memory Caches (for fallback/short-lived) ──
    private val streamUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()

    // ── PDF Download State (observed by ViewModel) ──
    private val _pdfDownloadState = MutableStateFlow<PdfDownloadState>(PdfDownloadState.Idle)
    val pdfDownloadState = _pdfDownloadState.asStateFlow()

    val btrLectures: Flow<List<Lecture>> = lectureDao.getBtrLectures()
    val localLectures: Flow<List<Lecture>> = lectureDao.getLocalLectures()
    val downloadedLectures: Flow<List<Lecture>> = lectureDao.getDownloadedLectures()
    val cloudOnlyLectures: Flow<List<Lecture>> = lectureDao.getCloudOnlyLectures()
    val recentLecture: Flow<Lecture?> = lectureDao.getRecentLecture()
    val completedLectures: Flow<List<Lecture>> = lectureDao.getCompletedLectures()

    fun getLectureById(id: String): Flow<Lecture?> = lectureDao.getById(id)
    fun getLecturesBySubject(subject: String): Flow<List<Lecture>> = lectureDao.getLecturesBySubject(subject)

    // ── Custom Lists ──
    fun getAllCustomLists(): Flow<List<CustomList>> = customListDao.getAllCustomLists()
    fun getLecturesForCustomList(listId: Long): Flow<List<Lecture>> = customListDao.getLecturesForCustomList(listId)
    suspend fun createCustomList(name: String): Long = customListDao.insertCustomList(CustomList(name = name))
    suspend fun deleteCustomList(listId: Long) = customListDao.deleteCustomList(listId)
    suspend fun addLectureToCustomList(listId: Long, lectureId: String) =
        customListDao.insertLectureToCustomList(CustomListLectureCrossRef(listId = listId, lectureId = lectureId))
    suspend fun removeLectureFromCustomList(listId: Long, lectureId: String) =
        customListDao.removeLectureFromCustomList(listId, lectureId)

    // --- Sync: push single lecture to Firestore (fire-and-forget) ---
    private fun firebasePush(lectureId: String) {
        scope.launch {
            try {
                val lecture = lectureDao.getById(lectureId).first() ?: return@launch
                syncManager.pushSingleLecture(lecture)
                com.pulse.data.sync.FirestoreSyncWorker.enqueueDebouncedSync(context)
            } catch (e: Exception) {
                logger.e("FirebaseSync", "Push failed for $lectureId: ${e.message}")
            }
        }
    }

    /** Called on video exit — pushes current state to Firestore */
    fun syncPushOnExit(lectureId: String) = firebasePush(lectureId)

    /** Pull all lectures from Firestore (called on BTR refresh) */
    suspend fun pullFromFirestore() {
        try {
            syncManager.pullAndMergeAll()
        } catch (e: Exception) {
            logger.e("FirebaseSync", "Pull failed: ${e.message}")
        }
    }

    suspend fun syncWithCloud(): com.pulse.core.domain.util.Result<Unit> = withContext(Dispatchers.IO) {
        com.pulse.core.domain.util.safeApiCall {
            pullFromFirestore()
        }
    }

    suspend fun deleteOfflineVideo(lectureId: String) = withContext(Dispatchers.IO) {
        val lecture = lectureDao.getById(lectureId).first()
        lecture?.videoLocalPath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updateVideoLocalPath(lectureId, "", now, hlc)
    }

    suspend fun addLocalLecture(name: String, videoPath: String?, pdfPath: String?) {
        addLocalLectureWithId(UUID.randomUUID().toString(), name, videoPath, pdfPath)
    }

    suspend fun addLocalLectureWithId(id: String, name: String, videoPath: String?, pdfPath: String?) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        val lecture = Lecture(
            id = id, name = name, videoId = null, pdfId = null,
            pdfLocalPath = pdfPath ?: "", videoLocalPath = videoPath,
            isPdfDownloaded = pdfPath != null, isLocal = true,
            hlcTimestamp = hlc, updatedAt = now
        )
        lectureDao.insert(lecture)
    }

    suspend fun addDriveLecture(id: String, name: String, subject: String, topic: String, videoId: String?, pdfId: String?) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        val lecture = Lecture(
            id = id, name = name, videoId = videoId, pdfId = pdfId,
            pdfLocalPath = "", videoLocalPath = null,
            isPdfDownloaded = false, isLocal = false,
            hlcTimestamp = hlc, updatedAt = now,
            subject = subject
        )
        lectureDao.insert(lecture)
    }

    suspend fun updateLectureProgress(lecture: Lecture, currentPosition: Long, duration: Long) {
        val resetThreshold = if (duration > 0) duration.toDouble() * 0.95 else Double.MAX_VALUE
        val finalPosition = if (currentPosition.toDouble() > resetThreshold) 0L else currentPosition
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updateProgress(
            id = lecture.id, lastPosition = finalPosition,
            duration = duration, updatedAt = now, hlcTimestamp = hlc
        )
        // No push here — pushed on video exit only
    }

    suspend fun updateLectureSpeed(lecture: Lecture, speed: Float) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updateSpeed(
            id = lecture.id, speed = speed, updatedAt = now, hlcTimestamp = hlc
        )
        // Local-only, no sync needed
    }

    suspend fun updateLocalPdfPath(lecture: Lecture, path: String) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updatePdfPath(
            id = lecture.id, path = path, updatedAt = now, hlcTimestamp = hlc
        )
        // Local-only, no sync needed
    }

    suspend fun updatePageCount(lectureId: String, count: Int) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updatePageCount(
            id = lectureId, count = count, updatedAt = now, hlcTimestamp = hlc
        )
        // Local-only, no sync needed
    }

    /** Copy a content:// PDF to internal storage and update the lecture's path to the stable copy */
    suspend fun copyLocalPdfToInternal(lectureId: String, contentUri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        val stablePath = fileStorage.copyContentUriToInternal(contentUri, lectureId) ?: return@withContext null
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        val lecture = lectureDao.getById(lectureId).first() ?: return@withContext null
        lectureDao.update(lecture.copy(
            pdfLocalPath = stablePath,
            isPdfDownloaded = true,
            updatedAt = now,
            hlcTimestamp = hlc
        ))
        stablePath
    }

    suspend fun updatePdfState(lectureId: String, page: Int, isHorizontal: Boolean) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updatePdfState(
            id = lectureId, page = page, isHorizontal = isHorizontal,
            updatedAt = now, hlcTimestamp = hlc
        )
        // Local-only, no sync needed
    }

    suspend fun toggleFavorite(lectureId: String) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.toggleFavorite(lectureId, now, hlc)
        firebasePush(lectureId) // Infrequent user action
    }

    suspend fun sync(): com.pulse.core.domain.util.Result<Unit> = withContext(Dispatchers.IO) {
        com.pulse.core.domain.util.safeApiCall {
            // Step 1: Pull from Firestore first (get latest from other devices)
            pullFromFirestore()

            // Step 2: Sync from Google Drive (BTR files)
            val token = authManager.getToken()
            
            logger.d("LectureSync", "Starting sync with folder: ${Constants.DRIVE_FOLDER_ID}")
            val files = btrService.listFolder(Constants.DRIVE_FOLDER_ID, token)
            logger.d("LectureSync", "Found ${files.size} files in Drive folder")

            val grouped = syncLecturesUseCase(files)
            
            val existingLectures = if (grouped.isNotEmpty()) {
                val ids = grouped.map { it.id }
                // SQLite limits IN clauses to 999 items, so we chunk to be safe
                ids.chunked(900).flatMap { chunk ->
                    lectureDao.getLecturesByIds(chunk)
                }.associateBy { it.id }
            } else {
                emptyMap()
            }
            
            val hlc = hlcGenerator.generate()
            val lecturesToInsert = grouped.map { newLecture ->
                val existing = existingLectures[newLecture.id]
                if (existing != null) {
                    newLecture.copy(
                        lastPosition = existing.lastPosition,
                        videoDuration = existing.videoDuration,
                        speed = existing.speed,
                        isFavorite = existing.isFavorite,
                        pdfPageCount = existing.pdfPageCount,
                        lastPdfPage = existing.lastPdfPage,
                        pdfIsHorizontal = existing.pdfIsHorizontal,
                        subject = existing.subject,
                        pdfLocalPath = existing.pdfLocalPath.ifEmpty { newLecture.pdfLocalPath },
                        videoLocalPath = existing.videoLocalPath,
                        isPdfDownloaded = existing.isPdfDownloaded,
                        isDeleted = existing.isDeleted,
                        updatedAt = existing.updatedAt,
                        hlcTimestamp = existing.hlcTimestamp
                    )
                } else {
                    newLecture.copy(hlcTimestamp = hlc)
                }
            }
            
            lectureDao.insertAll(lecturesToInsert)
            lectureDao.markMissingBtrDeleted(grouped.map { it.id }, hlc)

            // Step 3: Push all BTR lectures to Firestore
            val btrToSync = lecturesToInsert.filter { !it.isLocal }
            if (btrToSync.isNotEmpty()) {
                syncManager.pushLectures(btrToSync)
                logger.d("LectureSync", "Pushed ${btrToSync.size} BTR lectures to Firestore")
            }

            // PDFs are now downloaded on-demand when the user opens a lecture
        }
    }

    suspend fun syncSubjectFolder(folderId: String, subjectName: String): com.pulse.core.domain.util.Result<Unit> = withContext(Dispatchers.IO) {
        com.pulse.core.domain.util.safeApiCall {
            val token = authManager.getToken()
            val files = btrService.listFolder(folderId, token, recursive = true)
            val grouped = syncLecturesUseCase(files)
            
            val existingLectures = if (grouped.isNotEmpty()) {
                val ids = grouped.map { it.id }
                ids.chunked(900).flatMap { chunk ->
                    lectureDao.getLecturesByIds(chunk)
                }.associateBy { it.id }
            } else {
                emptyMap()
            }
            val hlc = hlcGenerator.generate()
            
            val lecturesToInsert = grouped.map { newLecture ->
                val existing = existingLectures[newLecture.id]
                if (existing != null) {
                    newLecture.copy(
                        lastPosition = existing.lastPosition,
                        videoDuration = existing.videoDuration,
                        speed = existing.speed,
                        isFavorite = existing.isFavorite,
                        pdfPageCount = existing.pdfPageCount,
                        lastPdfPage = existing.lastPdfPage,
                        pdfIsHorizontal = existing.pdfIsHorizontal,
                        subject = existing.subject.takeIf { !it.isNullOrBlank() } ?: subjectName,
                        pdfLocalPath = existing.pdfLocalPath.ifEmpty { newLecture.pdfLocalPath },
                        videoLocalPath = existing.videoLocalPath,
                        isPdfDownloaded = existing.isPdfDownloaded,
                        isDeleted = existing.isDeleted,
                        updatedAt = existing.updatedAt,
                        hlcTimestamp = existing.hlcTimestamp
                    )
                } else {
                    newLecture.copy(subject = subjectName, hlcTimestamp = hlc)
                }
            }
            
            lectureDao.insertAll(lecturesToInsert)
            lectureDao.markMissingSubjectBtrDeleted(subjectName, grouped.map { it.id }, hlc)
            
            val btrToSync = lecturesToInsert.filter { !it.isLocal }
            if (btrToSync.isNotEmpty()) {
                syncManager.pushLectures(btrToSync)
            }
            // PDFs are now downloaded on-demand when the user opens a lecture
        }
    }

    /** Public entry point for on-demand PDF download — disk-first with progress */
    suspend fun downloadPdfToDisk(pdfId: String, onProgress: (Long, Long) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val token = try { authManager.getToken() } catch (e: Exception) { "" }
        val tmpFile = File(fileStorage.getPdfTmpPath(pdfId))
        val finalFile = File(fileStorage.getPdfFinalPath(pdfId))
        btrService.downloadToFile(pdfId, token, tmpFile, finalFile, onProgress)
        finalFile.absolutePath
    }

    /** Download a lecture's paired PDF to disk and update DB */
    suspend fun downloadLecturePdf(lecture: Lecture) = withContext(Dispatchers.IO) {
        val pdfId = lecture.pdfId ?: return@withContext
        _pdfDownloadState.value = PdfDownloadState.Downloading(0f, 0, 0, 0)
        try {
            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()
            val path = downloadPdfToDisk(pdfId) { bytes, total ->
                val now = System.currentTimeMillis()
                val dt = now - lastTime
                val speedBps = if (dt > 0) ((bytes - lastBytes).toDouble() / (dt / 1000.0)).toLong() else 0L
                val progress = if (total > 0L) bytes.toFloat() / total else 0f
                _pdfDownloadState.value = PdfDownloadState.Downloading(progress, speedBps, bytes, total)
                if (dt > 500) {
                    lastBytes = bytes
                    lastTime = now
                }
            }
            val now = System.currentTimeMillis()
            val hlc = hlcGenerator.generate()
            lectureDao.update(lecture.copy(
                pdfLocalPath = path,
                isPdfDownloaded = true,
                updatedAt = now,
                hlcTimestamp = hlc
            ))
            fileStorage.evictPdfCacheIfNeeded()
            _pdfDownloadState.value = PdfDownloadState.Done
        } catch (e: Exception) {
            logger.e("LectureRepo", "PDF download failed for ${lecture.name}", e)
            _pdfDownloadState.value = PdfDownloadState.Error(e.message ?: "Download failed")
        }
    }

    /** Observe PDF files in a Drive folder locally, syncing if needed */
    fun observePdfs(folderId: String, forceSync: Boolean = false): Flow<List<BtrFile>> {
        triggerFolderSyncIfNeeded(folderId, true, forceSync)
        return driveFileDao.observeFilesByParentId(folderId).map { files ->
            files.map { it.toDomainModel() }
                 .filter { it.mimeType == "application/pdf" || it.name.endsWith(".pdf", ignoreCase = true) }
        }
    }

    /** Observe subfolders in a Drive folder locally, syncing if needed */
    fun observeSubfolders(folderId: String, forceSync: Boolean = false): Flow<List<BtrFile>> {
        triggerFolderSyncIfNeeded(folderId, false, forceSync)
        return driveFileDao.observeFilesByParentId(folderId).map { files ->
            files.map { it.toDomainModel() }
                 .filter { it.mimeType == "application/vnd.google-apps.folder" }
        }
    }

    private fun triggerFolderSyncIfNeeded(folderId: String, isPdfRequest: Boolean, forceSync: Boolean) {
        scope.launch {
            try {
                val lastSyncTime = driveFileDao.getLastSyncTime(folderId) ?: 0L
                val now = System.currentTimeMillis()
                val twelveHoursInMillis = 12 * 60 * 60 * 1000L

                if (forceSync || now - lastSyncTime > twelveHoursInMillis) {
                    val token = try { authManager.getToken() } catch (e: Exception) { "" }

                    // Fallback to anonymous access if token fails, since shared folders can often be viewed publicly
                    val actualToken = if (token.isEmpty()) null else token

                    val remoteFiles = try {
                        if (isPdfRequest) {
                            btrService.listFolder(folderId, actualToken ?: "")
                        } else {
                            btrService.listSubfolders(folderId, actualToken ?: "")
                        }
                    } catch (e: Exception) {
                        logger.e("LectureRepo", "Network call failed for background sync $folderId", e)
                        return@launch // Silently fallback to Room cache
                    }

                    val entities = remoteFiles.map { it.toEntity(folderId, now) }
                    // Update Room DB safely
                    driveFileDao.replaceFolderContents(folderId, entities)
                }
            } catch (e: Exception) {
                logger.e("LectureRepo", "Background sync completely failed for $folderId", e)
                // Let it fail silently, UI will just show the cached data from Room
            }
        }
    }

    /** Old suspended versions (can wrap the flow for compatibility if needed elsewhere) */
    suspend fun listPdfs(folderId: String, forceSync: Boolean = false): List<BtrFile> = observePdfs(folderId, forceSync).first()
    suspend fun listSubfolders(folderId: String, forceSync: Boolean = false): List<BtrFile> = observeSubfolders(folderId, forceSync).first()

    /** Compute the local path where a Drive PDF would be cached */
    fun getLecturePdfPath(lectureId: String, pdf: com.pulse.data.services.btr.BtrFile): String {
        return fileStorage.getPdfFinalPath(pdf.id)
    }

    /** Attach a Drive PDF to a lecture — downloads to cache with progress */
    suspend fun attachDrivePdfToLecture(lectureId: String, pdf: com.pulse.data.services.btr.BtrFile) = withContext(Dispatchers.IO) {
        val finalPath = fileStorage.getPdfFinalPath(pdf.id)
        val finalFile = File(finalPath)

        if (!finalFile.exists()) {
            _pdfDownloadState.value = PdfDownloadState.Downloading(0f, 0, 0, 0)
            try {
                var lastBytes = 0L
                var lastTime = System.currentTimeMillis()
                val token = try { authManager.getToken() } catch (e: Exception) { "" }
                val tmpFile = File(fileStorage.getPdfTmpPath(pdf.id))
                btrService.downloadToFile(pdf.id, token, tmpFile, finalFile) { bytes, total ->
                    val now = System.currentTimeMillis()
                    val dt = now - lastTime
                    val speedBps = if (dt > 0) ((bytes - lastBytes).toDouble() / (dt / 1000.0)).toLong() else 0L
                    val progress = if (total > 0L) bytes.toFloat() / total else 0f
                    _pdfDownloadState.value = PdfDownloadState.Downloading(progress, speedBps, bytes, total)
                    if (dt > 500) {
                        lastBytes = bytes
                        lastTime = now
                    }
                }
            } catch (e: Exception) {
                logger.e("LectureRepo", "Attach PDF download failed", e)
                _pdfDownloadState.value = PdfDownloadState.Error(e.message ?: "Download failed")
                return@withContext
            }
        }

        val lecture = lectureDao.getById(lectureId).first()
        if (lecture != null) {
            val now = System.currentTimeMillis()
            val hlc = hlcGenerator.generate()
            lectureDao.update(lecture.copy(
                pdfId = pdf.id,
                pdfLocalPath = finalPath,
                isPdfDownloaded = true,
                updatedAt = now,
                hlcTimestamp = hlc
            ))
            fileStorage.evictPdfCacheIfNeeded()
        }
        _pdfDownloadState.value = PdfDownloadState.Done
    }

    /** Reset PDF download state to idle */
    fun resetPdfDownloadState() {
        _pdfDownloadState.value = PdfDownloadState.Idle
    }

    /** Mark a lecture as completed (100% progress) */
    suspend fun markAsCompleted(lectureId: String) {
        val lecture = lectureDao.getById(lectureId).first() ?: return
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updateProgress(
            id = lectureId,
            lastPosition = 0L,
            duration = lecture.videoDuration,
            updatedAt = now,
            hlcTimestamp = hlc
        )
        firebasePush(lectureId)
    }

    /** Reset lecture progress back to zero */
    suspend fun resetProgress(lectureId: String) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updateProgress(
            id = lectureId,
            lastPosition = 0L,
            duration = 0L,
            updatedAt = now,
            hlcTimestamp = hlc
        )
    }

    // Old private downloadPdf removed — replaced by downloadLecturePdf()

    suspend fun downloadVideoToApp(lecture: Lecture, onProgress: (Long, Long) -> Unit = {_,_->}): String = withContext(Dispatchers.IO) {
        val videoId = lecture.videoId ?: error("No video ID")
        val token = authManager.getToken()
        logger.d("Download", "Downloading video: ${lecture.name}")
        val videoDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "Pulse").apply { mkdirs() }
        val tmpFile = File(videoDir, "${lecture.id}.mp4.tmp")
        
        try {
            tmpFile.outputStream().use { os ->
                btrService.downloadToStream(videoId, os, token, onProgress)
            }
            val finalFile = File(videoDir, "${lecture.id}.mp4")
            tmpFile.renameTo(finalFile)
            val path = finalFile.absolutePath
            val now = System.currentTimeMillis()
            val hlc = hlcGenerator.generate()
            lectureDao.updateVideoLocalPath(lecture.id, path, now, hlc)
            logger.d("Download", "Saved offline: ${lecture.name} -> $path")
            path
        } catch (e: Exception) {
            if (tmpFile.exists()) tmpFile.delete()
            throw e
        }
    }

    suspend fun downloadVideoToStream(lecture: Lecture, outputStream: java.io.OutputStream, onProgress: (Long, Long) -> Unit = {_,_->}): String = withContext(Dispatchers.IO) {
        val videoId = lecture.videoId ?: error("No video ID")
        val token = authManager.getToken()
        val fileName = "${lecture.name.replace(Regex("[^a-zA-Z0-9._\\- ]"), "")}.mp4"
        btrService.downloadToStream(videoId, outputStream, token, onProgress)
        fileName
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        fileStorage.clearCache()
    }

    suspend fun deleteLecture(id: String) {
        val hlc = hlcGenerator.generate()
        lectureDao.markDeleted(id, hlc)
        firebasePush(id)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun formatTime(seconds: Long): String {
        if (seconds < 0) return "--"
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    fun startDownload(lecture: Lecture) {
        if (_activeDownloads.value.containsKey(lecture.id)) return
        _activeDownloads.update { it + (lecture.id to DownloadStatus.Downloading(0f, "", "", "")) }
        
        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()

        val job = scope.launch {
            try {
                downloadVideoToApp(lecture) { bytes, total ->
                    val now = System.currentTimeMillis()
                    val dt = now - lastTime
                    if (dt > 500 || bytes == total) {
                        val speedBps = if (dt > 0) ((bytes - lastBytes).toDouble() / (dt / 1000.0)).toLong() else 0L
                        val speedStr = if (speedBps > 0) formatSize(speedBps) + "/s" else ""
                        val progress = if (total > 0L) bytes.toFloat() / total else 0f
                        val etaStr = if (speedBps > 0 && total > 0) formatTime((total - bytes) / speedBps) else "--"
                        val sizeStr = "${formatSize(bytes)} / ${formatSize(total)}"
                        
                        _activeDownloads.update { 
                            it + (lecture.id to DownloadStatus.Downloading(progress, speedStr, etaStr, sizeStr))
                        }
                        lastBytes = bytes
                        lastTime = now
                    }
                }
                _activeDownloads.update { it + (lecture.id to DownloadStatus.Done) }
                kotlinx.coroutines.delay(2000)
                _activeDownloads.update { it - lecture.id }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _activeDownloads.update { it - lecture.id }
            } catch (e: Exception) {
                _activeDownloads.update { it + (lecture.id to DownloadStatus.Error(e.message ?: "Failed")) }
            } finally {
                downloadJobs.remove(lecture.id)
            }
        }
        downloadJobs[lecture.id] = job
    }

    fun cancelDownload(lectureId: String) {
        val job = downloadJobs.remove(lectureId)
        job?.cancel()
        _activeDownloads.update { it - lectureId }
    }
}
