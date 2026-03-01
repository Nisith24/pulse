package com.pulse.domain.usecase

import com.pulse.domain.services.btr.IBtrAuthManager
import com.pulse.domain.services.btr.IBtrService

class GetLectureStreamUrlUseCase(
    private val btrService: IBtrService,
    private val authManager: IBtrAuthManager
) {
    suspend operator fun invoke(videoId: String?): Pair<String, String?>? {
        if (videoId == null) return null
        
        val token = authManager.getToken()
        return Pair(btrService.streamUrl(videoId), token)
    }

    suspend fun invalidateToken() {
        try {
            val oldToken = authManager.getToken()
            authManager.clearToken(oldToken)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
