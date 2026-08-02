package com.dylandos.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dylandos.iptv.data.entity.ChannelEntity
import com.dylandos.iptv.ui.theme.CinematicSurface
import com.dylandos.iptv.ui.theme.OnDarkHigh
import com.dylandos.iptv.ui.theme.OnDarkMid

/**
 * Simplified channel card — rewritten to compile with tv-material 1.0.0 / material3.
 * Original used tv-material Card with Border API that changed in 1.0.0 final.
 */
@Composable
fun ChannelCard(
    channel: ChannelEntity,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .tvFocus(scale = 1.07f)
            .width(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CinematicSurface),
        onClick = onClick
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
