package com.dylandos.iptv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dylandos.iptv.media.MediaEngineRegistry
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point for DYLANDOS IPTV ULTIMATE.
 *
 * This is a very low-memory (2 GB class) TV application. We intentionally keep
 * `largeHeap=false` and avoid heavy background allocations here; all heavy
 * state lives in Hilt-managed singletons that are lazily created.
 */
@HiltAndroidApp
class IptvApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var mediaEngineRegistry: MediaEngineRegistry

    override fun onCreate() {
        super.onCreate()
        mediaEngineRegistry.warmUp()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
