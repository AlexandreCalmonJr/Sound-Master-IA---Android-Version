package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SoundMasterColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = PureWhite,
    secondary = NeonMint,
    onSecondary = SpaceBlack,
    tertiary = GlowCyan,
    onTertiary = SpaceBlack,
    background = SpaceBlack,
    onBackground = PureWhite,
    surface = DarkSlate,
    onSurface = PureWhite,
    error = GlowingError,
    onError = PureWhite,
    outline = CardLine
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the professional audio workspace aesthetic
    dynamicColor: Boolean = false, // Preserve our custom styled UI aesthetics
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoundMasterColorScheme,
        typography = Typography,
        content = content
    )
}
