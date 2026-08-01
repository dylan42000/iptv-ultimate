package com.dylandos.iptv.ui.player

import android.view.Surface
import com.dylandos.iptv.data.SettingsRepository
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.dvr.DvrStorageManager
import com.dylandos.iptv.dvr.RecordingService
import com.dylandos.iptv.media.DualMediaEngine
import com.dylandos.iptv.media.PlaybackOptions
import com.dylandos.iptv.media.StreamClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import android.content.Intent
import com.dylandos.iptv.media.EngineType
import com.dylandos.iptv.media.PlaybackState
import com.dylandos.iptv.media.StreamDiagnostics

/** Immutable state for the playback screen + Sparkle Zap OSD. */
data class PlayerUiState(
    val channel: ChannelEntity? = null,
    val engineType: EngineType = EngineType.MPV,
    val playback: PlaybackState = PlaybackState.IDLE,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val diagnostics: StreamDiagnostics = StreamDiagnostics(engine = EngineType.MPV),
    val isTimeshiftEnabled: Boolean = false,
    val isRecording: Boolean = false
)

/**
 * Facade between Compose UI and the [DualMediaEngine]. Owns the playback session
 * and exposes a single reactive [state] used by the Player screen and the OSD.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dual: DualMediaEngine,
    private val settings: SettingsRepository,
    private val dvrStorageManager: DvrStorageManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** Last requested surface for video rendering. */
    @Volatile private var surface: Surface? = null

    private val activeChannel: ChannelEntity?
        get() = _uiState.value.channel

    private val currentPosition: Long
        get() = _uiState.value.positionMs

    init {
        scope.launch {
            combine(
                dual.state,
                dual.diagnostics,
                dual.positionMs,
                dual.durationMs,
                settings.settings
            ) { state, diag, pos, dur, s ->
                _uiState.update { cur ->
                    cur.copy(
                        engineType = dual.type,
                        playback = state,
                        positionMs = pos,
                        durationMs = dur,
                        diagnostics = diag,
                        isTimeshiftEnabled = s.timeshiftEnabled && cur.channel?.streamType == "live"
                    )
                }
            }.collect {}
        }
    }

    fun attachSurface(s: Surface?) {
        surface = s
        dual.setSurface(s)
    }

    /** Load and play a channel / VOD / series episode. */
    fun playChannel(channel: ChannelEntity, startPosMs: Long = channel.lastWatchedPosMs) {
        scope.launch {
            val s = settings.current()
            val recordingPath = if (s.autoRecordEnabled && channel.streamType == "live") {
                buildRecordingPath(channel)
            } else null
            val timeshiftPath = if (s.timeshiftEnabled && channel.streamType == "live") {
                buildTimeshiftPath()
            } else null
            val options = PlaybackOptions(
                url = channel.url,
                streamType = channel.streamType,
                container = channel.container,
                startPositionMs = startPosMs,
                timeshiftEnabled = s.timeshiftEnabled && channel.streamType == "live",
                timeshiftPath = timeshiftPath,
                timeshiftBufferMs = s.timeshiftBufferMinutes * 60_000L,
                recordingPath = recordingPath,
                preferHardwareDecoding = s.preferHwDecoding,
                audioPassthrough = s.audioPassthrough,
                hlsFailoverMs = s.hlsFailoverMs
            )
            _uiState.update { it.copy(channel = channel, isRecording = recordingPath != null) }
            dual.setSurface(surface)
            dual.load(options, autoPlay = true)
        }
    }

    fun play() = dual.play()
    fun pause() = dual.pause()

    fun togglePlayPause() {
        if (_uiState.value.playback == PlaybackState.PLAYING) pause() else play()
    }

    fun stop() {
        dual.stop()
        _uiState.update { PlayerUiState() }
    }

    fun seekTo(ms: Long) = dual.seekTo(ms)

    fun nextAudioTrack() = dual.nextAudioTrack()
    fun nextSubtitleTrack() = dual.nextSubtitleTrack()

    fun toggleRecord() {
        val ch = activeChannel ?: return
        val currentlyRecording = _uiState.value.isRecording
        if (currentlyRecording) {
            dual.stopRecording()
            _uiState.update { it.copy(isRecording = false) }
        } else {
            scope.launch {
                val path = buildRecordingPath(ch)
                dual.startRecording(path)
                _uiState.update { it.copy(isRecording = true) }
            }
        }
    }

    /** Rewind within the timeshift ring (up to the configured buffer). */
    fun rewind(seconds: Long = 15) {
        val target = (currentPosition - seconds * 1000).coerceAtLeast(0L)
        dual.seekTo(target)
    }

    fun fastForward(seconds: Long = 15) {
        val target = (currentPosition + seconds * 1000).coerceAtMost(durationOrMax())
        dual.seekTo(target)
    }

    private fun durationOrMax(): Long =
        _uiState.value.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE

    private suspend fun buildRecordingPath(channel: ChannelEntity): String {
        return try {
            val dir = dvrStorageManager.getDirectory(DvrStorageManager.RECORD_DIR)
            val base = sanitize(channel.name)
            File(dir, "$base.ts").absolutePath
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun buildTimeshiftPath(): String? {
        return try {
            val dir = dvrStorageManager.getDirectory(DvrStorageManager.TIMESHIFT_DIR)
            dir.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** Activate-key normalization handler invoked from MainActivity.dispatchKeyEvent. */
    fun onActivateKeyUp() {
        // Reserve for OSD activation; the Compose layer handles actual navigation.
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60)
}

private typealias File = java.io.File
