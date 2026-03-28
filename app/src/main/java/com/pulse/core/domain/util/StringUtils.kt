package com.pulse.core.domain.util

object StringUtils {
    /**
     * Calculates the Levenshtein distance between two strings.
     * Useful for fuzzy matching file names.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val str1 = s1.lowercase().trim()
        val str2 = s2.lowercase().trim()

        if (str1 == str2) return 0
        if (str1.isEmpty()) return str2.length
        if (str2.isEmpty()) return str1.length

        val v0 = IntArray(str2.length + 1)
        val v1 = IntArray(str2.length + 1)

        for (i in 0..str2.length) {
            v0[i] = i
        }

        for (i in 0 until str1.length) {
            v1[0] = i + 1
            for (j in 0 until str2.length) {
                val cost = if (str1[i] == str2[j]) 0 else 1
                v1[j + 1] = minOf(v1[j] + 1, v0[j + 1] + 1, v0[j] + cost)
            }
            for (j in 0..str2.length) {
                v0[j] = v1[j]
            }
        }
        return v0[str2.length]
    }

    /**
     * Calculates a similarity score between 0.0 and 1.0.
     * 1.0 means exact match.
     */
    fun similarityScore(s1: String, s2: String): Float {
        val str1 = s1.lowercase().trim()
        val str2 = s2.lowercase().trim()

        if (str1 == str2) return 1.0f
        if (str1.isEmpty() || str2.isEmpty()) return 0.0f

        // Bonus: if one contains the other entirely, give it a high score
        if (str1.contains(str2) || str2.contains(str1)) {
            val ratio = minOf(str1.length, str2.length).toFloat() / maxOf(str1.length, str2.length).toFloat()
            // Boost containment score but keep it below exact match
            return 0.8f + (ratio * 0.19f)
        }

        val maxLength = maxOf(str1.length, str2.length)
        if (maxLength == 0) return 1.0f

        val distance = levenshteinDistance(str1, str2)
        return 1.0f - (distance.toFloat() / maxLength)
    }
}
