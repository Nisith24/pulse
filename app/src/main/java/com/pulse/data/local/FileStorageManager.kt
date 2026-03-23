package com.pulse.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileStorageManager(private val context: Context) {
    
    val pdfDir: File = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
    val videoCacheDir: File = File(context.filesDir, "cache/video_cache").apply { if (!exists()) mkdirs() }

    companion object {
        /** Maximum total size for cached PDFs: 3 GB */
        private const val PDF_CACHE_MAX_BYTES = 3L * 1024 * 1024 * 1024
    }

    suspend fun getPdfPath(lectureId: String): String? = withContext(Dispatchers.IO) {
        val file = File(pdfDir, "$lectureId.pdf")
        if (file.exists()) file.absolutePath else null
    }

    /** Get the final destination path for a cached Drive PDF */
    fun getPdfFinalPath(fileId: String): String = File(pdfDir, "$fileId.pdf").absolutePath

    /** Get the temporary download path (.tmp) for a Drive PDF */
    fun getPdfTmpPath(fileId: String): String = File(pdfDir, "$fileId.pdf.tmp").absolutePath

    /** Evict oldest PDFs until total size is under the 3 GB limit */
    suspend fun evictPdfCacheIfNeeded() = withContext(Dispatchers.IO) {
        val files = pdfDir.listFiles() ?: return@withContext
        var totalSize = files.sumOf { it.length() }
        if (totalSize <= PDF_CACHE_MAX_BYTES) return@withContext

        // Sort by last modified (oldest first) for LRU eviction
        val sorted = files.sortedBy { it.lastModified() }
        for (file in sorted) {
            if (totalSize <= PDF_CACHE_MAX_BYTES) break
            val size = file.length()
            if (file.delete()) {
                totalSize -= size
            }
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        videoCacheDir.deleteRecursively()
        videoCacheDir.mkdirs()
    }

    suspend fun clearPdfCache() = withContext(Dispatchers.IO) {
        pdfDir.deleteRecursively()
        pdfDir.mkdirs()
    }

    /**
     * Copy a SAF content:// URI PDF into internal storage with a stable lectureId-based filename.
     * Returns the stable internal file path, or null on failure.
     */
    suspend fun copyContentUriToInternal(contentUri: android.net.Uri, lectureId: String): String? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(pdfDir, "local_$lectureId.pdf")
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
