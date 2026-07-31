package com.dylandos.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvTheme
import coil.compose.AsyncImage
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.ui.theme.CinematicSurface
import com.dylandos.iptv.ui.theme.OnDarkHigh
import com.dylandos.iptv.ui.theme.OnDarkMid

/**
 * A poster / channel card with hardware-accelerated Coil image loading and
 * graphicsLayer-only focus animation.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelCard(
    channel: ChannelEntity,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.tvFocus(scale = 1.07f).width(180.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (selected) Border(
            Border.Width(3.dp),
            TvTheme.colorScheme.primary
        ) else Border.None,
        onClick = onClick,
        colors = CardDefaults.tvCardColors()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(CinematicSurface)
            ) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnDarkHigh,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.streamType,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnDarkMid,
                    maxLines = 1
                )
            }
        }
    }
}

/** Minimal helper so we don't depend on the experimental card color builder. */
private object CardDefaults {
    fun tvCardColors() = androidx.tv.material3.CardDefaults.cardColors(
        containerColor = CinematicSurface
    )
}
