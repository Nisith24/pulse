package com.pulse.presentation.lecture

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.pulse.data.local.FileStorageManager
import com.pulse.utils.Constants

class PlayerProvider(context: Context, fileStorageManager: FileStorageManager) {

    private val cache = SimpleCache(
        fileStorageManager.videoCacheDir,
        LeastRecentlyUsedCacheEvictor(Constants.VIDEO_CACHE_SIZE),
        StandaloneDatabaseProvider(context)
    )

    // A unified DataSourceFactory that handles http (with cache) and content/file schemes
    private val dataSourceFactory = DefaultDataSource.Factory(
        context,
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent("Pulse/1.0 (Android)")
                    .setAllowCrossProtocolRedirects(true)
            )
    )

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    5000,    // min buffer ms
                    50000,   // max buffer ms
                    1500,    // play buffer ms
                    2500     // rebuffer ms
                )
                .build()
        )
        .setMediaSourceFactory(
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
        )
        .build().apply {
            addAnalyticsListener(androidx.media3.exoplayer.util.EventLogger())
        }

    private var currentUrl: String? = null

    fun prepare(url: String) {
        if (currentUrl == url) return
        currentUrl = url
        
        val uri = Uri.parse(url)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .build()
            
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }
    
    fun stop() {
        player.stop()
        player.clearMediaItems()
        currentUrl = null
    }

    fun release() {
        player.release()
    }
}
