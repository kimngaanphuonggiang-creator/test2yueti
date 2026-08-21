package com.yueti.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = White,
    primaryContainer = Lavender,
    onPrimaryContainer = Ink,
    secondary = Coral,
    onSecondary = Ink,
    secondaryContainer = Aubergine,
    onSecondaryContainer = Lavender,
    tertiary = Lime,
    onTertiary = Ink,
    tertiaryContainer = LimeSoft,
    onTertiaryContainer = Ink,
    background = Ink,
    onBackground = White,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = LavenderSoft,
    onSurfaceVariant = InkSoft,
    outline = Color(0xFF7C7182),
    outlineVariant = Color(0xFFD0C4D3),
    error = ErrorRed,
    onError = White,
)

private val DarkColors = darkColorScheme(
    primary = Lavender,
    onPrimary = Ink,
    primaryContainer = Purple,
    onPrimaryContainer = White,
    secondary = Coral,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF260C30),
    onSecondaryContainer = Lavender,
    tertiary = Lime,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF344A00),
    onTertiaryContainer = LimeSoft,
    background = Color(0xFF0F0B10),
    onBackground = White,
    surface = Color(0xFF211A22),
    onSurface = White,
    surfaceVariant = Color(0xFF352A3B),
    onSurfaceVariant = Color(0xFFE7D8ED),
    outline = Color(0xFF9C8FA2),
    outlineVariant = Color(0xFF514755),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val YuetiShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YuetiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = YuetiTypography,
        shapes = YuetiShapes,
        content = content,
    )
}
