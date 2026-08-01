package com.dylandos.iptv.di

import android.content.Context
import com.dylandos.iptv.dvr.RollingFileRecorder
import com.dylandos.iptv.media.LibVlcEngine
import com.dylandos.iptv.media.MediaEngine
import com.dylandos.iptv.media.MpvEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import org.videolan.libvlc.LibVLC
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    private val vlcArgs = listOf(
        "--no-drop-late-frames",
        "--no-skip-frames",
        "--avcodec-hw=any",                       // best-effort hardware decoding
        "--clock-synchro=0",
        "--network-caching=1200",                 // low-latency live tuning (ms)
        "--file-caching=300",
        "--live-caching=300",
        "--no-video-title-show",
        "--verbose=1"
    )

    /**
     * A single shared LibVLC instance, tuned for low-memory Android TV. One
     * instance keeps native heap small; a single MediaPlayer is (re)used for all
     * VLC-routed playback so surfaces attach/detach cheaply.
     */
    @Provides
    @Singleton
    fun provideLibVLC(@ApplicationContext context: Context): LibVLC =
        LibVLC(context.applicationContext, vlcArgs)

    @Provides
    @Singleton
    @IntoMap
    @StringKey(EngineKeys.KEY_VLC)
    fun provideVlcEngine(
        @ApplicationContext context: Context,
        libVLC: LibVLC,
        rollingFileRecorder: RollingFileRecorder
    ): MediaEngine = LibVlcEngine(context, libVLC, rollingFileRecorder)

    @Provides
    @Singleton
    @IntoMap
    @StringKey(EngineKeys.KEY_MPV)
    fun provideMpvEngine(
        @ApplicationContext context: Context
    ): MediaEngine = MpvEngine(context)
}

/** Key constants for engine multibinding. */
object EngineKeys {
    const val KEY_VLC = "VLC"
    const val KEY_MPV = "MPV"

    fun keyOf(type: com.dylandos.iptv.media.EngineType): String = when (type) {
        com.dylandos.iptv.media.EngineType.VLC -> KEY_VLC
        com.dylandos.iptv.media.EngineType.MPV -> KEY_MPV
    }
}
