package com.pulse.presentation.lecture.videoengine

import android.media.audiofx.Equalizer
import android.os.Build
import android.util.Log

class AudioEngine {
    private var equalizer: Equalizer? = null

    fun attachToSession(sessionId: Int) {
        release()
        try {
            // Apply standard Equalizer for older devices / primary broad shaping
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
                val numBands = numberOfBands
                val lowerEqBandFreqs = getBand(60_000) // 60 Hz 
                if (numBands > 0) {
                    // Try to map bands manually. Standard 5-band: 60, 230, 910, 3.6k, 14k
                    for (i in 0 until numBands) {
                        val freqCenter = getCenterFreq(i.toShort())
                        var level: Short = 0
                        // AGGRESSIVE HPF at 80-100Hz (Remove mic rumble and desk thumps)
                        if (freqCenter < 120_000) {
                            level = -2000 // -20 dB
                        }
                        // Vocal body 1kHz
                        else if (freqCenter in 800_000..1500_000) {
                            level = 100 // +1.0 dB
                        }
                        // Increase Presence 3 - 5 kHz (Articulation boost)
                        else if (freqCenter in 2500_000..5000_000) {
                            level = 350 // +3.5 dB (Very clear vocal consonants)
                        }
                        // High Air
                        else if (freqCenter > 8000_000) {
                            level = 100 // +1.0 dB
                        }
                        setBandLevel(i.toShort(), level)
                    }
                }
            }
            Log.d("AudioEngine", "Audio engine attached to session $sessionId with lecture-optimized EQ.")
        } catch (e: Exception) {
            Log.e("AudioEngine", "Failed to attach Audio FX: ${e.message}")
        }
    }

    fun release() {
        try {
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            // Ignored
        }
    }
}
