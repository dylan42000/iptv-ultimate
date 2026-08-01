package com.dylandos.iptv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dylandos.iptv.ui.theme.FocusRing

/**
 * Zero-jank TV focus handling. Focus scale/elevation is animated through
 * [Modifier.graphicsLayer] (a pure composition transform — no heavy blur passes)
 * and a crisp border ring is drawn when focused. This avoids the GPU cost of
 * Modifier.blur() which the architecture spec explicitly forbids.
 */
@Composable
fun Modifier.tvFocus(
    scale: Float = 1.08f,
    ringColor: Color = FocusRing,
    ringWidth: Dp = 3.dp
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val animScale by animateFloatAsState(if (focused) scale else 1f, label = "focusScale")
    val ringAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "focusRing")

    val interactionSource = remember { MutableInteractionSource() }

    val base = this
        .graphicsLayer {
            this.scaleX = animScale
            this.scaleY = animScale
            this.alpha = if (focused) 1f else 0.9f
        }
        .onFocusChanged { focused = it.isFocused }
        .focusable(interactionSource = interactionSource)

    return if (focused) {
        base.border(BorderStroke(ringWidth, ringColor.copy(alpha = ringAlpha)))
    } else {
        base
    }
}
