package com.dylandos.iptv.epg

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-pass EPG channel matcher.
 *
 *  Primary  : exact `tvg-id` match.
 *  Secondary: exact `tvg-name` match.
 *  Tertiary : normalized fuzzy match — strips country prefixes, resolution tags
 *             (HD/4K/FHD), special characters and leading digits, then runs a
 *             Levenshtein distance comparison.
 */
@Singleton
class ChannelMatcher @Inject constructor() {

    companion object {
        private const val FUZZY_THRESHOLD = 0.78f

        private val resolutionTags = Regex("(?i)\\b(4k|uhd|fhd|hd|hdr|hevc|x265|h264|x264|fhd)\\b")
        private val specialChars = Regex("[^a-z0-9]+")
    }

    data class ChannelCandidate(
        val dbId: Long,
        val tvgId: String?,
        val tvgName: String?,
        val displayName: String,
        val providerName: String
    )

    /** Normalize a name for fuzzy matching. */
    fun normalizeName(raw: String): String {
        var n = raw.lowercase()
        n = resolutionTags.replace(n, " ")
        n = specialChars.replace(n, " ")
        n = n.trim()
        // Drop leading numerals (channel numbers like "123 ").
        n = n.replaceFirst(Regex("^\\d+\\s+"), "")
        return n
    }

    /**
     * Resolve an EPG channel to a local DB channel id using the three-pass
     * strategy. Returns the best candidate's DB id, or null if no match.
     */
    fun match(
        epgChannel: EpgChannel,
        candidates: List<ChannelCandidate>
    ): Long? {
        if (candidates.isEmpty()) return null

        // Pass 1: exact tvg-id
        if (epgChannel.id.isNotBlank()) {
            candidates.firstOrNull { it.tvgId?.trim() == epgChannel.id.trim() }?.let {
                return it.dbId
            }
        }

        // Pass 2: exact tvg-name
        val epgName = epgChannel.displayName.trim()
        if (epgName.isNotBlank()) {
            candidates.firstOrNull { it.tvgName?.trim().equals(epgName, ignoreCase = true) }?.let {
                return it.dbId
            }
        }

        // Pass 3: normalized fuzzy match (Levenshtein).
        val normEpg = normalizeName(epgName)
        if (normEpg.isNotBlank()) {
            var bestId: Long? = null
            var bestScore = 0f
            for (c in candidates) {
                val normC = normalizeName(
                    listOfNotNull(c.tvgName, c.displayName, c.providerName)
                        .joinToString(" ")
                )
                if (normC.isBlank()) continue
                val score = Levenshtein.similarity(normEpg, normC)
                if (score > bestScore) {
                    bestScore = score
                    bestId = c.dbId
                }
            }
            if (bestScore >= FUZZY_THRESHOLD) return bestId
        }

        return null
    }
}
