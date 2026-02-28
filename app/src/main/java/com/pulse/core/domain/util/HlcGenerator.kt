package com.pulse.core.domain.util

import java.util.concurrent.atomic.AtomicLong

/**
 * A basic implementation of Hybrid Logical Clock (HLC)
 * format: <system_time_ms>:<counter>:<node_id>
 */
class HlcGenerator(private val nodeId: String) {
    private var lastTime = 0L
    private val counter = AtomicLong(0)

    fun generate(): String {
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (now > lastTime) {
                lastTime = now
                counter.set(0)
            } else {
                counter.incrementAndGet()
            }
            return "$lastTime:${counter.get()}:$nodeId"
        }
    }
    
    companion object {
        fun compare(ts1: String, ts2: String): Int {
            if (ts1 == ts2) return 0
            if (ts1.isEmpty()) return -1
            if (ts2.isEmpty()) return 1
            
            val parts1 = ts1.split(":")
            val parts2 = ts2.split(":")
            
            // Compare system time
            val timeCompare = parts1[0].toLong().compareTo(parts2[0].toLong())
            if (timeCompare != 0) return timeCompare
            
            // Compare counter
            val counterCompare = parts1[1].toLong().compareTo(parts2[1].toLong())
            if (counterCompare != 0) return counterCompare
            
            // Compare node ID as tie-breaker
            return parts1[2].compareTo(parts2[2])
        }
    }
}
