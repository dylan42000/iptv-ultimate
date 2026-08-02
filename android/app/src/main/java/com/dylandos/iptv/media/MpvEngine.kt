package com.dylandos.iptv.media

import android.content.Context
import android.view.Surface
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Singleton

/**
 * MPV engine (libmpv) — stubbed for build. Full impl requires real MPVLib AAR.
 * Uses backticks for `is` package (Kotlin keyword) and fixed type mismatches.
 */
@Singleton
class MpvEngine(
    private val context: Context
) : MediaEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(PlaybackState.IDLE)
    private val _diagnostics = MutableStateFlow(StreamDiagnostics(engine = EngineType.MPV))
    private val _position = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)

    override val state: StateFlow<PlaybackState> = _state.asStateFlow()
    override val diagnostics: StateFlow<StreamDiagnostics> = _diagnostics.asStateFlow()
    override val positionMs: StateFlow<Long> = _position.asStateFlow()
    override val durationMs: StateFlow<Long> = _duration.asStateFlow()

    override val type: EngineType get() = EngineType.MPV

    private var surface: Surface? = null
    private var loaded = false
    private var positionJob: Job? = null

    init {
        MPVLib.create(context.applicationContext)
        registerPropertyObservers()
    }

    private fun registerPropertyObservers() {
        MPVLib.observeProperty("duration", MPVLib.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("time-pos", MPVLib.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("pause", MPVLib.MPV_FORMAT_FLAG)
        MPVLib.observeProperty("video-params/w", MPVLib.MPV_FORMAT_INT64)
        MPVLib.observeProperty("video-params/h", MPVLib.MPV_FORMAT_INT64)
        MPVLib.observeProperty("video-codec", MPVLib.MPV_FORMAT_STRING)
        MPVLib.observeProperty("audio-codec", MPVLib.MPV_FORMAT_STRING)
        MPVLib.observeProperty("fps", MPVLib.MPV_FORMAT_DOUBLE)
        MPVLib.observeProperty("video-bitrate", MPVLib.MPV_FORMAT_INT64)
        MPVLib.observeProperty("core-idle", MPVLib.MPV_FORMAT_FLAG)

        MPVLib.setOnPropertyChangeListener { property, _, value ->
            onPropertyChanged(property, value)
        }
    }

    private fun onPropertyChanged(property: String, value: Any?) {
        when (property) {
            "duration" -> _duration.value = ((value as? Double) ?: 0.0).toLong() * 1000
            "time-pos" -> _position.value = ((value as? Double) ?: 0.0).toLong() * 1000
            "pause" -> {
                val paused = (value as? Boolean) ?: false
                if (loaded) _state.value = if (paused) PlaybackState.PAUSED else PlaybackState.PLAYING
            }
            "video-params/w", "video-params/h" -> {
                val w = try { MPVLib.getPropertyLong("video-params/w", 0L) } catch (_: Exception) { 0L }
                val h = try { MPVLib.getPropertyLong("video-params/h", 0L) } catch (_: Exception) { 0L }
                if (w > 0 && h > 0) {
                    _diagnostics.value = _diagnostics.value.copy(resolution = "${w}x$h")
                }
            }
            "video-codec" -> _diagnostics.value = _diagnostics.value.copy(
                codec = (value as? String) ?: _diagnostics.value.codec
            )
            "audio-codec" -> _diagnostics.value = _diagnostics.value.copy(
                audioCodec = (value as? String) ?: _diagnostics.value.audioCodec
            )
            "fps" -> _diagnostics.value = _diagnostics.value.copy(
                fps = (value as? Double)?.toFloat() ?: 0f
            )
            "video-bitrate" -> _diagnostics.value = _diagnostics.value.copy(
                bitrateKbps = (((value as? Long) ?: 0L) / 1000).toInt()
            )
            "core-idle" -> {
                val idle = (value as? Boolean) ?: false
                if (loaded && !idle && _state.value != PlaybackState.PAUSED) {
                    _state.value = PlaybackState.PLAYING
                }
            }
        }
    }

    override fun setSurface(surface: Surface?) {
        this.surface = surface
        if (surface != null) MPVLib.init(surface)
    }

    override suspend fun load(options: PlaybackOptions, autoPlay: Boolean) {
        loaded = false
        _state.value = PlaybackState.BUFFERING
        MPVLib.command("stop")
        MPVLib.setOptionString("hwdec", if (options.preferHardwareDecoding) "auto-safe" else "no")
        MPVLib.setOptionString("cache", "yes")
        MPVLib.setOptionString("cache-secs", "30")
        MPVLib.setOptionString("demuxer-max-bytes", "150MiB")
        MPVLib.setOptionString("demuxer-max-back-bytes", "50MiB")
        if (options.audioPassthrough) MPVLib.setOptionString("audio-spdif", "ac3,eac3,dts")
        options.userAgent?.let { MPVLib.setOptionString("user-agent", it) }
        options.referrer?.let { MPVLib.setOptionString("referrer", it) }
        MPVLib.command("loadfile", options.url, "replace")
        loaded = true
        if (options.startPositionMs > 0) {
            MPVLib.command("seek", (options.startPositionMs / 1000).toString(), "absolute")
        }
        if (!autoPlay) MPVLib.command("set", "pause", "yes")
        startPositionPolling()
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                try {
                    _position.value = MPVLib.getPropertyLong("time-pos", 0L) * 1000
                    _duration.value = MPVLib.getPropertyLong("duration", 0L) * 1000
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(500)
            }
        }
    }

    override fun play() { MPVLib.command("set", "pause", "no") }
    override fun pause() { MPVLib.command("set", "pause", "yes") }
    override fun stop() {
        loaded = false
        MPVLib.command("stop")
        _state.value = PlaybackState.STOPPED
    }
    override fun seekTo(positionMs: Long) {
        MPVLib.command("seek", (positionMs / 1000).toString(), "absolute")
    }
    override fun nextAudioTrack() {
        val current = try { MPVLib.getPropertyLong("aid", 0L).toInt() } catch (_: Exception) { 0 }
        MPVLib.command("cycle", "audio")
        _diagnostics.value = _diagnostics.value.copy(audioTrack = current + 1)
    }
    override fun nextSubtitleTrack() {
        val current = try { MPVLib.getPropertyLong("sid", 0L).toInt() } catch (_: Exception) { 0 }
        MPVLib.command("cycle", "sub")
        _diagnostics.value = _diagnostics.value.copy(subtitleTrack = current + 1)
    }
    override fun startRecording(path: String) {
        _diagnostics.value = _diagnostics.value.copy(isRecording = true)
    }
    override fun stopRecording() {
        _diagnostics.value = _diagnostics.value.copy(isRecording = false)
    }
    override fun release() {
        positionJob?.cancel()
        scope.cancel()
        try { MPVLib.command("quit") } catch (_: Exception) {}
    }
}
