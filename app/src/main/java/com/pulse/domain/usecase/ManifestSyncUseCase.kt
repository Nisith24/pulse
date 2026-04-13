package com.pulse.domain.usecase

import com.pulse.data.repository.manifest.ManifestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

class ManifestSyncUseCase(
    private val manifestRepository: ManifestRepository
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend operator fun invoke() {
        if (_syncState.value is SyncState.Loading) return

        _syncState.value = SyncState.Loading
        val result = manifestRepository.syncManifest()

        when (result) {
            is com.pulse.core.domain.util.Result.Success -> {
                _syncState.value = SyncState.Success
            }
            is com.pulse.core.domain.util.Result.Error -> {
                _syncState.value = SyncState.Error(result.message ?: "Unknown sync error")
            }
            is com.pulse.core.domain.util.Result.Loading -> { } // Should not happen here
        }
    }
}
