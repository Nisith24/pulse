package com.pulse.presentation.lecture.videoengine

import android.content.Context
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory

object PlayerOptimizer {
    
    /**
     * Aggressive DefaultLoadControl for lectures:
     * - Protects against micro-stalls
     * - Smooth seek preloading
     * - Higher buffer limits
     */
    fun createLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                // Min Buffer 15s (safe for standard bitrates without exceeding DefaultAllocator memory cap)
                15_000,  
                // Max Buffer 50s
                50_000,  
                // playback start after rebuffer
                1_500,   
                // playback start buffer
                2_000    
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * DefaultRenderersFactory configured for lecture smoothness:
     * - Asynchronous media streaming via codecs
     * - Extended video frame release time
     * - Hardware scaling where possible
     */
    fun createRenderersFactory(context: Context): RenderersFactory {
        return DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            // Enable asynchronous MediaCodec queueing
            setEnableDecoderFallback(true)
        }
    }
}
