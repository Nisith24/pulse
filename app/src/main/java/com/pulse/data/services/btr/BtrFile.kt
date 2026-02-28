package com.pulse.data.services.btr

data class BtrFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long? = null,
    val parentName: String? = null
)
