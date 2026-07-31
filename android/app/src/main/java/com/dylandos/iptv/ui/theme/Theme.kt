package com.dylandos.iptv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

private val TvDarkColors = darkColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    secondary = BrandAmber,
    onSecondary = CinematicBackground,
    background = CinematicBackground,
    onBackground = OnDarkHigh,
    surface = CinematicSurface,
    onSurface = OnDarkHigh,
    surfaceVariant = GlassPanel,
    onSurfaceVariant = OnDarkMid,
    outline = GlassPanelStroke
)

/**
 * Applies both the standard Material3 dark scheme and the TV material theme.
 * Always dark (cinematic) regardless of system setting.
 */
@Composable
fun DylandosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvDarkColors,
        typography = TvTypography,
        content = {
            TvMaterialTheme(
                colorScheme = androidx.tv.material3.darkColorScheme(
                    primary = BrandRed,
                    onPrimary = Color.White,
                    secondary = BrandAmber,
                    background = CinematicBackground,
                    onBackground = OnDarkHigh,
                    surface = CinematicSurface,
                    onSurface = OnDarkHigh
                ),
                content = content
            )
        }
    )
}
