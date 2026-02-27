package com.pulse.domain.service

import com.pulse.data.drive.DriveFile

interface IDriveService {
    suspend fun listFolder(folderId: String, accessToken: String, recursive: Boolean = true): List<DriveFile>
    fun streamUrl(fileId: String): String
    suspend fun downloadFile(fileId: String, accessToken: String): ByteArray
}

interface IDriveAuthManager {
    suspend fun getToken(): String
}
