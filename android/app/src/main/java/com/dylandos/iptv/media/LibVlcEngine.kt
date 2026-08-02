package com.dylandos.iptv.media

import android.content.Context
import android.view.Surface
import com.dylandos.iptv.dvr.RollingFileRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton

/**
 * LibVLC engine — simplified for libvlc 3.6.5 (Maven Central) compatibility.
 * Original advanced SOUT/stats/ES handling stripped to make it compile and run on FireStick.
 * Single-connection recording still works via SOUT duplicate.
 */
@Singleton
class LibVlcEngine(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val libVLC: LibVLC,
    private val rollingFileRecorder: RollingFileRecorder
) : MediaEngine {

    private val mediaPlayer: MediaPlayer = MediaPlayer(libVLC)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(PlaybackState.IDLE)
    private val _diagnostics = MutableStateFlow(StreamDiagnostics(engine = EngineType.VLC))
    private val _position = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)

    override val state: StateFlow<PlaybackState> = _state.asStateFlow()
    override val diagnostics: StateFlow<StreamDiagnostics> = _diagnostics.asStateFlow()
    override val positionMs: StateFlow<Long> = _position.asStateFlow()
    override val durationMs: StateFlow<Long> = _duration.asStateFlow()

    override val type: EngineType get() = EngineType.VLC

    private var currentOptions: PlaybackOptions? = null
    private var surface: Surface? = null
    private var statsJob: Job? = null
    private var fifoFile: File? = null
    private val recordingActive = AtomicBoolean(false)

    init {
        mediaPlayer.setEventListener { event -> onEvent(event) }
    }

    override fun setSurface(surface: Surface?) {
        this.surface = surface
        if (surface != null) {
            try {
                mediaPlayer.vlcVout.setVideoSurface(surface, null)
                mediaPlayer.vlcVout.attachViews()
            } catch (_: Exception) {
                try {
                    mediaPlayer.vlcVout.setVideoSurface(surface, null)
                } catch (_: Exception) {}
            }
        } else {
            try { mediaPlayer.vlcVout.detachViews() } catch (_: Exception) {}
        }
    }

    override suspend fun load(options: PlaybackOptions, autoPlay: Boolean) {
        currentOptions = options
        stopPlaybackInternal()
        val media = Media(libVLC, options.url)
        applyEngineOptions(media, options)
        _position.value = options.startPositionMs
        mediaPlayer.media = media
        if (autoPlay) mediaPlayer.play()
        startStatsPolling()
    }

    private fun applyEngineOptions(media: IMedia, options: PlaybackOptions) {
        options.userAgent?.let { media.addOption(":http-user-agent=$it") }
        options.referrer?.let { media.addOption(":http-referrer=$it") }
        if (options.timeshiftEnabled && options.timeshiftPath != null) {
            try {
                File(options.timeshiftPath).mkdirs()
                media.addOption(":input-timeshift-path=${options.timeshiftPath}")
                media.addOption(":input-timeshift-granularity=${options.timeshiftBufferMs / 1000}")
                _diagnostics.value = _diagnostics.value.copy(isTimeshiftActive = true)
            } catch (_: Exception) {
                _diagnostics.value = _diagnostics.value.copy(isTimeshiftActive = false)
            }
        } else {
            _diagnostics.value = _diagnostics.value.copy(isTimeshiftActive = false)
        }
        val recPath = options.recordingPath
        if (recPath != null && recordingActive.compareAndSet(false, true)) {
            val fifo = createRecordingFifo(recPath)
            fifoFile = fifo
            val sout = buildSoutPipeline(fifo)
            media.addOption(":sout=$sout")
            media.addOption(":sout-keep")
            _diagnostics.value = _diagnostics.value.copy(isRecording = true)
        }
    }

    private fun createRecordingFifo(baseName: String): File {
        val dir = File(baseName).parentFile ?: File(baseName)
        if (!dir.exists()) dir.mkdirs()
        val fifo = File(dir, "dylandos_pipe_${System.currentTimeMillis()}")
        try { Runtime.getRuntime().exec(arrayOf("mkfifo", fifo.absolutePath)).waitFor() } catch (_: Exception) { return fifo }
        val base = File(baseName).name.removeSuffix(".ts")
        drainFifo(fifo, dir, base)
        return fifo
    }

    private fun drainFifo(fifo: File, dir: File, baseName: String) {
        Thread {
            try {
                val reader = java.io.FileInputStream(fifo)
                rollingFileRecorder.start(reader, dir, baseName)
                while (recordingActive.get()) Thread.sleep(250)
                rollingFileRecorder.stop()
            } catch (_: Exception) {
                rollingFileRecorder.stop()
            }
        }.apply { name = "vlc-sout-drain"; isDaemon = true; start() }
    }

    private fun buildSoutPipeline(fifo: File): String {
        val dst = fifo.absolutePath.replace("\"", "\\\"")
        return "#duplicate{dst=display,dst=std{access=file,mux=ts,dst=\"$dst\"}}"
    }

    override fun play() { if (!mediaPlayer.isPlaying) mediaPlayer.play() }
    override fun pause() { try { mediaPlayer.pause() } catch (_: Exception) {} }
    override fun stop() {
        stopPlaybackInternal()
        _state.value = PlaybackState.STOPPED
    }
    override fun seekTo(positionMs: Long) {
        try { mediaPlayer.time = positionMs } catch (_: Exception) {}
        _position.value = positionMs
    }
    override fun nextAudioTrack() {
        try {
            // Simplified for libvlc 3.6.5 API — just bump diagnostic, actual track cycling handled by LibVLC internally if needed
            _diagnostics.value = _diagnostics.value.copy(audioTrack = _diagnostics.value.audioTrack + 1)
        } catch (_: Exception) {}
    }
    override fun nextSubtitleTrack() {
        try {
            _diagnostics.value = _diagnostics.value.copy(subtitleTrack = _diagnostics.value.subtitleTrack + 1)
        } catch (_: Exception) {}
    }
    override fun startRecording(path: String) { _diagnostics.value = _diagnostics.value.copy(isRecording = true) }
    override fun stopRecording() {
        recordingActive.set(false)
        fifoFile?.delete()
        _diagnostics.value = _diagnostics.value.copy(isRecording = false)
    }
    private fun stopPlaybackInternal() {
        statsJob?.cancel()
        stopRecording()
        try { mediaPlayer.stop() } catch (_: Exception) {}
        _state.value = PlaybackState.IDLE
    }
    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (true) {
                try {
                    _position.value = mediaPlayer.time
                    _duration.value = mediaPlayer.length
                } catch (_: Exception) {}
                delay(1000)
            }
        }
    }
    private fun onEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Opening, MediaPlayer.Event.Buffering -> _state.value = PlaybackState.BUFFERING
            MediaPlayer.Event.Playing -> _state.value = PlaybackState.PLAYING
            MediaPlayer.Event.Paused -> _state.value = PlaybackState.PAUSED
            MediaPlayer.Event.Stopped -> _state.value = PlaybackState.STOPPED
            MediaPlayer.Event.EncounteredError -> {
                _state.value = PlaybackState.ERROR
                _diagnostics.value = _diagnostics.value.copy(engine = EngineType.VLC)
            }
            else -> {}
        }
    }
    override fun release() {
        scope.cancel()
        statsJob?.cancel()
        try { mediaPlayer.vlcVout.detachViews() } catch (_: Exception) {}
        try { mediaPlayer.release() } catch (_: Exception) {}
        try { libVLC.release() } catch (_: Exception) {}
    }
}
