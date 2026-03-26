package com.pulse.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A single, consolidated file-like record containing all visual annotations
 * for a specific lecture. This replaces the fragmented row-by-row `note_visuals`
 * storage, acting as the "1 single file" requested by the user.
 *
 * It binds strictly to `lectureId` to ensure annotations "display always",
 * immune to the bug where re-downloaded PDFs change their volatile `pdfId`.
 */
@Serializable
@Entity(
    tableName = "lecture_annotations",
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
data class LectureAnnotation(
    @PrimaryKey val lectureId: String,
    // JSON-serialized list of all NoteVisuals for this lecture
    val annotationsJson: String,
    val hlcTimestamp: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
