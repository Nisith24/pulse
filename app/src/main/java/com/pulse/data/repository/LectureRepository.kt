package com.pulse.data.repository

import com.pulse.data.db.Lecture
import com.pulse.data.db.LectureDao
import com.pulse.domain.service.IDriveAuthManager
import com.pulse.domain.service.IDriveService
import com.pulse.data.local.FileStorageManager
import com.pulse.domain.usecase.SyncLecturesUseCase
import com.pulse.domain.util.ILogger
import com.pulse.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LectureRepository(
    private val lectureDao: LectureDao,
    private val driveService: IDriveService,
    private val authManager: IDriveAuthManager,
    private val fileStorage: FileStorageManager,
    private val syncLecturesUseCase: SyncLecturesUseCase,
    private val logger: ILogger
) {
    val driveLectures: Flow<List<Lecture>> = lectureDao.getDriveLectures()
    val localLectures: Flow<List<Lecture>> = lectureDao.getLocalLectures()

    fun getLectureById(id: String): Flow<Lecture?> = lectureDao.getById(id)

    suspend fun addLocalLecture(name: String, videoPath: String?, pdfPath: String?) {
        val lecture = Lecture(
            id = UUID.randomUUID().toString(),
            name = name,
            videoId = null,
            pdfId = null,
            pdfLocalPath = pdfPath ?: "",
            videoLocalPath = videoPath,
            isPdfDownloaded = pdfPath != null,
            isLocal = true,
            updatedAt = System.currentTimeMillis()
        )
        lectureDao.insert(lecture)
    }

    suspend fun updateLectureProgress(lecture: Lecture, currentPosition: Long, duration: Long) {
        val resetThreshold = if (duration > 0) duration.toDouble() * 0.95 else Double.MAX_VALUE
        val finalPosition = if (currentPosition.toDouble() > resetThreshold) 0L else currentPosition
        lectureDao.update(
            lecture.copy(
                lastPosition = finalPosition,
                videoDuration = duration,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateLectureZoom(lecture: Lecture, page: Int) {
         lectureDao.update(
            lecture.copy(
                lastPage = page,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun sync() {
        val token = try { authManager.getToken() } catch (e: Exception) { 
            logger.e("LectureSync", "Failed to get token: ${e.message}")
            throw e 
        }
        logger.d("LectureSync", "Starting sync with folder: ${Constants.DRIVE_FOLDER_ID}")
        val files = driveService.listFolder(Constants.DRIVE_FOLDER_ID, token)
        logger.d("LectureSync", "Found ${files.size} files in Drive folder")

        val grouped = syncLecturesUseCase(files)
        lectureDao.insertAll(grouped)
        lectureDao.deleteMissingDrive(grouped.map { it.id })

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
        val bytes = driveService.downloadFile(pdfId, token)
        val file = File(lecture.pdfLocalPath)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        lectureDao.update(lecture.copy(isPdfDownloaded = true, updatedAt = System.currentTimeMillis()))
        logger.d("LectureSync", "Downloaded PDF: ${lecture.name}")
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        fileStorage.clearCache()
    }

    suspend fun deleteLecture(id: String) {
        lectureDao.deleteById(id)
    }
}
