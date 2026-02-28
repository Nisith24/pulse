package com.pulse.core.data.repository

import com.pulse.core.data.db.Note
import com.pulse.data.db.NoteDao
import com.pulse.core.domain.repository.INoteRepository
import com.pulse.core.domain.util.HlcGenerator
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val hlcGenerator: HlcGenerator
) : INoteRepository {
    override fun getNotes(lectureId: String): Flow<List<Note>> = noteDao.getNotesForLecture(lectureId)
    
    override suspend fun insertNote(note: Note) {
        val noteWithHlc = note.copy(
            hlcTimestamp = hlcGenerator.generate()
        )
        noteDao.insert(noteWithHlc)
    }
    
    override suspend fun deleteNote(noteId: Long) {
        noteDao.markDeleted(noteId, hlcGenerator.generate())
    }

    override suspend fun getUpdatesSince(since: String): List<Note> {
        return noteDao.getUpdatesSince(since)
    }
}
