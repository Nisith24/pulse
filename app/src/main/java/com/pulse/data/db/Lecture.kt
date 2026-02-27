package com.pulse.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lectures")
data class Lecture(
    @PrimaryKey val id: String,
    val name: String,
    val videoId: String?,
    val pdfId: String?,
    val pdfLocalPath: String,
    val videoLocalPath: String?,
    val isPdfDownloaded: Boolean = false,
    val isLocal: Boolean = false,
    val lastPosition: Long = 0,
    val videoDuration: Long = 0,
    val lastPage: Int = 0,
    val speed: Float = 1f,
    val updatedAt: Long = System.currentTimeMillis()
)
