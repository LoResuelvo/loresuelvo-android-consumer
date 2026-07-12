package com.loresuelvo.consumer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material 3 color scheme mapped to the LoResuelvo brand palette
 * ([Color.kt], mirrored from the web design system). The app has a
 * single light scheme for now; dark mode can be added later by
 * providing a `darkColorScheme` and selecting on
 * `isSystemInDarkTheme()`.
 */
private val LoresuelvoColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = TextWhite,
    secondary = BrandSecondary,
    onSecondary = TextWhite,
    tertiary = BrandTertiary,
    onTertiary = BrandAccept,
    background = BrandNeutral,
    onBackground = BrandAccept,
    surface = SurfaceWhite,
    onSurface = BrandAccept,
    error = BrandDanger,
    onError = TextWhite,
)

/**
 * App-wide theme. Wrap the composition root ([com.loresuelvo.consumer.ui.navigation.LoResuelvoNav])
 * so every `MaterialTheme.colorScheme` / `MaterialTheme.typography`
 * lookup resolves to the brand values instead of Material defaults.
 */
@Composable
fun LoresuelvoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LoresuelvoColorScheme,
        content = content,
    )
}
