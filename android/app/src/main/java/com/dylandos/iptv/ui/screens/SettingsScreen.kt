package com.dylandos.iptv.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dylandos.iptv.ui.components.GlassPanel
import com.dylandos.iptv.ui.components.tvFocus
import com.dylandos.iptv.ui.theme.BrandRed
import com.dylandos.iptv.ui.theme.OnDarkHigh
import com.dylandos.iptv.ui.theme.OnDarkMid

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displayMedium,
            color = OnDarkHigh
        )

        Column(
            Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ToggleRow(
                title = "Timeshift (disk ring buffer)",
                subtitle = "Instant pause / rewind up to 30 min on live TV",
                enabled = state.settings.timeshiftEnabled,
                onToggle = { viewModel.setTimeshift(!state.settings.timeshiftEnabled) }
            )
            ToggleRow(
                title = "Prefer hardware decoding",
                subtitle = "HW acceleration for MPV and VLC",
                enabled = state.settings.preferHwDecoding,
                onToggle = { viewModel.setHwDecoding(!state.settings.preferHwDecoding) }
            )
            ToggleRow(
                title = "Audio passthrough (SPDIF)",
                subtitle = "Bitstream AC3 / EAC3 / DTS to receiver",
                enabled = state.settings.audioPassthrough,
                onToggle = { viewModel.setAudioPassthrough(!state.settings.audioPassthrough) }
            )
            ToggleRow(
                title = "Auto-record live channels",
                subtitle = "Begin DVR capture when a live channel starts",
                enabled = state.settings.autoRecordEnabled,
                onToggle = { viewModel.setAutoRecord(!state.settings.autoRecordEnabled) }
            )

            GlassPanel(Modifier.fillMaxWidth().tvFocus(scale = 1.02f)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("USB / OTG DVR Storage", style = MaterialTheme.typography.titleLarge, color = OnDarkHigh)
                        Text(
                            text = if (state.storageReady) "External storage ready" else "Not configured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnDarkMid
                        )
                    }
                }
            }

            GlassPanel(Modifier.fillMaxWidth().tvFocus(scale = 1.02f)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Active Account", style = MaterialTheme.typography.titleLarge, color = OnDarkHigh)
                    Text(
                        text = state.activeAccount?.let { "${it.name}  •  ${it.serverUrl}" } ?: "No account configured",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnDarkMid
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .then(Modifier.tvFocus(scale = 1.02f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = OnDarkHigh)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OnDarkMid)
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier.size(width = 72.dp, height = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (enabled) "ON" else "OFF",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) BrandRed else OnDarkMid
                )
            }
        }
    }
}
