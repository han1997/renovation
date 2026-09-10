package com.renovation.guardian.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Compose Material 3 主题。
 *
 * - Android 12+ 走 `dynamicLight/DarkColorScheme(context)`（Material You）；
 * - Android 7-11 走 [FallbackLightColors] / [FallbackDarkColors] 静态色板；
 * - 不在 App 内暴露"动态取色开关"，按 PRD D4 决定；
 * - 暗色跟随系统，不在 App 内提供手动切换。
 */
@Composable
fun RenovationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> FallbackDarkColors
        else -> FallbackLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RenovationTypography,
        shapes = RenovationShapes,
        content = content,
    )
}

private val FallbackLightColors = lightColorScheme(
    primary = FallbackLightPrimary,
    onPrimary = FallbackLightOnPrimary,
    primaryContainer = FallbackLightPrimaryContainer,
    onPrimaryContainer = FallbackLightOnPrimaryContainer,
    secondary = FallbackLightSecondary,
    onSecondary = FallbackLightOnSecondary,
    secondaryContainer = FallbackLightSecondaryContainer,
    onSecondaryContainer = FallbackLightOnSecondaryContainer,
    tertiary = FallbackLightTertiary,
    onTertiary = FallbackLightOnTertiary,
    tertiaryContainer = FallbackLightTertiaryContainer,
    onTertiaryContainer = FallbackLightOnTertiaryContainer,
    background = FallbackLightBackground,
    onBackground = FallbackLightOnBackground,
    surface = FallbackLightSurface,
    surfaceTint = FallbackLightPrimary,
    surfaceContainerLowest = FallbackLightContainerLowest,
    surfaceContainerLow = FallbackLightContainerLow,
    surfaceContainer = FallbackLightContainer,
    surfaceContainerHigh = FallbackLightContainerHigh,
    surfaceContainerHighest = FallbackLightContainerHighest,

    onSurface = FallbackLightOnSurface,
    surfaceVariant = FallbackLightSurfaceVariant,
    onSurfaceVariant = FallbackLightOnSurfaceVariant,
    outline = FallbackLightOutline,
    outlineVariant = FallbackLightOutlineVariant,
    error = FallbackLightError,
    onError = FallbackLightOnError,
    errorContainer = FallbackLightErrorContainer,
    onErrorContainer = FallbackLightOnErrorContainer,
)

private val FallbackDarkColors = darkColorScheme(
    primary = FallbackDarkPrimary,
    onPrimary = FallbackDarkOnPrimary,
    primaryContainer = FallbackDarkPrimaryContainer,
    onPrimaryContainer = FallbackDarkOnPrimaryContainer,
    secondary = FallbackDarkSecondary,
    onSecondary = FallbackDarkOnSecondary,
    secondaryContainer = FallbackDarkSecondaryContainer,
    onSecondaryContainer = FallbackLightSecondaryContainer,
    tertiary = FallbackDarkTertiary,
    onTertiary = FallbackDarkOnTertiary,
    tertiaryContainer = FallbackDarkTertiaryContainer,
    onTertiaryContainer = FallbackLightTertiaryContainer,
    background = FallbackDarkBackground,
    onBackground = FallbackDarkOnBackground,
    surface = FallbackDarkSurface,
    surfaceTint = FallbackDarkPrimary,
    surfaceContainerLowest = FallbackDarkContainerLowest,
    surfaceContainerLow = FallbackDarkContainerLow,
    surfaceContainer = FallbackDarkContainer,
    surfaceContainerHigh = FallbackDarkContainerHigh,
    surfaceContainerHighest = FallbackDarkContainerHighest,

    onSurface = FallbackDarkOnSurface,
    surfaceVariant = FallbackDarkSurfaceVariant,
    onSurfaceVariant = FallbackDarkOnSurfaceVariant,
    outline = FallbackDarkOutline,
    outlineVariant = FallbackDarkOutlineVariant,
    error = FallbackDarkError,
    onError = FallbackDarkOnError,
    errorContainer = FallbackDarkErrorContainer,
    onErrorContainer = FallbackDarkOnErrorContainer,
)