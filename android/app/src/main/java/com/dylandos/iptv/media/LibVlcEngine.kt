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
 * LibVLC engine.
 *
 * Routing: Live TV (MPEG-TS / RTSP / RTMP) plays here for native transport-stream
 * parsing and reliable `:sout` stream-copy capture.
 *
 * Single-connection recording: when a recording is requested we append
 *   `:sout=#duplicate{dst=display,dst=std{access=file,mux=ts,dst="<fifo>"}}`
 * to the *active* player. The display and record paths share one network socket.
 * The FIFO is drained by a lightweight thread that feeds the FAT32
 * [com.dylandos.iptv.dvr.RollingFileRecorder], enabling seamless 3.8 GB rotation.
 *
 * Timeshift: when enabled, a circular disk-ring buffer is allocated on the USB
 * path (input-timeshift) so pause/rewind up to 30 min works on live TV. When
 * disabled the buffer is never created and disk I/O is bypassed.
 */
@Singleton
class LibVlcEngine(
    @androidx.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val libVLC: LibVLC,
    private val rollingFileRecorder: RollingFileRecorder
) : MediaEngine {

    private val mediaPlayer: MediaPlayer = MediaPlayer(libVLC)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(PlaybackState.IDLE)
    private val _diagnostics = MutableStateFlow(
        StreamDiagnostics(engine = EngineType.VLC)
    )
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
        mediaPlayer.setEventListener { event ->
            onEvent(event)
        }
    }

    override fun setSurface(surface: Surface?) {
        this.surface = surface
        if (surface != null) {
            // Re-attach the video surface to the VLC output on the calling thread.
            mediaPlayer.vlcVout.setVideoSurface(surface, null, context)
            mediaPlayer.vlcVout.attachViews()
        } else {
            try {
                mediaPlayer.vlcVout.detachViews()
            } catch (_: Exception) {
            }
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
            // Disk-ring buffer for instant pause/rewind/FF on live TV.
            File(options.timeshiftPath).mkdirs()
            media.addOption(":input-timeshift-path=${options.timeshiftPath}")
            media.addOption(":input-timeshift-granularity=${options.timeshiftBufferMs / 1000}")
            _diagnostics.value = _diagnostics.value.copy(isTimeshiftActive = true)
        } else {
            _diagnostics.value = _diagnostics.value.copy(isTimeshiftActive = false)
        }

        // --- Single-instance stream-copy recording pipeline ---
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

    /**
     * Creates a Unix FIFO as the `:sout` file destination, then spawns a pump
     * thread that drains the FIFO into the FAT32 rolling segmenter. This makes
     * the rotation seamless and frame-lossless.
     */
    private fun createRecordingFifo(baseName: String): File {
        val dir = File(baseName).parentFile ?: File(baseName)
        if (!dir.exists()) dir.mkdirs()
        val fifo = File(dir, "dylandos_pipe_${System.currentTimeMillis()}")
        try {
            // mkfifo via toybox/toolbox; the pipe is the file VLC writes into.
            Runtime.getRuntime().exec(arrayOf("mkfifo", fifo.absolutePath)).waitFor()
        } catch (_: Exception) {
            // Fall back: VLC creates the file directly (no seamless rotation).
            return fifo
        }
        val base = File(baseName).name.removeSuffix(".ts")
        drainFifo(fifo, dir, base)
        return fifo
    }

    /**
     * Drains the SOUT FIFO into the injected [RollingFileRecorder], which
     * transparently rotates the output at the FAT32 3.8 GB boundary. Because the
     * FIFO is the same byte stream the display path is produced from, capture is
     * single-connection and frame-lossless across rotations.
     */
    private fun drainFifo(fifo: File, dir: File, baseName: String) {
        Thread {
            try {
                val reader = java.io.FileInputStream(fifo)
                rollingFileRecorder.start(reader, dir, baseName)
                while (recordingActive.get()) {
                    // RollingFileRecorder owns the read loop; just block here.
                    Thread.sleep(250)
                }
                rollingFileRecorder.stop()
            } catch (_: Exception) {
                rollingFileRecorder.stop()
            }
        }.apply {
            name = "vlc-sout-drain"
            isDaemon = true
            start()
        }
    }

    private fun buildSoutPipeline(fifo: File): String {
        val dst = fifo.absolutePath.replace("\"", "\\\"")
        return "#duplicate{dst=display,dst=std{access=file,mux=ts,dst=\"$dst\"}}"
    }

    override fun play() {
        if (!mediaPlayer.isPlaying) mediaPlayer.play()
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    override fun stop() {
        stopPlaybackInternal()
        _state.value = PlaybackState.STOPPED
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer.time = positionMs
        _position.value = positionMs
    }

    override fun nextAudioTrack() {
        mediaPlayer.audio.next()
        val tr = mediaPlayer.audio.track
        _diagnostics.value = _diagnostics.value.copy(audioTrack = tr)
    }

    override fun nextSubtitleTrack() {
        mediaPlayer.video.nextSpu()
        val tr = mediaPlayer.video.spuTrack
        _diagnostics.value = _diagnostics.value.copy(subtitleTrack = tr)
    }

    override fun startRecording(path: String) {
        // Recording is initiated at load() time via the SOUT pipeline. This
        // method is a no-op guard to keep the interface uniform.
        _diagnostics.value = _diagnostics.value.copy(isRecording = true)
    }

    override fun stopRecording() {
        recordingActive.set(false)
        fifoFile?.delete()
        _diagnostics.value = _diagnostics.value.copy(isRecording = false)
    }

    private fun stopPlaybackInternal() {
        statsJob?.cancel()
        stopRecording()
        try {
            mediaPlayer.stop()
        } catch (_: Exception) {
        }
        _state.value = PlaybackState.IDLE
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (true) {
                try {
                    val stats = mediaPlayer.media?.stats
                    if (stats != null) {
                        _diagnostics.value = _diagnostics.value.copy(
                            bitrateKbps = (stats.inputBitrate / 1000).toInt()
                        )
                    }
                    _position.value = mediaPlayer.time
                    _duration.value = mediaPlayer.length
                } catch (_: Exception) {
                }
                delay(1000)
            }
        }
    }

    private fun onEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Opening, MediaPlayer.Event.Buffering -> {
                _state.value = PlaybackState.BUFFERING
            }
            MediaPlayer.Event.Playing -> _state.value = PlaybackState.PLAYING
            MediaPlayer.Event.Paused -> _state.value = PlaybackState.PAUSED
            MediaPlayer.Event.Stopped -> _state.value = PlaybackState.STOPPED
            MediaPlayer.Event.EncounteredError -> {
                _state.value = PlaybackState.ERROR
                _diagnostics.value = _diagnostics.value.copy(engine = EngineType.VLC)
            }
            MediaPlayer.Event.ESAdded -> {
                // A new elementary stream (audio/video/subtitle) arrived; expose its
                // fourcc codec when available. event.channel distinguishes ES kinds.
                val codec = event.codec
                if (codec != null) {
                    val isAudio = (event.channel and MediaPlayer.Event.ES_AUDIO) != 0
                    val isVideo = (event.channel and MediaPlayer.Event.ES_VIDEO) != 0
                    val d = _diagnostics.value
                    _diagnostics.value = when {
                        isAudio -> d.copy(audioCodec = codec)
                        isVideo -> d.copy(codec = codec)
                        else -> d
                    }
                }
            }
            MediaPlayer.Event.Vout -> {
                _diagnostics.value = _diagnostics.value.copy(
                    resolution = "${event.width}x${event.height}"
                )
            }
        }
    }

    override fun release() {
        scope.cancel()
        statsJob?.cancel()
        try {
            mediaPlayer.vlcVout.detachViews()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer.release()
        } catch (_: Exception) {
        }
        try {
            libVLC.release()
        } catch (_: Exception) {
        }
    }
}
