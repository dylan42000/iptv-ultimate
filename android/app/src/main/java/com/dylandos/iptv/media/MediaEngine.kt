package com.dylandos.iptv.media

import android.view.Surface
import kotlinx.coroutines.flow.StateFlow

/**
 * Common contract implemented by [LibVlcEngine] and [MpvEngine]. All state is
 * exposed as reactive StateFlows on the Main dispatcher; engines do their heavy
 * lifting on dedicated single-thread dispatchers to keep GC pressure low.
 */
interface MediaEngine {

    val type: EngineType

    val state: StateFlow<PlaybackState>
    val diagnostics: StateFlow<StreamDiagnostics>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>

    /** Attach a hardware surface for rendering (SurfaceView). */
    fun setSurface(surface: Surface?)

    /** Load a URL and begin buffering. Does not auto-play unless requested. */
    suspend fun load(options: PlaybackOptions, autoPlay: Boolean = true)

    fun play()

    fun pause()

    fun stop()

    fun seekTo(positionMs: Long)

    fun nextAudioTrack()

    fun nextSubtitleTrack()

    /** Start concurrent single-connection recording to [path] (VLC only stream-copy). */
    fun startRecording(path: String)

    fun stopRecording()

    fun release()
}
