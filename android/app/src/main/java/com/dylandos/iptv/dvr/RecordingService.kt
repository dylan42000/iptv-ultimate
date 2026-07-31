package com.dylandos.iptv.dvr

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.dylandos.iptv.MainActivity
import com.dylandos.iptv.R
import com.dylandos.iptv.data.dao.RecordingDao
import com.dylandos.iptv.data.entity.RecordingEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Foreground DVR service used for scheduled / background recordings of a plain
 * stream URL. For "record the channel you are watching", the single-connection
 * capture lives in [com.dylandos.iptv.media.LibVlcEngine] (SOUT stream-copy on
 * the active instance); this service is the standalone path for timer recordings.
 */
@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var dvrStorageManager: DvrStorageManager
    @Inject lateinit var rollingFileRecorder: RollingFileRecorder
    @Inject lateinit var recordingDao: RecordingDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var connection: HttpURLConnection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        val channelId = intent.getLongExtra(EXTRA_CHANNEL_ID, 0L)
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Recording"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: channelName
        if (url.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification("Recording $title"))

        scope.launch {
            val recording = startRecording(url, channelId, channelName, title)
            try {
                runRecordingLoop(url, recording)
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun runRecordingLoop(url: String, recording: RecordingEntity) {
        try {
            val dir = dvrStorageManager.getDirectory(DvrStorageManager.RECORD_DIR)
            val baseName = sanitize(recording.title) + "_" + System.currentTimeMillis()

            connection = URL(url).openConnection() as HttpURLConnection
            connection?.requestMethod = "GET"
            connection?.setRequestProperty("Connection", "Keep-Alive")
            connection?.connectTimeout = 10_000
            connection?.readTimeout = 15_000

            val status = connection?.responseCode
            if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_PARTIAL) {
                recordingDao.complete(recording.id, "FAILED", System.currentTimeMillis(), 0, 0)
                return
            }

            val input = BufferedInputStream(connection!!.inputStream, 256 * 1024)
            rollingFileRecorder.start(input, dir, baseName)

            // Poll size while recording to update Room metadata.
            while (rollingFileRecorder.isActive()) {
                val size = rollingFileRecorder.totalSizeBytes()
                val now = System.currentTimeMillis()
                recordingDao.complete(
                    recording.id, "RECORDING", now,
                    now - recording.startMs, size
                )
                Thread.sleep(5000)
            }
            val finalSize = rollingFileRecorder.totalSizeBytes()
            recordingDao.complete(
                recording.id, "COMPLETE", System.currentTimeMillis(),
                System.currentTimeMillis() - recording.startMs, finalSize
            )
        } catch (_: Exception) {
            recordingDao.complete(recording.id, "FAILED", System.currentTimeMillis(), 0, 0)
        } finally {
            rollingFileRecorder.stop()
            connection?.disconnect()
        }
    }

    private suspend fun startRecording(
        url: String,
        channelId: Long,
        channelName: String,
        title: String
    ): RecordingEntity {
        val uri = dvrStorageManager.buildDocumentUri(
            DvrStorageManager.RECORD_DIR,
            "${sanitize(title)}_${System.currentTimeMillis()}"
        )?.toString() ?: url
        val rec = RecordingEntity(
            channelId = channelId,
            channelName = channelName,
            title = title,
            uri = uri,
            fileName = sanitize(title),
            startMs = System.currentTimeMillis(),
            status = "RECORDING"
        )
        val id = recordingDao.upsert(rec)
        return rec.copy(id = id)
    }

    override fun onDestroy() {
        connection?.disconnect()
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "DVR Recordings", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val launchIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = if (android.os.Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("DYLANDOS DVR")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(launchIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "dvr_recording"
        private const val NOTIF_ID = 2001
        const val EXTRA_URL = "url"
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_TITLE = "title"

        private fun sanitize(name: String): String =
            name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                .take(60)
    }
}
