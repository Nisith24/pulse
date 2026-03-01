package com.pulse.data.repository

import com.pulse.core.domain.util.HlcGenerator
import com.pulse.core.domain.util.ILogger
import com.pulse.core.domain.util.Constants
import com.pulse.core.data.db.Lecture
import com.pulse.data.db.LectureDao
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed class DownloadStatus {
    data class Downloading(val progress: Float, val speed: String, val eta: String, val size: String) : DownloadStatus()
    object Done : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

class LectureRepository(
    private val lectureDao: LectureDao,
    private val btrService: IBtrService,
    private val authManager: IBtrAuthManager,
    private val fileStorage: FileStorageManager,
    private val syncLecturesUseCase: SyncLecturesUseCase,
    private val hlcGenerator: HlcGenerator,
    private val logger: ILogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val activeDownloads = _activeDownloads.asStateFlow()
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    val btrLectures: Flow<List<Lecture>> = lectureDao.getBtrLectures()
    val localLectures: Flow<List<Lecture>> = lectureDao.getLocalLectures()
    val downloadedLectures: Flow<List<Lecture>> = lectureDao.getDownloadedLectures()
    val cloudOnlyLectures: Flow<List<Lecture>> = lectureDao.getCloudOnlyLectures()

    fun getLectureById(id: String): Flow<Lecture?> = lectureDao.getById(id)

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
            id = id,
            name = name,
            videoId = null,
            pdfId = null,
            pdfLocalPath = pdfPath ?: "",
            videoLocalPath = videoPath,
            isPdfDownloaded = pdfPath != null,
            isLocal = true,
            hlcTimestamp = hlc,
            updatedAt = now
        )
        lectureDao.insert(lecture)
    }

    suspend fun updateLectureProgress(lecture: Lecture, currentPosition: Long, duration: Long) {
        val resetThreshold = if (duration > 0) duration.toDouble() * 0.95 else Double.MAX_VALUE
        val finalPosition = if (currentPosition.toDouble() > resetThreshold) 0L else currentPosition
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updateProgress(
            id = lecture.id,
            lastPosition = finalPosition,
            duration = duration,
            updatedAt = now,
            hlcTimestamp = hlc
        )
    }

    suspend fun updateLectureSpeed(lecture: Lecture, speed: Float) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updateSpeed(
            id = lecture.id,
            speed = speed,
            updatedAt = now,
            hlcTimestamp = hlc
        )
    }

    suspend fun updateLocalPdfPath(lecture: Lecture, path: String) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updatePdfPath(
            id = lecture.id,
            path = path,
            updatedAt = now,
            hlcTimestamp = hlc
        )
    }

    suspend fun updatePageCount(lectureId: String, count: Int) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.updatePageCount(
            id = lectureId,
            count = count,
            updatedAt = now,
            hlcTimestamp = hlc
        )
    }

    suspend fun toggleFavorite(lectureId: String) {
        val now = System.currentTimeMillis()
        val hlc = hlcGenerator.generate()
        lectureDao.toggleFavorite(lectureId, now, hlc)
    }

    suspend fun sync() {
        val token = try { authManager.getToken() } catch (e: Exception) { 
            logger.e("LectureSync", "Failed to get token: ${e.message}")
            throw e 
        }
        logger.d("LectureSync", "Starting sync with folder: ${Constants.DRIVE_FOLDER_ID}")
        val files = btrService.listFolder(Constants.DRIVE_FOLDER_ID, token)
        logger.d("LectureSync", "Found ${files.size} files in public Drive folder")
        logger.d("LectureSync", "Found ${files.size} files in Drive folder")

        val grouped = syncLecturesUseCase(files)
        
        // For CRDT sync from Drive, we need to be careful not to overwrite local-only progress
        // if the lecture already exists.
        val hlc = hlcGenerator.generate()
        val lecturesToInsert = grouped.map { it.copy(hlcTimestamp = hlc) }
        
        lectureDao.insertAll(lecturesToInsert)
        lectureDao.markMissingBtrDeleted(grouped.map { it.id }, hlc)

        // Auto-download PDFs that aren't local yet
        for (lecture in grouped) {
            if (lecture.pdfId != null && !lecture.isPdfDownloaded) {
                try {
                    downloadPdf(lecture, token)
                } catch (e: Exception) {
                    logger.e("LectureSync", "Failed to download PDF for ${lecture.name}: ${e.message}")
                }
            }
        }
    }

    private suspend fun downloadPdf(lecture: Lecture, token: String) = withContext(Dispatchers.IO) {
        val pdfId = lecture.pdfId ?: return@withContext
        val file = File(lecture.pdfLocalPath)
        file.parentFile?.mkdirs()
        
        try {
            file.outputStream().use { os ->
                btrService.downloadToStream(pdfId, os, token) { _, _ -> }
            }
            val now = System.currentTimeMillis()
            val hlc = hlcGenerator.generate()
            lectureDao.update(lecture.copy(isPdfDownloaded = true, updatedAt = now, hlcTimestamp = hlc))
            logger.d("LectureSync", "Downloaded PDF: ${lecture.name}")
        } catch (e: Exception) {
            if (file.exists()) file.delete()
            throw e
        }
    }

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
