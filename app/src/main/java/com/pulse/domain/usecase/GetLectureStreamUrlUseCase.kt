package com.pulse.domain.usecase

import com.pulse.domain.service.IDriveAuthManager
import com.pulse.domain.service.IDriveService

class GetLectureStreamUrlUseCase(
    private val driveService: IDriveService,
    private val authManager: IDriveAuthManager
) {
    suspend operator fun invoke(videoId: String?): String? {
        if (videoId == null) return null
        val token = try { authManager.getToken() } catch (_: Exception) { null }
        return if (token != null) {
            driveService.streamUrl(videoId) + "&access_token=$token"
        } else {
            driveService.streamUrl(videoId)
        }
    }
}
