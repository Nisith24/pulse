package com.pulse.data.drive

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long? = null,
    val parentName: String? = null
)
