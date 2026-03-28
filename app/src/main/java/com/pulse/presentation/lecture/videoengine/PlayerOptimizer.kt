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
                15_000,   // min buffer: 15s (was 30s) — fast start
                50_000,   // max buffer: 50s (was 120s) — enough ahead without memory pressure
                1_500,    // rebuffer resume
                2_000     // initial start
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(15_000, true) // 15s back-buffer (was 30s)
            .build()
    }

    /**
     * Standard renderers factory:
     * - Default mode to prevent MediaCodec and software rendering errors
     * - Enable decoder fallback for resilience
     */
    fun createRenderersFactory(context: Context): RenderersFactory {
        return DefaultRenderersFactory(context).apply {
            setEnableDecoderFallback(true)
        }
    }
}
