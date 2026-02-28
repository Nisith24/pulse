package com.pulse.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pulse.core.data.db.Lecture
import com.pulse.core.data.db.Note
import com.pulse.core.data.db.NoteVisual

@Database(entities = [Lecture::class, Note::class, NoteVisual::class], version = 11, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao
    abstract fun noteDao(): NoteDao
    abstract fun noteVisualDao(): NoteVisualDao
}
