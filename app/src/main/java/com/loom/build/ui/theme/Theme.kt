package com.loom.build.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalBuildColors = staticCompositionLocalOf { PaperColors }

object LoomBuild {

    val colors: BuildColors
        @Composable get() = LocalBuildColors.current

    @Composable
    fun phaseColor(phase: Phase): Color {
        val c = colors
        return when (phase) {
            Phase.DONE -> c.ok
            Phase.ERROR -> c.danger
            Phase.IDLE -> c.muted
            else -> c.accent
        }
    }

    @Composable
    fun phaseSoft(phase: Phase): Color {
        val c = colors
        return when (phase) {
            Phase.DONE -> c.okSoft
            Phase.ERROR -> c.dangerSoft
            Phase.IDLE -> c.surfaceAlt
            else -> c.accentSoft
        }
    }
}

enum class Phase {
    IDLE,
    SCANNING,
    UPLOADING,
    COMMITTING,
    QUEUED,
    RUNNING,
    DOWNLOADING,
    DONE,
    ERROR,
}

@Composable
fun LoomBuildTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (dark) NightColors else PaperColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.inkInverse,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceAlt,
            onSurfaceVariant = colors.muted,
            outline = colors.hairStrong,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.inkInverse,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surfaceAlt,
            onSurfaceVariant = colors.muted,
            outline = colors.hairStrong,
            error = colors.danger,
        )
    }
    CompositionLocalProvider(LocalBuildColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
