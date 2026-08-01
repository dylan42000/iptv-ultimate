package com.dylandos.iptv.epg

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Raw XMLTV channel mapping (channel id -> display name / icon). */
data class EpgChannel(
    val id: String,
    val displayName: String,
    val iconUrl: String?
)

/** One normalized EPG programme with absolute UTC epoch times. */
data class EpgProgramme(
    val channelId: String,
    val title: String,
    val description: String?,
    val category: String?,
    val posterUrl: String?,
    val startMs: Long,
    val endMs: Long
)

/**
 * Streaming XMLTV parser. Runs on Dispatchers.IO and normalizes every date to a
 * UTC epoch-millisecond (handling `+0200` offsets and `Z` suffix correctly).
 * The pull-parser keeps memory flat even for very large (multi-hundred-MB) files.
 */
@Singleton
class XmltvParser @Inject constructor() {

    fun parse(input: InputStream): XmltvResult {
        val channels = ArrayList<EpgChannel>()
        val programmes = ArrayList<EpgProgramme>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var event = parser.eventType
        var currentChannel: EpgChannel? = null
        var currentProgramme: ProgrammeAccumulator? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "channel" -> currentChannel = EpgChannel(
                            id = parser.getAttributeValue(null, "id") ?: "",
                            displayName = "",
                            iconUrl = null
                        )
                        "programme" -> {
                            currentProgramme = ProgrammeAccumulator(
                                channelId = parser.getAttributeValue(null, "channel") ?: "",
                                start = parser.getAttributeValue(null, "start"),
                                stop = parser.getAttributeValue(null, "stop")
                            )
                        }
                        "display-name" -> {
                            currentChannel?.let { c ->
                                val text = parser.nextText()
                                if (c.displayName.isBlank()) {
                                    currentChannel = c.copy(displayName = text)
                                }
                            }
                        }
                        "title" -> {
                            currentProgramme?.let { p ->
                                if (p.title.isBlank()) {
                                    val text = parser.nextText()
                                    currentProgramme = p.copy(title = text)
                                }
                            }
                        }
                        "desc" -> {
                            currentProgramme?.let { p ->
                                if (p.description == null) {
                                    currentProgramme = p.copy(description = parser.nextText())
                                }
                            }
                        }
                        "category" -> {
                            currentProgramme?.let { p ->
                                if (p.category == null) {
                                    currentProgramme = p.copy(category = parser.nextText())
                                }
                            }
                        }
                        "icon" -> {
                            val src = parser.getAttributeValue(null, "src")
                            if (currentChannel != null && currentChannel!!.iconUrl == null && src != null) {
                                currentChannel = currentChannel!!.copy(iconUrl = src)
                            }
                            if (currentProgramme != null && currentProgramme!!.posterUrl == null && src != null) {
                                currentProgramme = currentProgramme!!.copy(posterUrl = src)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "channel" -> {
                            currentChannel?.let { if (it.id.isNotBlank()) channels.add(it) }
                            currentChannel = null
                        }
                        "programme" -> {
                            currentProgramme?.let { p ->
                                val start = parseXmltvDate(p.start)
                                val end = parseXmltvDate(p.stop)
                                if (p.title.isNotBlank() && start != null && end != null) {
                                    programmes.add(
                                        EpgProgramme(
                                            channelId = p.channelId,
                                            title = p.title,
                                            description = p.description,
                                            category = p.category,
                                            posterUrl = p.posterUrl,
                                            startMs = start,
                                            endMs = end
                                        )
                                    )
                                }
                            }
                            currentProgramme = null
                        }
                    }
                }
            }
            event = parser.next()
        }

        return XmltvResult(channels, programmes)
    }

    private data class ProgrammeAccumulator(
        val channelId: String,
        val start: String?,
        val stop: String?,
        var title: String = "",
        var description: String? = null,
        var category: String? = null,
        var posterUrl: String? = null
    )

    /**
     * Parse "20170310120000 +0100", "20170310120000 Z" or "20170310120000".
     * Returns epoch milliseconds UTC or null when unparseable.
     */
    fun parseXmltvDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim()

        // Offset suffix
        var offsetSeconds = 0
        var datePart = s
        val offsetMatch = Regex("([+-]\\d{2})(\\d{2})$").find(s)
        if (offsetMatch != null) {
            val hh = offsetMatch.groupValues[1].toInt()
            val mm = offsetMatch.groupValues[2].toInt()
            offsetSeconds = hh * 3600 + mm * 60
            datePart = s.substring(0, offsetMatch.range.first).trim()
        } else if (s.endsWith("Z", ignoreCase = true)) {
            datePart = s.dropLast(1).trim()
        }

        val dt = try {
            if (datePart.contains("T")) {
                LocalDateTime.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } else {
                LocalDateTime.parse(datePart, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            }
        } catch (_: Exception) {
            return null
        }

        return dt.toEpochSecond(ZoneOffset.UTC) * 1000 - (offsetSeconds * 1000L)
    }
}

data class XmltvResult(
    val channels: List<EpgChannel>,
    val programmes: List<EpgProgramme>
)
