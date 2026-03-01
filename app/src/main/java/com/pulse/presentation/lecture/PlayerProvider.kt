package com.pulse.presentation.lecture

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.pulse.core.domain.util.Constants
import com.pulse.data.local.FileStorageManager
import com.pulse.presentation.lecture.videoengine.AudioEngine
import com.pulse.presentation.lecture.videoengine.PlayerOptimizer
import com.pulse.presentation.lecture.videoengine.LectureVideoEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Singleton player manager. Uses OkHttp-backed DataSource so that the
 * Authorization header survives Google Drive's redirects to googlevideo.com.
 */
class PlayerProvider(private val context: Context, fileStorageManager: FileStorageManager) {
    private val audioEngine = AudioEngine()

    // ── Auth-Pinning Interceptor ──
    // Keeps the Bearer token on redirects ONLY within Google's own domains.
    @Volatile
    private var currentToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val host = original.url.host.lowercase()

        val isGoogleDomain = host.endsWith("googleapis.com") ||
                host.endsWith("googlevideo.com") ||
                host.endsWith("googleusercontent.com") ||
                host.endsWith("google.com")

        val token = currentToken
        val needsAuth = token != null && isGoogleDomain &&
                original.header("Authorization") == null

        val request = if (needsAuth) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    // ── Cache ──
    private val cache = SimpleCache(
        fileStorageManager.videoCacheDir,
        LeastRecentlyUsedCacheEvictor(Constants.VIDEO_CACHE_SIZE),
        StandaloneDatabaseProvider(context)
    )

    // ── OkHttp client for streaming ──
    private val streamingClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── DataSource pipeline: OkHttp → Cache → DefaultDataSource ──
    private val okHttpDataSourceFactory = OkHttpDataSource.Factory(streamingClient)
        .setUserAgent("Pulse/1.0 (Android)")

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(okHttpDataSourceFactory)

    private val dataSourceFactory = DefaultDataSource.Factory(context, cacheDataSourceFactory)

    // ── Player ──
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(PlayerOptimizer.createLoadControl())
        .setRenderersFactory(PlayerOptimizer.createRenderersFactory(context))
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build().apply {
            addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    audioEngine.attachToSession(audioSessionId)
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    if (error.cause is androidx.media3.common.VideoFrameProcessingException) {
                        Log.e("PlayerProvider", "Falling back to native rendering.", error.cause)
                        setVideoEffects(emptyList())
                        prepare()
                        play()
                    }
                }
            })
            try {
                setVideoEffects(listOf(LectureVideoEffect()))
            } catch (e: Exception) {
                Log.e("PlayerProvider", "Video Engine hardware fallback active.", e)
            }
        }

    private val mediaSession = MediaSession.Builder(context, player).build()
    private var currentSessionId: String? = null
    private var currentUrl: String? = null

    // ── Mini-player state ──
    private val _miniPlayerLectureId = MutableStateFlow<String?>(null)
    private val _miniPlayerTitle = MutableStateFlow<String?>(null)
    val miniPlayerLectureId: String? get() = _miniPlayerLectureId.value
    val miniPlayerTitle: String? get() = _miniPlayerTitle.value
    val miniPlayerFlow: StateFlow<String?> = _miniPlayerLectureId
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

    fun prepareSession(
        sessionId: String,
        url: String,
        token: String?,
        title: String,
        seekTo: Long = 0,
        speed: Float = 1f,
        fileId: String? = null
    ) {
        try {
            if (currentUrl == url && currentSessionId == sessionId &&
                player.playbackState != Player.STATE_IDLE &&
                player.playbackState != Player.STATE_ENDED
            ) return

            player.stop()
            player.clearMediaItems()
            currentSessionId = sessionId
            currentUrl = url
            currentToken = token

            val mediaUri = when {
                url.startsWith("/") -> Uri.fromFile(java.io.File(url))
                url.startsWith("content://") || url.startsWith("file://") -> Uri.parse(url)
                else -> Uri.parse(url)
            }

            val mediaItem = MediaItem.Builder()
                .setUri(mediaUri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist("PULSE").build())
                .build()

            player.setMediaItem(mediaItem)
            player.setPlaybackSpeed(speed)
            if (seekTo > 0) player.seekTo(seekTo)
            player.prepare()
            player.playWhenReady = true
        } catch (e: Exception) {
            Log.e("PlayerProvider", "prepareSession FAILED", e)
        }
    }

    fun isSessionActive(sessionId: String): Boolean = currentSessionId == sessionId

    fun stopSession(sessionId: String) {
        if (currentSessionId != sessionId) return
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        currentSessionId = null
        currentUrl = null
    }

    fun release() {
        audioEngine.release()
        mediaSession.release()
        player.release()
        cache.release()
    }
}
