package com.pulse.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pulse.core.data.db.Lecture
import com.pulse.core.data.db.Note
import com.pulse.core.data.db.NoteVisual
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.pulse.core.data.db.CustomList
import com.pulse.core.data.db.CustomListLectureCrossRef
import com.pulse.core.data.db.DriveFileEntity
import com.pulse.core.data.db.LectureAnnotation

@Database(
    entities = [Lecture::class, Note::class, NoteVisual::class, CustomList::class, CustomListLectureCrossRef::class, DriveFileEntity::class, LectureAnnotation::class],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lectureDao(): LectureDao
    abstract fun noteDao(): NoteDao
    abstract fun noteVisualDao(): NoteVisualDao
    abstract fun customListDao(): com.pulse.data.db.CustomListDao
    abstract fun driveFileDao(): DriveFileDao
    abstract fun lectureAnnotationDao(): LectureAnnotationDao

    companion object {
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lectures ADD COLUMN category TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE lectures ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE lectures ADD COLUMN manifestDurationSeconds INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE lectures ADD COLUMN dateAdded TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE lectures ADD COLUMN tags TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE lectures ADD COLUMN downloadable INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE lectures ADD COLUMN manifestVersion INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `lecture_annotations` (`lectureId` TEXT NOT NULL, `annotationsJson` TEXT NOT NULL, `hlcTimestamp` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`lectureId`), FOREIGN KEY(`lectureId`) REFERENCES `lectures`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_lecture_annotations_lectureId` ON `lecture_annotations` (`lectureId`)")
            }
        }
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `drive_files` (`id` TEXT NOT NULL, `parentId` TEXT NOT NULL, `name` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `size` INTEGER, `lastSyncedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lectures ADD COLUMN subject TEXT DEFAULT NULL")
            }
        }
        
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `custom_lists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS `custom_list_lectures` (`listId` INTEGER NOT NULL, `lectureId` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`listId`, `lectureId`), FOREIGN KEY(`listId`) REFERENCES `custom_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`lectureId`) REFERENCES `lectures`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_list_lectures_listId` ON `custom_list_lectures` (`listId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_list_lectures_lectureId` ON `custom_list_lectures` (`lectureId`)")
            }
        }
    }
}
