package com.pulse.domain.repository

import com.pulse.data.db.Note
import kotlinx.coroutines.flow.Flow

interface INoteRepository {
    fun getNotes(lectureId: String): Flow<List<Note>>
    suspend fun insertNote(note: Note)
    suspend fun deleteNote(noteId: Long)
}
