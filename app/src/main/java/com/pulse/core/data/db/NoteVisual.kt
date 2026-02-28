package com.pulse.core.data.db

import androidx.room.*

/**
 * NoteVisual stores complex visual annotations (drawings, bounding boxes)
 * mapped to a video timestamp and a PDF page.
 */
@Entity(
    tableName = "note_visuals",
    foreignKeys = [
        ForeignKey(
            entity = Lecture::class,
            parentColumns = ["id"],
            childColumns = ["lectureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lectureId"), Index("pdfId")]
)
data class NoteVisual(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lectureId: String,
    val pdfId: String = "", // Map to local URI or Drive fileId
    val timestamp: Long, // Video timestamp
    val pageNumber: Int, // PDF page number
    val type: VisualType, // DRAWING, HIGHLIGHT, BOX
    val data: String, // SVG path, JSON coordinates (normalized 0..1), or serialized drawing data
    val color: Int, // ARGB color
    val strokeWidth: Float = 5f,
    val alpha: Float = 1f,
    val hlcTimestamp: String = "",
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class VisualType {
    DRAWING,
    HIGHLIGHT,
    BOX,
    ERASER
}
