package com.rimvydop.redboxpcemulator.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// RedBox Premium — Obsidian Cyberdeck Design Tokens
// ============================================================================

// Brand Primary (Crimson / Ruby)
val RedBoxCrimson = Color(0xFFFF2A42)
val RedBoxCrimsonGlow = Color(0x33FF2A42)
val RedBoxCrimsonDark = Color(0xFF7A0E1C)
val RedBoxCrimsonContainer = Color(0xFF38080E)

// Canvas & Surfaces (Obsidian & Carbon)
val RedBoxBackground = Color(0xFF080A0F)
val RedBoxSurface = Color(0xFF10141D)
val RedBoxSurfaceElevated = Color(0xFF181E2B)
val RedBoxSurfaceGlass = Color(0xCC131824)
val RedBoxSurfaceGlassBorder = Color(0x33FFFFFF)

// Accent Status & Telemetry
val RedBoxStatusOnline = Color(0xFF00E676)
val RedBoxStatusOnlineGlow = Color(0x3300E676)
val RedBoxStatusStandby = Color(0xFF5C6B82)
val RedBoxStatusWarning = Color(0xFFFFB300)

// Category & Feature Accents
val RedBoxCyan = Color(0xFF00E5FF)
val RedBoxCyanGlow = Color(0x3300E5FF)
val RedBoxPurple = Color(0xFFB388FF)
val RedBoxBlue = Color(0xFF2979FF)
val RedBoxAmber = Color(0xFFFF9100)

// Text & Content
val RedBoxTextPrimary = Color(0xFFFFFFFF)
val RedBoxTextSecondary = Color(0xFF9EAABF)
val RedBoxTextMuted = Color(0xFF5A667A)

// Gradients
val RedBoxHeroGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2D0F16),
        Color(0xFF131722),
        Color(0xFF0D1017)
    )
)

val RedBoxCrimsonGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFFF2A42),
        Color(0xFFFF5268)
    )
)

val RedBoxGlassBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color(0x66FF2A42),
        Color(0x1AFFFFFF),
        Color(0x332979FF)
    )
)

// Card & Border Aliases
val RedBoxCardBg = Color(0xFF131824)
val RedBoxCardBorder = Color(0x33FFFFFF)
val RedBoxAccentBlue = Color(0xFF2979FF)

// Legacy backward-compatibility tokens
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
