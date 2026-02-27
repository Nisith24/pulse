package com.pulse.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileStorageManager(private val context: Context) {
    
    val pdfDir: File = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
    val videoCacheDir: File = File(context.filesDir, "cache/video_cache").apply { if (!exists()) mkdirs() }

    suspend fun getPdfPath(lectureId: String): String? = withContext(Dispatchers.IO) {
        val file = File(pdfDir, "$lectureId.pdf")
        if (file.exists()) file.absolutePath else null
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        videoCacheDir.deleteRecursively()
        videoCacheDir.mkdirs()
    }
}
