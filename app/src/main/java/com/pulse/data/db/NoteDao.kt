package com.pulse.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import com.pulse.core.data.db.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE lectureId = :lectureId AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getNotesForLecture(lectureId: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note)

    @Query("UPDATE notes SET isDeleted = 1, hlcTimestamp = :hlcTimestamp WHERE id = :noteId")
    suspend fun markDeleted(noteId: Long, hlcTimestamp: String)

    @Query("SELECT * FROM notes WHERE hlcTimestamp > :since")
    suspend fun getUpdatesSince(since: String): List<Note>
}
