package com.dylandos.iptv.epg

/**
 * Levenshtein string-distance implementation used by the tertiary fuzzy EPG
 * channel matcher. Operates on a rolling two-row matrix to stay O(m) in memory —
 * important on a 2 GB device when matching thousands of channels.
 */
object Levenshtein {

    /**
     * Normalized similarity in [0,1]. 1.0 means the strings are identical.
     * Works on already-normalized (lowercased, stripped) input.
     */
    fun similarity(a: String, b: String): Float {
        if (a == b) return 1f
        val la = a.length
        val lb = b.length
        if (la == 0 || lb == 0) return 0f

        // Keep the shorter string as the column dimension.
        val s = if (la < lb) a else b
        val t = if (la < lb) b else a
        val n = s.length
        val m = t.length

        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)

        for (j in 1..m) {
            val tj = t[j - 1]
            curr[0] = j
            for (i in 1..n) {
                val cost = if (s[i - 1] == tj) 0 else 1
                val del = prev[i] + 1
                val ins = curr[i - 1] + 1
                val sub = prev[i - 1] + cost
                curr[i] = minOf(del, ins, sub)
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }

        val distance = prev[n]
        val maxLen = m
        return 1f - (distance.toFloat() / maxLen.toFloat())
    }
}
