package com.pulse.domain.services.btr

import com.pulse.data.services.btr.BtrFile

interface IBtrService {
    suspend fun listFolder(folderId: String, accessToken: String, recursive: Boolean = true): List<BtrFile>
    fun streamUrl(fileId: String): String
    suspend fun downloadFile(fileId: String, accessToken: String): ByteArray
    suspend fun downloadToStream(fileId: String, outputStream: java.io.OutputStream, accessToken: String, onProgress: (bytesRead: Long, totalBytes: Long) -> Unit)
}

interface IBtrAuthManager {
    val isSignedIn: Boolean
    val displayName: String?
    val email: String?
    val photoUrl: String?
    
    suspend fun getToken(): String
    suspend fun clearToken(token: String)
    suspend fun signOut()
}
