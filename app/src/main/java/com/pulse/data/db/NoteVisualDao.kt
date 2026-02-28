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
}
