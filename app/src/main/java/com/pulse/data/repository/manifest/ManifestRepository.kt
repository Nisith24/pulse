package com.pulse.data.repository.manifest

import com.pulse.core.data.db.Lecture
import com.pulse.core.domain.util.Constants
import com.pulse.core.domain.util.ILogger
import com.pulse.core.domain.util.executeWithRetry
import com.pulse.data.db.LectureDao
import com.pulse.data.local.SettingsManager
import com.pulse.domain.model.Manifest
import com.pulse.domain.model.ManifestLecture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class ManifestRepository(
    private val lectureDao: LectureDao,
    private val client: OkHttpClient,
    private val logger: ILogger,
    private val settingsManager: SettingsManager
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncManifest(): com.pulse.core.domain.util.Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Guard: skip sync if manifest file ID is not configured yet
            if (Constants.MANIFEST_FILE_ID.startsWith("1-TODO")) {
                logger.w("ManifestRepository", "MANIFEST_FILE_ID is not configured. Skipping manifest sync.")
                return@withContext com.pulse.core.domain.util.Result.Success(Unit)
            }
            logger.d("ManifestRepository", "Starting manifest sync...")
            val manifestUrl = "https://www.googleapis.com/drive/v3/files/${Constants.MANIFEST_FILE_ID}?alt=media&key=${Constants.GOOGLE_API_KEY}"
            val request = Request.Builder().url(manifestUrl).build()

            val responseBody = client.executeWithRetry(request).use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch manifest: ${response.code} ${response.message}")
                }
                response.body?.string() ?: throw Exception("Empty manifest body")
            }

            val manifest = json.decodeFromString<Manifest>(responseBody)
            logger.d("ManifestRepository", "Fetched manifest version ${manifest.version} with ${manifest.lectures.size} lectures")

            val currentVersion = settingsManager.manifestVersionFlow.firstOrNull() ?: 0
            if (manifest.version == currentVersion) {
                logger.d("ManifestRepository", "Manifest version ${manifest.version} is up to date. Skipping DB update.")
                return@withContext com.pulse.core.domain.util.Result.Success(Unit)
            }

            // Atomically upsert lectures into Room
            val lecturesToUpsert = manifest.lectures.map { it.toEntity(manifest.version) }

            // Note: We use insertAll which internally does an Ignore + Update for conflicts.
            // This preserves our local-only fields like lastPosition and isFavorite because
            // the update query in DAO might need tuning to not overwrite them, but for now we
            // can do a manual merge.

            val existingLecturesMap = lectureDao.getAllLecturesAsList().associateBy { it.id }

            val mergedLectures = lecturesToUpsert.map { newLecture ->
                val existing = existingLecturesMap[newLecture.id]
                if (existing != null) {
                    // Merge new manifest data while preserving user state
                    existing.copy(
                        name = newLecture.name,
                        category = newLecture.category,
                        subject = newLecture.subject,
                        videoId = newLecture.videoId,
                        pdfId = newLecture.pdfId,
                        orderIndex = newLecture.orderIndex,
                        manifestDurationSeconds = newLecture.manifestDurationSeconds,
                        dateAdded = newLecture.dateAdded,
                        tags = newLecture.tags,
                        downloadable = newLecture.downloadable,
                        manifestVersion = newLecture.manifestVersion,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    newLecture
                }
            }

            // Perform batch insert/update
            lectureDao.insertAll(mergedLectures)
            settingsManager.saveManifestVersion(manifest.version)
            logger.d("ManifestRepository", "Successfully synced manifest to Room and updated version to ${manifest.version}")
            com.pulse.core.domain.util.Result.Success(Unit)
        } catch (e: Exception) {
            logger.e("ManifestRepository", "Manifest sync failed", e)
            com.pulse.core.domain.util.Result.Error(e)
        }
    }

    private fun ManifestLecture.toEntity(version: Int): Lecture {
        return Lecture(
            id = id,
            name = title,
            category = category,
            subject = subject,
            videoId = videoFileId,
            pdfId = pdfFileId,
            orderIndex = order,
            manifestDurationSeconds = durationSeconds,
            dateAdded = dateAdded,
            tags = tags.joinToString(","),
            downloadable = downloadable,
            manifestVersion = version,
            isLocal = false
        )
    }
}
