package com.pulse.domain.usecase

import com.pulse.domain.service.IDriveAuthManager
import com.pulse.domain.service.IDriveService

class GetLectureStreamUrlUseCase(
    private val driveService: IDriveService,
    private val authManager: IDriveAuthManager
) {
    suspend operator fun invoke(videoId: String?, forceRefresh: Boolean = false): String? {
        if (videoId == null) return null
        
        var token: String? = null
        try {
            if (forceRefresh) {
                val oldToken = authManager.getToken()
                authManager.clearToken(oldToken)
            }
            token = authManager.getToken()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return if (token != null) {
            driveService.streamUrl(videoId) + "&access_token=$token"
        } else {
            driveService.streamUrl(videoId)
        }
    }
}
