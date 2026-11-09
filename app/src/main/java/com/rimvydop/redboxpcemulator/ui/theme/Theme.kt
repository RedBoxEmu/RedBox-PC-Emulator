package com.rimvydop.redboxpcemulator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RedBoxCrimson,
    onPrimary = Color.White,
    primaryContainer = RedBoxCrimsonContainer,
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = RedBoxCyan,
    onSecondary = Color(0xFF00363D),
    tertiary = RedBoxPurple,
    onTertiary = Color.White,
    background = RedBoxBackground,
    onBackground = RedBoxTextPrimary,
    surface = RedBoxSurface,
    onSurface = RedBoxTextPrimary,
    surfaceVariant = RedBoxSurfaceElevated,
    onSurfaceVariant = RedBoxTextSecondary,
    outline = RedBoxSurfaceGlassBorder,
    outlineVariant = Color(0x1AFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = RedBoxCrimsonDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF006876),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF131720),
    surface = Color.White,
    onSurface = Color(0xFF131720),
    surfaceVariant = Color(0xFFE9EDF5),
    onSurfaceVariant = Color(0xFF4C5566)
)

@Composable
fun RedBoxPCEmulatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve RedBox branding by default
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
        content = content
    )
}
