package com.jaminsmoke.personalcomander.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Identidad dark premium (plantilla → Compose).
 * `dynamicColor` desactivado: la marca no sigue el wallpaper.
 */
private val PcDarkColorScheme = darkColorScheme(
    primary = PcPrimary,
    onPrimary = PcOnPrimary,
    primaryContainer = PcPrimaryContainer,
    onPrimaryContainer = PcOnPrimaryContainer,
    secondary = PcSecondary,
    onSecondary = PcOnSecondary,
    secondaryContainer = PcSecondaryContainer,
    onSecondaryContainer = PcOnSecondaryContainer,
    tertiary = PcTertiary,
    onTertiary = PcOnTertiary,
    tertiaryContainer = PcTertiaryContainer,
    onTertiaryContainer = PcOnTertiaryContainer,
    error = PcError,
    onError = PcOnError,
    errorContainer = PcErrorContainer,
    onErrorContainer = PcOnErrorContainer,
    background = PcBackground,
    onBackground = PcOnBackground,
    surface = PcSurface,
    onSurface = PcOnSurface,
    surfaceVariant = PcSurfaceVariant,
    onSurfaceVariant = PcOnSurfaceVariant,
    surfaceDim = PcSurfaceDim,
    surfaceBright = PcSurfaceBright,
    surfaceContainerLowest = PcSurfaceContainerLowest,
    surfaceContainerLow = PcSurfaceContainerLow,
    surfaceContainer = PcSurfaceContainer,
    surfaceContainerHigh = PcSurfaceContainerHigh,
    surfaceContainerHighest = PcSurfaceContainerHighest,
    outline = PcOutline,
    outlineVariant = PcOutlineVariant,
    inverseSurface = PcInverseSurface,
    inverseOnSurface = PcInverseOnSurface,
    inversePrimary = PcInversePrimary,
)

/** Light mínimo espejo (por si se reactiva); no es el look de producto. */
private val PcLightColorScheme = lightColorScheme(
    primary = PcInversePrimary,
    onPrimary = PcPrimary,
    primaryContainer = PcPrimary,
    onPrimaryContainer = PcOnPrimary,
    secondary = PcSecondaryContainer,
    onSecondary = PcSecondary,
    secondaryContainer = PcSecondary,
    onSecondaryContainer = PcOnSecondary,
    tertiary = PcOnTertiaryContainer,
    onTertiary = PcTertiary,
    tertiaryContainer = PcTertiary,
    onTertiaryContainer = PcOnTertiary,
    error = PcErrorContainer,
    onError = PcError,
    errorContainer = PcError,
    onErrorContainer = PcOnError,
    background = PcInverseSurface,
    onBackground = PcInverseOnSurface,
    surface = PcInverseSurface,
    onSurface = PcInverseOnSurface,
    surfaceVariant = PcSurfaceVariant,
    onSurfaceVariant = PcOnSurfaceVariant,
    outline = PcOutline,
    outlineVariant = PcOutlineVariant,
    inverseSurface = PcSurface,
    inverseOnSurface = PcOnSurface,
    inversePrimary = PcPrimary,
)

@Composable
fun PersonalComanderTheme(
    /** Forzar scheme de marca dark (recomendado para producto). */
    forceBrandDark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (forceBrandDark) PcDarkColorScheme else PcLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
