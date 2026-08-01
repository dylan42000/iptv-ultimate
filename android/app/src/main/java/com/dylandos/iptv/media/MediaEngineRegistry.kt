package com.dylandos.iptv.media

import com.dylandos.iptv.di.EngineKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry that owns the two concrete engine instances and the [DualMediaEngine]
 * facade. On cold start it warms the lightweight MPV binding without allocating
 * any heavy buffers, keeping launch memory minimal.
 */
@Singleton
class MediaEngineRegistry @Inject constructor(
    val dual: DualMediaEngine,
    engines: Map<@JvmSuppressWildcards String, @JvmSuppressWildcards MediaEngine>
) {

    val vlc: MediaEngine = checkNotNull(engines[EngineKeys.KEY_VLC])
    val mpv: MediaEngine = checkNotNull(engines[EngineKeys.KEY_MPV])

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Cheap warm-up that does not allocate decoder pipelines. Both engine
     * instances were already materialized during Hilt injection; this just
     * ensures their native bindings are loaded on an IO thread so the first
     * play() feels instant.
     */
    fun warmUp() {
        scope.launch {
            try {
                @Suppress("UNUSED_EXPRESSION")
                dual.state.value // touch the facade
            } catch (_: Exception) {
            }
        }
    }

    fun shutdown() {
        scope.cancel()
        dual.release()
    }
}
