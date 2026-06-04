package com.mc.mateamhf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Crimson,
    background = DarkBg,
    surface = DarkSurface,
    onBackground = OnDark,
    onSurface = OnDark,
)

private val LightColors = lightColorScheme(
    primary = Crimson,
)

@Composable
fun MaTeamHFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = MaTeamHFTypography, content = content)
}
