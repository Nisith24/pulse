package com.pulse.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Manifest(
    val version: Int,
    val lastUpdated: String,
    val lectures: List<ManifestLecture>
)

@Serializable
data class ManifestLecture(
    val id: String,
    val title: String,
    val category: String,
    val subject: String? = null,
    val videoFileId: String? = null,
    val pdfFileId: String? = null,
    val order: Int = 0,
    val durationSeconds: Int = 0,
    val dateAdded: String? = null,
    val tags: List<String> = emptyList(),
    val downloadable: Boolean = true
)
