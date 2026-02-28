package com.pulse.core.domain.repository

import com.pulse.core.data.db.Note
import kotlinx.coroutines.flow.Flow

interface INoteRepository {
    fun getNotes(lectureId: String): Flow<List<Note>>
    suspend fun insertNote(note: Note)
    suspend fun deleteNote(noteId: Long)
    suspend fun getUpdatesSince(since: String): List<Note>
}
