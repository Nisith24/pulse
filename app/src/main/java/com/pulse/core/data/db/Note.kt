package com.pulse.core.data.db

import androidx.room.*

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Lecture::class,
            parentColumns = ["id"],
            childColumns = ["lectureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lectureId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lectureId: String,
    val timestamp: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val hlcTimestamp: String = "",
    val isDeleted: Boolean = false
)
