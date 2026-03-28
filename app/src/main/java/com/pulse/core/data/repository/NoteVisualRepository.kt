package com.pulse.core.data.repository

import com.pulse.core.data.db.NoteVisual
import com.pulse.data.db.NoteVisualDao
import com.pulse.core.domain.util.HlcGenerator
import kotlinx.coroutines.flow.Flow

class NoteVisualRepository(
    private val dao: NoteVisualDao,
    private val hlcGenerator: HlcGenerator
) {
    fun getVisualsForFile(lectureId: String, pdfId: String): Flow<List<NoteVisual>> {
        return dao.getVisualsForFile(lectureId, pdfId)
    }

    suspend fun insert(visual: NoteVisual) {
        val visualWithHlc = visual.copy(
            hlcTimestamp = hlcGenerator.generate(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insert(visualWithHlc)
    }

    suspend fun delete(id: Long) {
        dao.markDeleted(id, hlcGenerator.generate())
    }

    suspend fun getUpdatesSince(since: String): List<NoteVisual> {
        return dao.getUpdatesSince(since)
    }

    /** Migrate old annotations to use lectureId as pdfId (fixes volatile key issue) */
    suspend fun migrateToLectureId(lectureId: String) {
        dao.migrateToLectureId(lectureId)
        dao.migrateBlankNotesToLectureId(lectureId)
    }
}
