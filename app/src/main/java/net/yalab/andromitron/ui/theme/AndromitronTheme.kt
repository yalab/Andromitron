package net.yalab.andromitron.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = Color(0xFFB8C8D1),
    onSecondary = Color(0xFF23323B),
    secondaryContainer = Color(0xFF394952),
    onSecondaryContainer = Color(0xFFD4E4ED),
    tertiary = Color(0xFFBFC6E0),
    onTertiary = Color(0xFF293042),
    tertiaryContainer = Color(0xFF404659),
    onTertiaryContainer = Color(0xFFDCE2FC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0E1419),
    onBackground = Color(0xFFDEE3E8),
    surface = Color(0xFF0E1419),
    onSurface = Color(0xFFDEE3E8),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C8CD),
    outline = Color(0xFF8A9297),
    outlineVariant = Color(0xFF40484C),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFDEE3E8),
    inverseOnSurface = Color(0xFF2E3135),
    inversePrimary = Color(0xFF006781)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006781),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8EAFF),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = Color(0xFF4F616A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5F0),
    onSecondaryContainer = Color(0xFF0B1D25),
    tertiary = Color(0xFF5C5E71),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E2F9),
    onTertiaryContainer = Color(0xFF191A2B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FAFE),
    onBackground = Color(0xFF171C20),
    surface = Color(0xFFF6FAFE),
    onSurface = Color(0xFF171C20),
    surfaceVariant = Color(0xFFDCE4E9),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787D),
    outlineVariant = Color(0xFFC0C8CD),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2C3135),
    inverseOnSurface = Color(0xFFEDF1F5),
    inversePrimary = Color(0xFF4FC3F7)
)

@Composable
fun AndromitronTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}