package com.pulse.data.repository

import com.pulse.data.db.Note
import com.pulse.data.db.NoteDao
import com.pulse.domain.repository.INoteRepository
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) : INoteRepository {
    override fun getNotes(lectureId: String): Flow<List<Note>> = noteDao.getNotesForLecture(lectureId)
    override suspend fun insertNote(note: Note) = noteDao.insert(note)
    override suspend fun deleteNote(noteId: Long) = noteDao.delete(noteId)
}
