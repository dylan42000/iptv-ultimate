package com.dylandos.iptv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dylandos.iptv.data.entity.RecordingEntity
import com.dylandos.iptv.ui.components.GlassPanel
import com.dylandos.iptv.ui.components.tvFocus
import com.dylandos.iptv.ui.theme.OnDarkHigh
import com.dylandos.iptv.ui.theme.OnDarkMid

@Composable
fun DvrScreen(viewModel: DvrViewModel) {
    val storageReady by viewModel.storageReady.collectAsState()
    val recordings by viewModel.recordings.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text(
            text = "DVR Recordings",
            style = MaterialTheme.typography.displayMedium,
            color = OnDarkHigh
        )

        if (!storageReady) {
            Text(
                text = "Set up USB / OTG storage in Settings to enable DVR.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnDarkMid,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else if (recordings.isEmpty()) {
            Text(
                text = "No recordings yet. Press REC while watching live TV.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnDarkMid,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recordings, key = { it.id }) { rec ->
                    RecordingRow(rec)
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(recording: RecordingEntity) {
    GlassPanel(Modifier.fillMaxWidth().tvFocus(scale = 1.02f)) {
        Column(Modifier.padding(20.dp).fillMaxWidth()) {
            Text(recording.title, style = MaterialTheme.typography.titleLarge, color = OnDarkHigh)
            Text(
                text = "${recording.channelName}  •  ${formatSize(recording.sizeBytes)}  •  ${recording.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = OnDarkMid
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1024) "%.1f GB".format(mb / 1024.0) else "%.0f MB".format(mb)
}
