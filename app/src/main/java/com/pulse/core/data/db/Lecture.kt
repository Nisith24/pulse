package com.pulse.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey val id: String,
    val name: String,
    val videoId: String? = null,
    val pdfId: String? = null,
    val pdfLocalPath: String = "",
    val videoLocalPath: String? = null,
    val isPdfDownloaded: Boolean = false,
    val isLocal: Boolean = false,
    val lastPosition: Long = 0,
    val videoDuration: Long = 0,
    val speed: Float = 1.0f,
    val isFavorite: Boolean = false,
    val pdfPageCount: Int = 0,
    val hlcTimestamp: String = "",
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
