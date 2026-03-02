package com.pulse.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pulse.core.data.db.Lecture
import com.pulse.core.data.db.Note
import com.pulse.core.data.db.NoteVisual
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Lecture::class, Note::class, NoteVisual::class], version = 13, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao
    abstract fun noteDao(): NoteDao
    abstract fun noteVisualDao(): NoteVisualDao

    companion object {
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lectures ADD COLUMN subject TEXT DEFAULT NULL")
            }
        }
    }
}
