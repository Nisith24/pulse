package com.pulse.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Lecture::class, Note::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao
    abstract fun noteDao(): NoteDao
}
