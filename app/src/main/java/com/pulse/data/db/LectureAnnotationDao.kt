package com.pulse.data.db

import androidx.room.*
import com.pulse.core.data.db.LectureAnnotation
import kotlinx.coroutines.flow.Flow

/**
 * Replaces the fragmented `NoteVisualDao` with a single-file document approach.
 * We fetch and update the ENTIRE annotation layer for a lecture at once.
 */
@Dao
interface LectureAnnotationDao {
    @Query("SELECT * FROM lecture_annotations WHERE lectureId = :lectureId")
    fun getAnnotationsForLecture(lectureId: String): Flow<LectureAnnotation?>

    @Query("SELECT * FROM lecture_annotations WHERE lectureId = :lectureId")
    suspend fun getAnnotationsSync(lectureId: String): LectureAnnotation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(annotation: LectureAnnotation)

    @Query("SELECT * FROM lecture_annotations WHERE updatedAt > :since")
    suspend fun getUpdatesSince(since: Long): List<LectureAnnotation>

    @Transaction
    suspend fun insertWithCrdt(annotation: LectureAnnotation) {
        val existing = getAnnotationsSync(annotation.lectureId)
        if (existing == null) {
            insertOrUpdate(annotation)
        } else {
            // CRDT Merge: compare HLC timestamps to prevent overwriting newer annotations from the cloud
            val comparison = com.pulse.core.domain.util.HlcGenerator.compare(annotation.hlcTimestamp, existing.hlcTimestamp)
            if (comparison > 0) {
                // Incoming is newer
                insertOrUpdate(annotation)
            }
        }
    }
}
