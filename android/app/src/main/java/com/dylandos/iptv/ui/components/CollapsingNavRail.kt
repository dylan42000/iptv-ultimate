package com.dylandos.iptv.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import com.dylandos.iptv.ui.navigation.AppSection
import com.dylandos.iptv.ui.theme.BrandRed
import com.dylandos.iptv.ui.theme.CinematicSurface
import com.dylandos.iptv.ui.theme.OnDarkHigh
import com.dylandos.iptv.ui.theme.OnDarkMid

private data class RailItem(
    val section: AppSection,
    val label: String,
    val icon: ImageVector
)

private val railItems = listOf(
    RailItem(AppSection.LIVE_TV, "Live TV", Icons.Filled.LiveTv),
    RailItem(AppSection.MOVIES, "Movies", Icons.Filled.Movie),
    RailItem(AppSection.SERIES, "Series", Icons.Filled.VideoLibrary),
    RailItem(AppSection.DVR, "DVR", Icons.Filled.Folder),
    RailItem(AppSection.SETTINGS, "Settings", Icons.Filled.Settings)
)

/**
 * Collapsing navigation rail. Collapsed to a slim icon strip; expands to show
 * labels with a tween(150) animation whenever an item gains focus. The whole rail
 * auto-expands when the expanded flag is true (e.g. after pressing LEFT).
 */
@Composable
fun CollapsingNavRail(
    selected: AppSection,
    onSelect: (AppSection) -> Unit,
    modifier: Modifier = Modifier,
    forceExpanded: Boolean = false,
    firstItemFocusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    val collapsedWidth = 72.dp
    val expandedWidth = 224.dp
    val width by animateDpAsState(
        targetValue = if (forceExpanded) expandedWidth else collapsedWidth,
        animationSpec = tween(150),
        label = "railWidth"
    )

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(listOf(CinematicSurface, CinematicSurface.copy(alpha = 0.4f)))
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start
    ) {
        railItems.forEachIndexed { index, item ->
            RailButton(
                item = item,
                selected = item.section == selected,
                expanded = forceExpanded,
                onSelect = { onSelect(item.section) },
                focusRequester = if (index == 0) firstItemFocusRequester else null
            )
        }
    }
}

@Composable
private fun RailButton(
    item: RailItem,
    selected: Boolean,
    expanded: Boolean,
    onSelect: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null
) {
    val bg = if (selected) {
        Brush.horizontalGradient(listOf(BrandRed.copy(alpha = 0.85f), BrandRed.copy(alpha = 0.0f)))
    } else Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, androidx.compose.ui.graphics.Color.Transparent))

    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .background(bg, RoundedCornerShape(14.dp))
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            )
            .then(
                Modifier.tvFocus(scale = 1.06f, ringWidth = 2.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onSelect
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) OnDarkHigh else OnDarkMid,
            modifier = Modifier.size(26.dp)
        )
        if (expanded) {
            Spacer(Modifier.width(14.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) OnDarkHigh else OnDarkMid,
                maxLines = 1
            )
        }
    }
}
