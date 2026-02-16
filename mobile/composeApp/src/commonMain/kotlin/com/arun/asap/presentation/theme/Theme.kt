package com.arun.asap.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AsapIndigo,
    onPrimary = AsapWhite,
    primaryContainer = AsapSkyBlue.copy(alpha = 0.25f),
    onPrimaryContainer = AsapNavy,
    secondary = AsapTeal,
    onSecondary = AsapWhite,
    secondaryContainer = AsapTealLight.copy(alpha = 0.25f),
    onSecondaryContainer = AsapTealDark,
    tertiary = AsapOrange,
    onTertiary = AsapWhite,
    tertiaryContainer = AsapOrangeLight.copy(alpha = 0.25f),
    onTertiaryContainer = AsapOrange,
    error = AsapError,
    onError = AsapWhite,
    errorContainer = AsapErrorLight,
    onErrorContainer = AsapErrorDark,
    background = AsapGray50,
    onBackground = AsapGray900,
    surface = AsapWhite,
    onSurface = AsapGray900,
    surfaceVariant = AsapGray100,
    onSurfaceVariant = AsapGray600,
    outline = AsapGray300,
    outlineVariant = AsapGray200,
    inverseSurface = AsapGray900,
    inverseOnSurface = AsapGray100,
    surfaceTint = AsapIndigo
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = AsapGray900,
    primaryContainer = AsapSlate,
    onPrimaryContainer = AsapSkyBlue,
    secondary = DarkSecondary,
    onSecondary = AsapGray900,
    secondaryContainer = AsapTealDark.copy(alpha = 0.3f),
    onSecondaryContainer = AsapTealLight,
    tertiary = AsapOrangeLight,
    onTertiary = AsapGray900,
    tertiaryContainer = AsapOrange.copy(alpha = 0.3f),
    onTertiaryContainer = AsapOrangeLight,
    error = Color(0xFFFF6B6B),
    onError = AsapGray900,
    errorContainer = AsapErrorDark.copy(alpha = 0.3f),
    onErrorContainer = AsapErrorLight,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = AsapGray400,
    outline = AsapGray600,
    outlineVariant = AsapGray700,
    inverseSurface = AsapGray100,
    inverseOnSurface = AsapGray900,
    surfaceTint = DarkPrimary
)

@Composable
fun AsapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AsapTypography,
        shapes = AsapShapes,
        content = content
    )
}
