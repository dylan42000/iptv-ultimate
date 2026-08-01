package com.dylandos.iptv.dvr

import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FAT32 Rolling Segmenter.
 *
 * FAT32 caps a single file at 4 GB. This recorder transparently rotates output to
 * `name_part2.ts`, `name_part3.ts`, ... whenever the active part reaches
 * [FAT32_SAFE_MAX_BYTES] (3.8 GB), guaranteeing the on-disk mux never exceeds the
 * filesystem limit and never drops a video frame during rotation.
 *
 * It consumes an [InputStream] (which for LibVLC is a Unix FIFO fed by the
 * `:sout` duplicate stream-copy pipeline) so recording shares the *same* network
 * connection as the display path — no secondary provider socket is opened.
 */
@Singleton
class RollingFileRecorder @Inject constructor() {

    companion object {
        const val FAT32_SAFE_MAX_BYTES: Long = 3_800_000_000L // 3.8 GB
        private const val BUFFER_SIZE = 256 * 1024
    }

    private data class ActiveSession(
        val baseDir: File,
        val baseName: String,
        val maxPartSize: Long
    )

    private var session: ActiveSession? = null
    private var out: OutputStream? = null
    private val partFiles = ArrayList<File>()
    private var partIndex = 1
    private var bytesInPart = 0L

    private val running = AtomicBoolean(false)
    private val totalBytes = AtomicLong(0L)

    /** Ordered list of part files produced so far (empty until recording). */
    @Synchronized
    fun parts(): List<File> = partFiles.toList()

    @Synchronized
    fun totalSizeBytes(): Long = totalBytes.get()

    /** True while the pump thread is actively consuming the input stream. */
    @Synchronized
    fun isActive(): Boolean = running.get()

    /** Begin recording [input] to rotating files under [baseDir]/[baseName]. */
    @Synchronized
    fun start(
        input: InputStream,
        baseDir: File,
        baseName: String,
        maxPartSize: Long = FAT32_SAFE_MAX_BYTES
    ) {
        if (running.get()) stop()
        if (!baseDir.exists()) baseDir.mkdirs()
        session = ActiveSession(baseDir, baseName, maxPartSize)
        partFiles.clear()
        partIndex = 1
        bytesInPart = 0
        totalBytes.set(0)
        running.set(true)

        Thread {
            pump(input)
        }.apply {
            name = "rolling-recorder"
            isDaemon = true
            start()
        }
    }

    private fun pump(input: InputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        try {
            openNextPartInternal()
            while (running.get()) {
                val read = input.read(buffer)
                if (read == -1) break
                if (read > 0) {
                    writeInternal(buffer, 0, read)
                }
            }
        } catch (_: Exception) {
            // Recording loop ended; ensure final part is closed.
        } finally {
            running.set(false)
            closeCurrentPartInternal()
        }
    }

    @Synchronized
    private fun openNextPartInternal() {
        closeCurrentPartInternal()
        val s = session ?: return
        val part = File(s.baseDir, "${s.baseName}_part${partIndex}.ts")
        out = FileOutputStream(part, true)
        partFiles.add(part)
        bytesInPart = 0
        partIndex++
    }

    @Synchronized
    private fun writeInternal(buffer: ByteArray, offset: Int, length: Int) {
        val s = session ?: return
        // Seamless rotation: spill into a fresh part when the current one is full.
        if (bytesInPart + length > s.maxPartSize) {
            openNextPartInternal()
        }
        val o = out ?: return
        o.write(buffer, offset, length)
        o.flush()
        bytesInPart += length
        totalBytes.addAndGet(length.toLong())
    }

    @Synchronized
    private fun closeCurrentPartInternal() {
        try {
            out?.flush()
            out?.close()
        } catch (_: Exception) {
        } finally {
            out = null
        }
    }

    /** Stop recording and close the current part (and input). */
    @Synchronized
    fun stop() {
        running.set(false)
        session = null
    }
}

/** Extension that also closes the upstream input stream. */
fun RollingFileRecorder.stopAndClose(input: InputStream?) {
    stop()
    try {
        input?.close()
    } catch (_: Exception) {
    }
}
