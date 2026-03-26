package com.pulse.core.data.repository

import com.pulse.core.data.db.LectureAnnotation
import com.pulse.core.data.db.NoteVisual
import com.pulse.data.db.LectureAnnotationDao
import com.pulse.data.db.NoteVisualDao
import com.pulse.core.domain.util.HlcGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.firstOrNull

class NoteVisualRepository(
    private val dao: NoteVisualDao,
    private val lectureAnnotationDao: LectureAnnotationDao,
    private val hlcGenerator: HlcGenerator
) {
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    fun getVisualsForFile(lectureId: String, pdfId: String): Flow<List<NoteVisual>> {
        // Return from the single file. We ignore pdfId completely for lookup now,
        // fulfilling the "display always" intent.
        return lectureAnnotationDao.getAnnotationsForLecture(lectureId)
            .map { annotation ->
                if (annotation == null || annotation.annotationsJson.isEmpty()) {
                    emptyList()
                } else {
                    try {
                        jsonFormat.decodeFromString<List<NoteVisual>>(annotation.annotationsJson)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            .map { visuals -> visuals.filter { !it.isDeleted } }
    }

    suspend fun insert(visual: NoteVisual) {
        val visualWithHlc = visual.copy(
            id = if (visual.id == 0L) System.currentTimeMillis() else visual.id, // Auto-generate ID if needed
            hlcTimestamp = hlcGenerator.generate(),
            updatedAt = System.currentTimeMillis()
        )

        val existingAnnotation = lectureAnnotationDao.getAnnotationsSync(visual.lectureId)
        val currentVisuals = if (existingAnnotation != null && existingAnnotation.annotationsJson.isNotEmpty()) {
            try {
                jsonFormat.decodeFromString<MutableList<NoteVisual>>(existingAnnotation.annotationsJson)
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        // Remove existing if replacing
        currentVisuals.removeAll { it.id == visualWithHlc.id }
        currentVisuals.add(visualWithHlc)

        val newJson = jsonFormat.encodeToString(currentVisuals)

        lectureAnnotationDao.insertOrUpdate(
            LectureAnnotation(
                lectureId = visual.lectureId,
                annotationsJson = newJson,
                hlcTimestamp = hlcGenerator.generate(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun delete(lectureId: String, id: Long) {
        val existingAnnotation = lectureAnnotationDao.getAnnotationsSync(lectureId)
        if (existingAnnotation != null && existingAnnotation.annotationsJson.isNotEmpty()) {
            val currentVisuals = try {
                jsonFormat.decodeFromString<MutableList<NoteVisual>>(existingAnnotation.annotationsJson)
            } catch (e: Exception) {
                mutableListOf()
            }

            val itemIndex = currentVisuals.indexOfFirst { it.id == id }
            if (itemIndex != -1) {
                val updatedItem = currentVisuals[itemIndex].copy(
                    isDeleted = true,
                    hlcTimestamp = hlcGenerator.generate(),
                    updatedAt = System.currentTimeMillis()
                )
                currentVisuals[itemIndex] = updatedItem

                val newJson = jsonFormat.encodeToString(currentVisuals)
                lectureAnnotationDao.insertOrUpdate(
                    LectureAnnotation(
                        lectureId = lectureId,
                        annotationsJson = newJson,
                        hlcTimestamp = hlcGenerator.generate(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun getUpdatesSince(since: String): List<NoteVisual> {
        // Legacy: Return empty list because sync now handles the whole document via FirestoreSyncManager.
        return emptyList()
    }

    /** Migrate old annotations to use the new single-file JSON structure */
    suspend fun migrateToLectureId(lectureId: String) {
        // Step 1: Ensure legacy DB has stable keys
        dao.migrateToLectureId(lectureId)

        // Step 2: Migrate all rows to the new single file format
        val oldVisualsFlow = dao.getAllVisualsForLecture(lectureId)
        // We just collect the first emission which is the current state
        val oldVisuals = oldVisualsFlow.firstOrNull()

        if (oldVisuals != null && oldVisuals.isNotEmpty()) {
            val existingAnnotation = lectureAnnotationDao.getAnnotationsSync(lectureId)
            // If we already migrated, don't overwrite if it exists and has content
            if (existingAnnotation == null || existingAnnotation.annotationsJson.isEmpty()) {
                val jsonStr = jsonFormat.encodeToString(oldVisuals)
                val newDoc = LectureAnnotation(
                    lectureId = lectureId,
                    annotationsJson = jsonStr,
                    hlcTimestamp = hlcGenerator.generate(),
                    updatedAt = System.currentTimeMillis()
                )
                lectureAnnotationDao.insertOrUpdate(newDoc)
            }
        }
    }
}
