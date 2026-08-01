package com.dylandos.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.data.entity.ProgramEntity
import com.dylandos.iptv.ui.components.GlassPanel
import com.dylandos.iptv.ui.components.tvFocus
import com.dylandos.iptv.ui.theme.BrandRed
import com.dylandos.iptv.ui.theme.OnDarkHigh
import com.dylandos.iptv.ui.theme.OnDarkLow
import com.dylandos.iptv.ui.theme.OnDarkMid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * EPG Canvas Grid: a horizontal timeline of programmes per channel row, with the
 * focused channel highlighted and a current-show progress bar + the next 3 shows.
 */
@Composable
fun EpgScreen(viewModel: EpgViewModel) {
    val uiState by viewModel.uiState.collectAsState(initial = EpgUiState())
    val channelList = uiState.channels
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.selectedChannel?.id) {
        val idx = channelList.indexOfFirst { it.id == uiState.selectedChannel?.id }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            text = "TV Guide",
            style = MaterialTheme.typography.displayMedium,
            color = OnDarkHigh
        )
        Spacer(Modifier.height(8.dp))

        // Focused channel detail panel (current + next 3).
        SelectedChannelDetail(uiState)

        Spacer(Modifier.height(16.dp))

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(channelList, key = { _, c -> c.id }) { index, channel ->
                EpgChannelRow(
                    channel = channel,
                    programs = if (channel.id == uiState.selectedChannel?.id) {
                        uiState.allPrograms
                    } else emptyList(),
                    focused = channel.id == uiState.selectedChannel?.id,
                    onClick = { viewModel.select(index) }
                )
            }
        }
    }
}

@Composable
private fun SelectedChannelDetail(state: EpgUiState) {
    val current = state.currentProgram
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = state.selectedChannel?.name ?: "—",
                style = MaterialTheme.typography.titleLarge,
                color = OnDarkHigh
            )
            if (current != null) {
                Spacer(Modifier.height(6.dp))
                Text(current.title, style = MaterialTheme.typography.titleMedium, color = OnDarkHigh)
                Text(
                    text = formatRange(current.startMs, current.endMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnDarkMid
                )
                // Current show progress bar.
                val total = (current.endMs - current.startMs).toFloat().coerceAtLeast(1f)
                val progress = ((System.currentTimeMillis() - current.startMs) / total)
                    .coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(6.dp)
                        .background(Color(0xFF2A2E38))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(BrandRed)
                    )
                }
                // Next 3 upcoming programs.
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.upcomingPrograms.take(3).forEach { p ->
                        Box(
                            Modifier
                                .width(220.dp)
                                .background(Color(0xFF1A1E28))
                                .padding(10.dp)
                        ) {
                            Text(p.title, style = MaterialTheme.typography.labelLarge, color = OnDarkHigh, maxLines = 2)
                            Text(
                                formatRange(p.startMs, p.endMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnDarkMid
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No EPG data. Press REFRESH to sync.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnDarkMid,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EpgChannelRow(
    channel: ChannelEntity,
    programs: List<ProgramEntity>,
    focused: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .then(Modifier.tvFocus(scale = 1.01f))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .background(if (focused) BrandRed.copy(alpha = 0.14f) else Color(0xFF14171E))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(220.dp)) {
            Text(channel.name, style = MaterialTheme.typography.titleMedium, color = OnDarkHigh, maxLines = 1)
        }
        // Horizontal timeline of the channel's programmes.
        LazyRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(programs.size) { i ->
                val p = programs[i]
                val w = widthForProgram(p).dp
                Box(
                    Modifier
                        .width(w)
                        .background(if (p.startMs <= System.currentTimeMillis() && p.endMs > System.currentTimeMillis()) BrandRed.copy(alpha = 0.55f) else Color(0xFF242A36))
                        .padding(8.dp)
                ) {
                    Text(p.title, style = MaterialTheme.typography.labelMedium, color = OnDarkHigh, maxLines = 1)
                }
            }
        }
    }
}

private fun widthForProgram(p: ProgramEntity): Int {
    val mins = ((p.endMs - p.startMs) / 60000L).toInt().coerceAtLeast(30)
    return (mins * 3).coerceAtMost(420) // ~3 dp per minute, capped.
}

private fun formatRange(startMs: Long, endMs: Long): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${fmt.format(Date(startMs))} – ${fmt.format(Date(endMs))}"
}
