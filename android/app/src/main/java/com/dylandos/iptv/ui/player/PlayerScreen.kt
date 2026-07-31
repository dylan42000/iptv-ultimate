package com.dylandos.iptv.ui.player

import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dylandos.iptv.data.entity.ProgramEntity
import com.dylandos.iptv.media.PlaybackState
import com.dylandos.iptv.media.StreamDiagnostics
import com.dylandos.iptv.ui.components.GlassPanel
import com.dylandos.iptv.ui.theme.BrandRed
import com.dylandos.iptv.ui.theme.OnDarkHigh
import com.dylandos.iptv.ui.theme.OnDarkLow
import com.dylandos.iptv.ui.theme.OnDarkMid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen player with the "Sparkle Zap" OSD overlay.
 *
 * The overlay occupies the bottom third and appears on D-pad UP/DOWN/CENTER. It
 * shows the channel logo, a current-show progress bar, live stream diagnostics
 * (codec / bitrate / FPS / active engine), and a horizontal preview ribbon of the
 * next 3 EPG programmes. Play/pause, subtitle, audio and record buttons bind
 * directly to the active media engine.
 */
@Composable
fun PlayerScreen(
    channelId: Long,
    controller: PlayerController,
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by controller.uiState.collectAsState()
    val playerData by viewModel.data.collectAsState(initial = PlayerUiData())
    var osdVisible by remember { mutableStateOf(true) }

    val context = LocalContext.current

    // Bind the player to the channel and start playback once.
    LaunchedEffect(channelId) {
        viewModel.setChannel(channelId)
    }
    LaunchedEffect(playerData.channel?.id) {
        playerData.channel?.let { controller.playChannel(it) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    Key.DirectionUp, Key.DirectionDown, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        osdVisible = !osdVisible
                        true
                    }
                    Key.MediaPlayPause, Key.Spacebar -> {
                        controller.togglePlayPause(); true
                    }
                    Key.Back -> { onBack(); true }
                    else -> false
                }
            }
    ) {
        // Hardware video surface.
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            controller.attachSurface(holder.surface)
                        }

                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, h: Int) {
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            controller.attachSurface(null)
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Sparkle Zap OSD (bottom third).
        AnimatedVisibility(
            visible = osdVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
        ) {
            SparkleZapOsd(
                channelName = playerData.channel?.name ?: "—",
                logoUrl = playerData.channel?.logoUrl,
                diagnostics = uiState.diagnostics,
                playback = uiState.playback,
                positionMs = uiState.positionMs,
                durationMs = uiState.durationMs,
                currentProgram = playerData.currentProgram,
                nextPrograms = playerData.nextPrograms,
                isRecording = uiState.isRecording,
                onTogglePlayPause = controller::togglePlayPause,
                onNextAudio = controller::nextAudioTrack,
                onNextSubtitle = controller::nextSubtitleTrack,
                onToggleRecord = controller::toggleRecord,
                onRewind = controller::rewind,
                onFastForward = controller::fastForward
            )
        }
    }
}

@Composable
private fun SparkleZapOsd(
    channelName: String,
    logoUrl: String?,
    diagnostics: StreamDiagnostics,
    playback: PlaybackState,
    positionMs: Long,
    durationMs: Long,
    currentProgram: ProgramEntity?,
    nextPrograms: List<ProgramEntity>,
    isRecording: Boolean,
    onTogglePlayPause: () -> Unit,
    onNextAudio: () -> Unit,
    onNextSubtitle: () -> Unit,
    onToggleRecord: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit
) {
    GlassPanel(Modifier.fillMaxWidth(), cornerRadius = 16) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
            // Row 1: logo + channel + show progress + live badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = channelName,
                    modifier = Modifier.width(96.dp).height(54.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(channelName, style = MaterialTheme.typography.titleLarge, color = OnDarkHigh)
                    Text(
                        text = currentProgram?.title ?: "No EPG",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnDarkMid
                    )
                    if (currentProgram != null) {
                        Spacer(Modifier.height(6.dp))
                        val total = (currentProgram.endMs - currentProgram.startMs).toFloat().coerceAtLeast(1f)
                        val progress = ((System.currentTimeMillis() - currentProgram.startMs) / total).coerceIn(0f, 1f)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(Color(0xFF2A2E38))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(BrandRed)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = if (isRecording) "● REC" else "LIVE",
                    color = if (isRecording) BrandRed else OnDarkHigh,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(12.dp))

            // Row 2: diagnostics + transport controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${diagnostics.engine}  |  ${diagnostics.codec ?: "—"}  |  " +
                        "${diagnostics.bitrateKbps} kbps  |  ${formatFps(diagnostics.fps)} fps  |  " +
                        "${diagnostics.resolution ?: "—"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = OnDarkMid,
                    modifier = Modifier.weight(1f)
                )
                TransportButton("⏪") { onRewind() }
                TransportButton(if (playback == PlaybackState.PLAYING) "⏸" else "▶") { onTogglePlayPause() }
                TransportButton("⏩") { onFastForward() }
                TransportButton("SUB") { onNextSubtitle() }
                TransportButton("AUD") { onNextAudio() }
                TransportButton("REC") { onToggleRecord() }
            }

            Spacer(Modifier.height(12.dp))

            // Row 3: next-3 preview ribbon
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                nextPrograms.take(3).forEach { p ->
                    Box(
                        Modifier
                            .width(220.dp)
                            .background(Color(0xFF242A36))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(p.title, style = MaterialTheme.typography.labelLarge, color = OnDarkHigh, maxLines = 1)
                            Text(
                                formatRange(p.startMs, p.endMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnDarkLow
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .width(64.dp)
            .height(44.dp)
            .background(BrandRed.copy(alpha = 0.25f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = OnDarkHigh, style = MaterialTheme.typography.titleSmall)
    }
}

private fun formatFps(fps: Float): String =
    if (fps > 0f) String.format(Locale.US, "%.0f", fps) else "—"

private fun formatRange(startMs: Long, endMs: Long): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${fmt.format(Date(startMs))} – ${fmt.format(Date(endMs))}"
}
