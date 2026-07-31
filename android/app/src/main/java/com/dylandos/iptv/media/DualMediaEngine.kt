package com.dylandos.iptv.media

import android.view.Surface
import com.dylandos.iptv.di.EngineKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The DualMediaEngine routing facade. It dynamically selects the correct backend:
 *
 *  - Live TV (MPEG-TS / RTSP / RTMP)          -> LibVlcEngine
 *  - VOD / Series / HDR / local DVR files     -> MpvEngine
 *  - Live HLS (.m3u8)                         -> MpvEngine with a 4000 ms failover
 *                                               to LibVlcEngine (configurable).
 *
 * Only one engine is "active" at a time; the other is kept idle and its resources
 * released so we never hold two hardware decoder pipelines on a 2 GB device.
 */
@Singleton
class DualMediaEngine @Inject constructor(
    engines: Map<@JvmSuppressWildcards String, @JvmSuppressWildcards MediaEngine>
) : MediaEngine {

    private val vlc: MediaEngine = checkNotNull(engines[EngineKeys.KEY_VLC])
    private val mpv: MediaEngine = checkNotNull(engines[EngineKeys.KEY_MPV])

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var failoverJob: Job? = null

    private val activeFlow = MutableStateFlow<MediaEngine>(mpv)

    override val type: EngineType get() = activeFlow.value.type

    private val _state = MutableStateFlow(PlaybackState.IDLE)
    private val _diagnostics = MutableStateFlow(StreamDiagnostics(engine = EngineType.MPV))
    private val _position = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)

    override val state: StateFlow<PlaybackState> = _state.asStateFlow()
    override val diagnostics: StateFlow<StreamDiagnostics> = _diagnostics.asStateFlow()
    override val positionMs: StateFlow<Long> = _position.asStateFlow()
    override val durationMs: StateFlow<Long> = _duration.asStateFlow()

    init {
        // Mirror whichever engine is active, switching sources transparently.
        scope.launch {
            activeFlow.flatMapLatest { it.state }.collect { _state.value = it }
        }
        scope.launch {
            activeFlow.flatMapLatest { it.diagnostics }.collect { _diagnostics.value = it }
        }
        scope.launch {
            activeFlow.flatMapLatest { it.positionMs }.collect { _position.value = it }
        }
        scope.launch {
            activeFlow.flatMapLatest { it.durationMs }.collect { _duration.value = it }
        }
    }

    /** The engine currently in use. */
    val activeEngine: MediaEngine get() = activeFlow.value

    private fun selectEngine(options: PlaybackOptions): MediaEngine {
        val container = StreamClassifier.containerOf(options.url, options.container)
        return when {
            options.streamType == "live" && (container == "mpegts" || container == "rtsp" || container == "rtmp") ->
                vlc
            options.streamType == "live" && container == "m3u8" -> mpv
            container == "dash" -> mpv
            options.streamType == "live" -> mpv // generic live (e.g. m3u8 already, or unknown)
            else -> mpv // VOD / series / local files
        }
    }

    override fun setSurface(surface: Surface?) {
        vlc.setSurface(null)
        mpv.setSurface(null)
        activeFlow.value.setSurface(surface)
    }

    override suspend fun load(options: PlaybackOptions, autoPlay: Boolean) {
        failoverJob?.cancel()

        val target = selectEngine(options)
        val isHls = StreamClassifier.isHls(options.url)

        // Prepare the target surface on the new engine.
        val isLiveHlsWithFailover = isHls && options.streamType == "live"
        val isLiveVlcRoute = options.streamType == "live" &&
            !isHls &&
            StreamClassifier.containerOf(options.url, options.container) in setOf("mpegts", "rtsp", "rtmp")

        if (isLiveVlcRoute) {
            switchTo(vlc)
            vlc.load(options, autoPlay)
            scheduleLiveFailover(vlc, mpv, options)
        } else {
            switchTo(mpv)
            mpv.load(options, autoPlay)
            if (isLiveHlsWithFailover) {
                scheduleHlsFailover(mpv, vlc, options)
            }
        }
    }

    /** If MPV stalls on HLS past the timeout, transparently hand off to LibVLC. */
    private fun scheduleHlsFailover(primary: MediaEngine, fallback: MediaEngine, options: PlaybackOptions) {
        failoverJob = scope.launch {
            delay(options.hlsFailoverMs)
            if (_state.value != PlaybackState.PLAYING && primary.type == EngineType.MPV) {
                switchTo(fallback)
                primary.stop()
                fallback.load(options, true)
            }
        }
    }

    /** Watchdog so a VLC-route that never reaches PLAYING isn't left stuck. */
    private fun scheduleLiveFailover(primary: MediaEngine, fallback: MediaEngine, options: PlaybackOptions) {
        failoverJob = scope.launch {
            delay(options.hlsFailoverMs)
            if (_state.value != PlaybackState.PLAYING && primary.type == EngineType.VLC) {
                switchTo(fallback)
                primary.stop()
                fallback.load(options, true)
            }
        }
    }

    private fun switchTo(newEngine: MediaEngine) {
        if (activeFlow.value === newEngine) return
        val old = activeFlow.value
        activeFlow.value = newEngine
        old.stop()
    }

    override fun play() = activeFlow.value.play()

    override fun pause() = activeFlow.value.pause()

    override fun stop() {
        failoverJob?.cancel()
        vlc.stop()
        mpv.stop()
        _state.value = PlaybackState.STOPPED
    }

    override fun seekTo(positionMs: Long) = activeFlow.value.seekTo(positionMs)

    override fun nextAudioTrack() = activeFlow.value.nextAudioTrack()

    override fun nextSubtitleTrack() = activeFlow.value.nextSubtitleTrack()

    override fun startRecording(path: String) = activeFlow.value.startRecording(path)

    override fun stopRecording() {
        vlc.stopRecording()
        mpv.stopRecording()
    }

    override fun release() {
        failoverJob?.cancel()
        scope.cancel()
        vlc.release()
        mpv.release()
    }
}
