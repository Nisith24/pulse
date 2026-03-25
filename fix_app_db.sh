#!/bin/bash
sed -i 's/exportSchema = false/exportSchema = false\n)\nabstract class AppDatabase : RoomDatabase() {\n    abstract fun lectureDao(): LectureDao\n    abstract fun noteDao(): NoteDao\n    abstract fun noteVisualDao(): NoteVisualDao\n    abstract fun customListDao(): com.pulse.data.db.CustomListDao\n    abstract fun driveFileDao(): DriveFileDao\n\n    companion object {\n        val MIGRATION_14_15 = object : Migration(14, 15) {\n            override fun migrate(database: SupportSQLiteDatabase) {\n                database.execSQL("CREATE TABLE IF NOT EXISTS `drive_files` (`id` TEXT NOT NULL, `parentId` TEXT NOT NULL, `name` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `size` INTEGER, `lastSyncedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")\n            }\n        }/g' app/src/main/java/com/pulse/data/db/AppDatabase.kt

sed -i 's/import com.pulse.core.data.db.CustomListLectureCrossRef/import com.pulse.core.data.db.CustomListLectureCrossRef\nimport com.pulse.core.data.db.DriveFileEntity/g' app/src/main/java/com/pulse/data/db/AppDatabase.kt
sed -i 's/version = 14,/version = 15,/g' app/src/main/java/com/pulse/data/db/AppDatabase.kt
sed -i 's/entities = \[Lecture::class, Note::class, NoteVisual::class, CustomList::class, CustomListLectureCrossRef::class\],/entities = \[Lecture::class, Note::class, NoteVisual::class, CustomList::class, CustomListLectureCrossRef::class, DriveFileEntity::class\],/g' app/src/main/java/com/pulse/data/db/AppDatabase.kt

# Remove duplicates added by sed
sed -i '/abstract class AppDatabase : RoomDatabase() {/,/customListDao(): com.pulse.data.db.CustomListDao/d' app/src/main/java/com/pulse/data/db/AppDatabase.kt
