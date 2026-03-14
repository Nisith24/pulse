package com.pulse.domain.usecase

import com.pulse.domain.services.btr.IBtrAuthManager
import com.pulse.domain.services.btr.IBtrService

class GetLectureStreamUrlUseCase(
    private val btrService: IBtrService,
    private val authManager: IBtrAuthManager
) {
    suspend operator fun invoke(videoId: String?): Pair<String, String?>? {
        if (videoId == null) return null

        // Try to get auth token; for publicly shared Drive folders (e.g. Microbiology)
        // a null/empty token still allows streaming — no sign-in required
        val token = try { authManager.getToken() } catch (e: Exception) { null }
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
