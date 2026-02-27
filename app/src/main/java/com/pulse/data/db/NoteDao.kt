package com.pulse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE lectureId = :lectureId ORDER BY timestamp ASC")
    fun getNotesForLecture(lectureId: String): Flow<List<Note>>

    @Insert
    suspend fun insert(note: Note)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun delete(noteId: Long)
}
