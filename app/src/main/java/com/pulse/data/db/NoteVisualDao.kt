package com.pulse.data.db

import androidx.room.*
import com.pulse.core.data.db.NoteVisual
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteVisualDao {
    @Query("SELECT * FROM note_visuals WHERE lectureId = :lectureId AND pdfId = :pdfId AND isDeleted = 0")
    fun getVisualsForFile(lectureId: String, pdfId: String): Flow<List<NoteVisual>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visual: NoteVisual)

    @Update
    suspend fun update(visual: NoteVisual)

    @Query("UPDATE note_visuals SET isDeleted = 1, hlcTimestamp = :hlcTimestamp WHERE id = :id")
    suspend fun markDeleted(id: Long, hlcTimestamp: String)

    @Query("SELECT * FROM note_visuals WHERE hlcTimestamp > :since")
    suspend fun getUpdatesSince(since: String): List<NoteVisual>

    @Transaction
    suspend fun insertWithCrdt(visual: NoteVisual) {
        val existing = getByIdSync(visual.id)
        if (existing == null) {
            insert(visual)
        } else {
            // CRDT Merge: compare HLC timestamps
            val comparison = com.pulse.core.domain.util.HlcGenerator.compare(visual.hlcTimestamp, existing.hlcTimestamp)
            if (comparison > 0) {
                // Incoming is newer
                insert(visual)
            }
        }
    }

    @Query("SELECT * FROM note_visuals WHERE id = :id")
    suspend fun getByIdSync(id: Long): NoteVisual?

    @Query("SELECT * FROM note_visuals WHERE hlcTimestamp > :since AND isDeleted = 0")
    fun observeUpdatesSince(since: String): Flow<List<NoteVisual>>

    @Query("SELECT * FROM note_visuals WHERE updatedAt > :since")
    suspend fun getModifiedSince(since: Long): List<NoteVisual>

    @Query("SELECT * FROM note_visuals")
    suspend fun getAllNoteVisualsAsList(): List<NoteVisual>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(visuals: List<NoteVisual>)

    /** Migrate all existing visuals for a lecture to use lectureId as pdfId (skips blank_note) */
    @Query("UPDATE note_visuals SET pdfId = :lectureId WHERE lectureId = :lectureId AND pdfId != 'blank_note' AND pdfId != :lectureId")
    suspend fun migrateToLectureId(lectureId: String)

    /** Get ALL visuals for a lecture (ignoring pdfId) — used as fallback */
    @Query("SELECT * FROM note_visuals WHERE lectureId = :lectureId AND isDeleted = 0")
    fun getAllVisualsForLecture(lectureId: String): Flow<List<NoteVisual>>
}
