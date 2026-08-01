package com.dylandos.iptv.media

/** The two native media backends. */
enum class EngineType { VLC, MPV }

/** Coarse playback lifecycle state. */
enum class PlaybackState { IDLE, BUFFERING, PLAYING, PAUSED, SEEKING, STOPPED, ERROR }

/** Runtime stream diagnostics surfaced in the Sparkle Zap OSD. */
data class StreamDiagnostics(
    val engine: EngineType,
    val codec: String? = null,
    val bitrateKbps: Int = 0,
    val fps: Float = 0f,
    val resolution: String? = null,
    val audioCodec: String? = null,
    val subtitleTrack: Int = -1,
    val audioTrack: Int = -1,
    val isRecording: Boolean = false,
    val isTimeshiftActive: Boolean = false,
    val dropRate: Float = 0f
)

/** Everything the engine needs to start a playback session. */
data class PlaybackOptions(
    val url: String,
    val streamType: String,            // "live" | "vod" | "series" | "movie"
    val container: String? = null,     // "mpegts", "m3u8", "mp4", ...
    val userAgent: String? = null,
    val referrer: String? = null,
    val startPositionMs: Long = 0L,
    val timeshiftEnabled: Boolean = false,
    val timeshiftPath: String? = null, // disk-ring buffer location (USB)
    val timeshiftBufferMs: Long = 30L * 60_000L,
    val recordingPath: String? = null, // if non-null, start recording on load
    val preferHardwareDecoding: Boolean = true,
    val audioPassthrough: Boolean = false,
    val hlsFailoverMs: Long = 4000L
)

/** Categorization used by the router to pick the correct engine. */
object StreamClassifier {
    fun containerOf(url: String, declared: String?): String {
        declared?.let { if (it.isNotBlank()) return it.lowercase() }
        val u = url.lowercase()
        return when {
            u.contains(".m3u8") -> "m3u8"
            u.contains(".ts") || u.contains(".mpegts") -> "mpegts"
            u.contains(".mpd") || u.contains(".dash") -> "dash"
            u.startsWith("rtsp://") -> "rtsp"
            u.startsWith("rtmp://") || u.startsWith("rtmps://") -> "rtmp"
            u.contains(".mkv") -> "mkv"
            u.contains(".mp4") -> "mp4"
            u.contains(".avi") -> "avi"
            u.contains(".flv") -> "flv"
            else -> "unknown"
        }
    }

    fun isHls(url: String) = url.lowercase().contains(".m3u8")
}
