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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.brajesh.gaas.data.ThemeMode

/**
 * GaaS palette: deep teal, vivid violet and warm amber. The two schemes keep
 * the same three hues at inverted lightness, so switching themes changes the
 * mood without changing the app's identity. No Material You — the palette is
 * deliberately the same on every device.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2F2E4),
    onPrimaryContainer = Color(0xFF00201B),
    inversePrimary = Color(0xFF5EEAD4),
    secondary = Color(0xFF6C4CE0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEFF),
    onSecondaryContainer = Color(0xFF1E0F45),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF3B2600),
    tertiaryContainer = Color(0xFFFFE0A3),
    onTertiaryContainer = Color(0xFF2B1A00),
    error = Color(0xFFC5372C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F8FA),
    onBackground = Color(0xFF101828),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEAF0F5),
    onSurfaceVariant = Color(0xFF475467),
    surfaceTint = Color(0xFF00695C),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDDE5EC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE8EEF3),
    surfaceContainerHighest = Color(0xFFE2E9F0),
    inverseSurface = Color(0xFF1D2939),
    inverseOnSurface = Color(0xFFF5F8FA),
    outline = Color(0xFF98A2B3),
    outlineVariant = Color(0xFFE4E9F0),
    scrim = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF00332B),
    primaryContainer = Color(0xFF00594B),
    onPrimaryContainer = Color(0xFFB2F2E4),
    inversePrimary = Color(0xFF00695C),
    secondary = Color(0xFFBFA8FF),
    onSecondary = Color(0xFF241253),
    secondaryContainer = Color(0xFF3B2A6B),
    onSecondaryContainer = Color(0xFFE8DEFF),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3B2600),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFE0A3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6EDF5),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = Color(0xFF253247),
    onSurfaceVariant = Color(0xFFA8B6CC),
    surfaceTint = Color(0xFF5EEAD4),
    surfaceBright = Color(0xFF25334D),
    surfaceDim = Color(0xFF0B1220),
    surfaceContainerLowest = Color(0xFF070C16),
    surfaceContainerLow = Color(0xFF111A2B),
    surfaceContainer = Color(0xFF162034),
    surfaceContainerHigh = Color(0xFF1D2940),
    surfaceContainerHighest = Color(0xFF25334D),
    inverseSurface = Color(0xFFE6EDF5),
    inverseOnSurface = Color(0xFF1D2939),
    outline = Color(0xFF66768F),
    outlineVariant = Color(0xFF2B3B55),
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
 * Hosts the app's [ColorScheme] and keeps the system bar icon contrast in step
 * with it. `enableEdgeToEdge()` only follows the *OS* dark setting, which is
 * wrong the moment the user pins Light or Dark here. Bar backgrounds are no
 * longer ours to set: on API 35+ the setters are deprecated no-ops because the
 * bars are always transparent, and the app's own background draws behind them.
 */
@Composable
fun GaaSTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = themeMode.isDark()
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.let { activity ->
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
