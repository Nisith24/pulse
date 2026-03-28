package com.pulse.core.domain.util

object StringUtils {
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val d = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            d[i][0] = i
        }

        for (j in 0..s2.length) {
            d[0][j] = j
        }

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                d[i][j] = minOf(
                    d[i - 1][j] + 1,
                    d[i][j - 1] + 1,
                    d[i - 1][j - 1] + cost
                )
            }
        }
        return d[s1.length][s2.length]
    }

    fun similarityScore(s1: String, s2: String): Double {
        val s1Lower = s1.lowercase()
        val s2Lower = s2.lowercase()
        val maxLength = maxOf(s1Lower.length, s2Lower.length)
        if (maxLength == 0) return 1.0
        val distance = levenshteinDistance(s1Lower, s2Lower)
        return 1.0 - (distance.toDouble() / maxLength.toDouble())
    }
}
