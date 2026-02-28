package com.pulse.presentation.lecture

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.pulse.data.local.FileStorageManager
import com.pulse.core.domain.util.Constants
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pulse.presentation.lecture.videoengine.AudioEngine
import com.pulse.presentation.lecture.videoengine.PlayerOptimizer
import com.pulse.presentation.lecture.videoengine.LectureVideoEffect

class PlayerProvider(private val context: Context, fileStorageManager: FileStorageManager) {

    private val audioEngine = AudioEngine()

    private val cache = SimpleCache(
        fileStorageManager.videoCacheDir,
        LeastRecentlyUsedCacheEvictor(Constants.VIDEO_CACHE_SIZE),
        StandaloneDatabaseProvider(context)
    )

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Pulse/1.0 (Android)")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(15000)

    private val dataSourceFactory = DefaultDataSource.Factory(
        context,
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
    )

    private val trackSelector = DefaultTrackSelector(context).apply {
        val parameters = buildUponParameters()
            .setAllowVideoMixedMimeTypeAdaptiveness(true)
            .setAllowAudioMixedMimeTypeAdaptiveness(true)
            .build()
        setParameters(parameters)
    }

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setLoadControl(PlayerOptimizer.createLoadControl())
        .setRenderersFactory(PlayerOptimizer.createRenderersFactory(context))
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build().apply {
            // Apply Audio Engine when session ID changes
            addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    super.onAudioSessionIdChanged(audioSessionId)
                    audioEngine.attachToSession(audioSessionId)
                }
            })
            // Apply Custom GPU Shader
            try {
                setVideoEffects(listOf(LectureVideoEffect()))
            } catch (e: Exception) {
                Log.e("PlayerProvider", "Video effects not supported", e)
            }
        }

    private val mediaSession = MediaSession.Builder(context, player).build()

    private var currentSessionId: String? = null

    // Mini-player state - using StateFlow for reactivity
    private val _miniPlayerLectureId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val _miniPlayerTitle = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val miniPlayerLectureId: String? get() = _miniPlayerLectureId.value
    val miniPlayerTitle: String? get() = _miniPlayerTitle.value
    val miniPlayerFlow: kotlinx.coroutines.flow.StateFlow<String?> = _miniPlayerLectureId
    val isMiniPlayerActive: Boolean get() = _miniPlayerLectureId.value != null

    fun activateMiniPlayer(lectureId: String, title: String) {
        _miniPlayerLectureId.value = lectureId
        _miniPlayerTitle.value = title
    }

    fun closeMiniPlayer() {
        val id = _miniPlayerLectureId.value
        _miniPlayerLectureId.value = null
        _miniPlayerTitle.value = null
        id?.let { stopSession(it) }
    }

    fun clearMiniPlayerState() {
        _miniPlayerLectureId.value = null
        _miniPlayerTitle.value = null
    }

    fun getContext(): Context = context

    private var currentUrl: String? = null
    
    /**
     * Prepares the player for a new session.
     * Clears previous state to ensure no "leaks" between videos.
     */
    fun prepareSession(sessionId: String, url: String, token: String?, title: String, seekTo: Long = 0, speed: Float = 1f, fileId: String? = null) {
        try {
            Log.d("PlayerProvider", "Preparing session: $sessionId, same: ${currentUrl == url}")
            
            if (currentUrl == url && player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED) {
                Log.d("PlayerProvider", "URL already playing, skipping prepare")
                currentSessionId = sessionId
                return
            }

            if (token != null) {
                httpDataSourceFactory.setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
            } else {
                httpDataSourceFactory.setDefaultRequestProperties(emptyMap())
            }
            
            // Critical: Ensure player is stopped before changing media
            player.stop()
            player.clearMediaItems()
            
            currentSessionId = sessionId
            currentUrl = url
            
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val mediaUri = if (url.startsWith("/") || url.startsWith("content://") || url.startsWith("file://")) {
                    if (url.startsWith("/")) Uri.fromFile(java.io.File(url)) else Uri.parse(url)
                } else {
                    Uri.parse(url)
                }

                val mediaItemBuilder = MediaItem.Builder()
                    .setUri(mediaUri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist("PULSE")
                            .build()
                    )

                player.setMediaItem(mediaItemBuilder.build())
                player.setPlaybackSpeed(speed)
                
                if (seekTo > 0) {
                    player.seekTo(seekTo)
                }
                
                player.prepare()
                player.playWhenReady = true // Auto-play when ready
                Log.d("PlayerProvider", "Session $sessionId prepared at $seekTo")
            }

        } catch (e: Exception) {
            Log.e("PlayerProvider", "Failed to prepare session", e)
        }
    }

    fun isSessionActive(sessionId: String): Boolean = currentSessionId == sessionId

    fun stopSession(sessionId: String) {
        if (currentSessionId != sessionId) return
        Log.d("PlayerProvider", "Stopping session: $currentSessionId")
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        currentSessionId = null
        currentUrl = null
    }

    fun release() {
        Log.d("PlayerProvider", "Releasing player resources")
        audioEngine.release()
        mediaSession.release()
        player.release()
        cache.release()
    }
}
