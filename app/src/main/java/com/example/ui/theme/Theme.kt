package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = OnEmeraldContainer,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = EmeraldContainer,
    secondary = TerracottaDarkSecondary,
    onSecondary = OnTerracottaContainer,
    secondaryContainer = TerracottaDarkContainer,
    onSecondaryContainer = TerracottaContainer,
    tertiary = GoldDarkTertiary,
    onTertiary = OnGoldContainer,
    tertiaryContainer = GoldDarkContainer,
    onTertiaryContainer = GoldContainer,
    background = ForestDarkBackground,
    onBackground = CreamDarkInk,
    surface = ForestDarkSurface,
    onSurface = CreamDarkInk,
    surfaceVariant = ForestDarkSurfaceVariant,
    onSurfaceVariant = MistDarkInk,
    outline = ForestDarkOutline,
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = TerracottaSecondary,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = SunlitGoldTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = SandBackground,
    onBackground = CharcoalInk,
    surface = IvorySurface,
    onSurface = CharcoalInk,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = SlateInk,
    outline = WarmOutline,
    error = DisputeError
)

private val MukamboShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MukamboTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = MukamboShapes,
        content = content
    )
}
