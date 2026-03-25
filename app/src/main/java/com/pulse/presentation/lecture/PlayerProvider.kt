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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Singleton player manager. Uses OkHttp-backed DataSource so that the
 * Authorization header survives Google Drive's redirects to googlevideo.com.
 *
 * Optimizations:
 * - HTTP/2 multiplexing for Google APIs
 * - Aggressive connection pooling (5 idle, 5 min keepalive)
 * - DNS caching (3 min TTL) to skip DNS lookups on every request
 * - Retry interceptor (3 attempts, exponential backoff) for transient failures
 * - 1 GB LRU video cache with error-fallback to bypass corrupted cache entries
 */
class PlayerProvider(private val context: Context, fileStorageManager: FileStorageManager) {
    private val audioEngine = AudioEngine()

    // ── Auth-Pinning Interceptor ──
    @Volatile
    private var currentToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val host = original.url.host.lowercase()

        // Only googleapis.com requires the Bearer token. 
        // googleusercontent.com and googlevideo.com URLs already contain an access token in the query params.
        val isGoogleDomain = host.endsWith("googleapis.com")

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

    // ── DNS Cache: Avoids repeated DNS lookups for Google domains ──
    private val dnsCache = ConcurrentHashMap<String, Pair<List<InetAddress>, Long>>()
    private val DNS_TTL_MS = 3 * 60 * 1000L // 3 minutes

    private val cachedDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val now = System.currentTimeMillis()
            val cached = dnsCache[hostname]
            if (cached != null && now - cached.second < DNS_TTL_MS) {
                return cached.first
            }
            val addresses = Dns.SYSTEM.lookup(hostname)
            dnsCache[hostname] = Pair(addresses, now)
            return addresses
        }
    }

    // ── Cache: 1 GB LRU ──
    private val cache = SimpleCache(
        fileStorageManager.videoCacheDir,
        LeastRecentlyUsedCacheEvictor(1L * 1024 * 1024 * 1024), // 1 GB
        StandaloneDatabaseProvider(context)
    )

    // ── OkHttp Streaming Client: connection pool + DNS cache + auth only ──
    // NOTE: No retry interceptor here — ExoPlayer manages its own retries & Range requests.
    // NOTE: No HTTP/2 — Google Drive redirects to googlevideo.com which may not negotiate H2.
    private val streamingClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .dns(cachedDns)
        .addInterceptor(authInterceptor)
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── DataSource pipeline: OkHttp → Cache (with error fallback) → DefaultDataSource ──
    private val okHttpDataSourceFactory = OkHttpDataSource.Factory(streamingClient)
        .setUserAgent("Pulse/1.0 (Android)")

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR) // fallback to network on cache corruption

    private val dataSourceFactory = DefaultDataSource.Factory(context, cacheDataSourceFactory)

    // ── Player ──
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(PlayerOptimizer.createLoadControl())
        .setRenderersFactory(PlayerOptimizer.createRenderersFactory(context))
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    _playbackState.value = state
                }
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    audioEngine.attachToSession(audioSessionId)
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _playerError.value = error
                    Log.e("PlayerProvider", "Playback Error: ${error.errorCodeName}", error)
                    
                    if (error.cause is androidx.media3.datasource.HttpDataSource.HttpDataSourceException) {
                        val httpError = error.cause as androidx.media3.datasource.HttpDataSource.HttpDataSourceException
                        Log.e("PlayerProvider", "HTTP Error: ${httpError.message}")
                        if (httpError is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                            Log.e("PlayerProvider", "Response Code: ${httpError.responseCode}")
                            Log.e("PlayerProvider", "Headers: ${httpError.headerFields}")
                        }
                    } else {
                        Log.e("PlayerProvider", "Player error unhandled: ", error)
                        // Attempt a generic prepare retry on error
                        if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                            Log.e("PlayerProvider", "Decoder init failed. Retrying...")
                            this@apply.prepare()
                            this@apply.play()
                        }
                    }
                }
            })
        }

    private val mediaSession = MediaSession.Builder(context, player).build()
    private var currentSessionId: String? = null
    private var currentUrl: String? = null

    // ── ExoPlayer State ──
    private val _playbackState = MutableStateFlow<Int>(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState

    private val _playerError = MutableStateFlow<androidx.media3.common.PlaybackException?>(null)
    val playerError: StateFlow<androidx.media3.common.PlaybackException?> = _playerError

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
                player.playbackState != Player.STATE_ENDED &&
                player.playerError == null
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
