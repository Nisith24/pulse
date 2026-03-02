package com.pulse.presentation.lecture.videoengine

import android.content.Context
import android.os.Build
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory

/**
 * Production-grade player configuration optimized for lecture video quality.
 *
 * Priorities:
 * 1. Maximum decode quality (prefer software decoders for accurate color)
 * 2. Large buffers to prevent micro-stalls during network hiccups
 * 3. Smooth seeking for scrubbing through lectures
 * 4. Extension renderer support (AV1, VP9 software decoders)
 */
object PlayerOptimizer {

    /**
     * Aggressive buffering for lecture streaming:
     * - 30s min buffer: prevents stalls on fluctuating connections
     * - 120s max buffer: preloads ahead for smooth scrubbing
     * - Fast rebuffer recovery (1s)
     * - Quick initial start (1.5s)
     */
    fun createLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,   // min buffer 30s
                120_000,  // max buffer 120s (2 min lookahead)
                1_000,    // rebuffer resume playback
                1_500     // initial playback start
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(30_000, true) // 30s back-buffer for instant backward seek
            .build()
    }

    /**
     * Renderers factory tuned for quality:
     * - Prefer extension decoders (FFmpeg/AV1 software) for accurate color
     * - Enable decoder fallback for resilience
     * - Enable async queueing for smoother frame delivery
     */
    fun createRenderersFactory(context: Context): RenderersFactory {
        return DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
            // Async MediaCodec queueing: smoother frame delivery pipeline
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                forceEnableMediaCodecAsynchronousQueueing()
            }
        }
    }
}
