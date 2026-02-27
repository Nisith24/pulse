package com.pulse.domain.util

interface IFileTypeDetector {
    fun isVideo(mimeType: String, fileName: String): Boolean
    fun isPdf(mimeType: String, fileName: String): Boolean
    fun isGenericName(fileName: String): Boolean
}

class DefaultFileTypeDetector : IFileTypeDetector {
    override fun isVideo(mimeType: String, fileName: String): Boolean {
        val nameLower = fileName.lowercase()
        return mimeType.lowercase().contains("video") || 
            nameLower.endsWith(".mp4") || 
            nameLower.endsWith(".mkv") || 
            nameLower.endsWith(".mov") || 
            nameLower.endsWith(".avi") ||
            nameLower.endsWith(".webm")
    }

    override fun isPdf(mimeType: String, fileName: String): Boolean {
        return mimeType.lowercase().contains("pdf") || fileName.lowercase().endsWith(".pdf")
    }
    
    override fun isGenericName(fileName: String): Boolean {
        val baseFileName = if (fileName.contains(".")) fileName.substringBeforeLast(".") else fileName
        return baseFileName.lowercase() in listOf("video", "lecture", "main", "notes", "pdf", "output", "recording")
    }
}
