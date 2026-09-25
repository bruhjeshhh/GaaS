package com.brajesh.gaas.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.brajesh.gaas.data.ThemeMode

/**
 * GaaS palette: a calm teal primary with a warm amber tertiary for accents.
 * The two schemes are tonal counterparts of each other (same hues, inverted
 * lightness), so switching themes re-colours the app without changing its
 * character. No Material You — the palette is the same on every device.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF00696B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9EF1F1),
    onPrimaryContainer = Color(0xFF002020),
    inversePrimary = Color(0xFF82D5D4),
    secondary = Color(0xFF4A6362),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E6),
    onSecondaryContainer = Color(0xFF05201F),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDF9B),
    onTertiaryContainer = Color(0xFF261A00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4948),
    surfaceTint = Color(0xFF00696B),
    surfaceBright = Color(0xFFFAFDFC),
    surfaceDim = Color(0xFFDBE5E3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F7F6),
    surfaceContainer = Color(0xFFEEF2F1),
    surfaceContainerHigh = Color(0xFFE8EDEB),
    surfaceContainerHighest = Color(0xFFE2E8E6),
    inverseSurface = Color(0xFF2D3130),
    inverseOnSurface = Color(0xFFEFF1F0),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBEC9C7),
    scrim = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D5D4),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504C),
    onPrimaryContainer = Color(0xFF9EF1F1),
    inversePrimary = Color(0xFF00696B),
    secondary = Color(0xFFB1CCCA),
    onSecondary = Color(0xFF1C3533),
    secondaryContainer = Color(0xFF334B4A),
    onSecondaryContainer = Color(0xFFCCE8E6),
    tertiary = Color(0xFFEFC048),
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5D4200),
    onTertiaryContainer = Color(0xFFFFDF9B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDDE4E2),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C7),
    surfaceTint = Color(0xFF82D5D4),
    surfaceBright = Color(0xFF343B3A),
    surfaceDim = Color(0xFF0E1514),
    surfaceContainerLowest = Color(0xFF09100F),
    surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF303635),
    inverseSurface = Color(0xFFDDE4E2),
    inverseOnSurface = Color(0xFF2D3130),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4948),
    scrim = Color(0xFF000000)
)

/**
 * Resolves the user's [ThemeMode] against the current system setting. Split
 * out so the activity can decide the window's status-bar icon contrast using
 * the exact same rule the UI is painted with.
 */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Hosts the app's [ColorScheme], and repaints the system bars to match. The
 * platform theme only knows about the *OS* dark setting, which is wrong twice
 * over: on a dark phone the launch window is white, and once the user pins a
 * theme here the bars have to follow the app, not the OS.
 */
@Composable
fun GaaSTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = themeMode.isDark()
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.let { activity ->
                val barColor = colorScheme.surface.toArgb()
                activity.window.statusBarColor = barColor
                activity.window.navigationBarColor = barColor
                WindowCompat.getInsetsController(activity.window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
