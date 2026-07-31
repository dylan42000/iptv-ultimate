package com.dylandos.iptv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dylandos.iptv.ui.theme.GlassPanel
import com.dylandos.iptv.ui.theme.GlassPanelStroke
import com.dylandos.iptv.ui.theme.SurfaceGradient

/**
 * Semi-transparent glassmorphism panel: a subtle gradient base, a translucent
 * overlay and a hairline stroke. Uses only cheap layer composition — no blur.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 18,
    gradient: List<Color> = SurfaceGradient,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(cornerRadius.dp))
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Brush.verticalGradient(gradient))
            .background(GlassPanel.copy(alpha = 0.92f))
            .border(
                BorderStroke(1.dp, GlassPanelStroke),
                RoundedCornerShape(cornerRadius.dp)
            )
    ) {
        content()
    }
}
