package com.fatum.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// FATUM Design System — Inspired by loggd.life
// Signature colour: bright green on near-black background
// ─────────────────────────────────────────────────────────────────────────────
object FatumColors {
    // Backgrounds
    val Background       = Color(0xFF0F1117)   // Near-black, slightly blue-tinted
    val Surface          = Color(0xFF1A1D27)   // Card surface
    val SurfaceVariant   = Color(0xFF232633)   // Elevated card / input bg
    val SurfaceHigh      = Color(0xFF2C3040)   // Chips, selected states

    // Greens (loggd.life signature)
    val Green            = Color(0xFF4ADE80)   // Primary accent — bright green
    val GreenDim         = Color(0xFF22C55E)   // Darker green for pressed states
    val GreenSurface     = Color(0xFF162412)   // Tinted green card background
    val GreenBorder      = Color(0xFF1E3A1F)   // Subtle green border

    // Text
    val TextPrimary      = Color(0xFFF1F2F5)   // Headings, active labels
    val TextSecondary    = Color(0xFF9BA3BA)   // Body copy, descriptions
    val TextMuted        = Color(0xFF555F78)   // Timestamps, placeholders

    // State colours
    val Error            = Color(0xFFFC6161)
    val Warning          = Color(0xFFFBBF24)
    val Info             = Color(0xFF60A5FA)

    // Heat-map scale (green like loggd.life/GitHub)
    val Heat0            = Color(0xFF1A1D27)   // Empty
    val Heat1            = Color(0xFF14352B)
    val Heat2            = Color(0xFF185C3A)
    val Heat3            = Color(0xFF1E8C4E)
    val Heat4            = Color(0xFF4ADE80)   // Max

    // Border/Divider
    val Border           = Color(0xFF2A2F3E)
    val DividerLine      = Color(0xFF1F2333)

    // Star priority
    val Star1            = Color(0xFF555F78)
    val Star2            = Color(0xFF4ADE80)
    val Star3            = Color(0xFFFBBF24)
}

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 dark scheme using FATUM green
// ─────────────────────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = FatumColors.Green,
    onPrimary            = Color(0xFF0F1117),
    primaryContainer     = FatumColors.GreenSurface,
    onPrimaryContainer   = FatumColors.Green,
    secondary            = FatumColors.Info,
    background           = FatumColors.Background,
    surface              = FatumColors.Surface,
    surfaceVariant       = FatumColors.SurfaceVariant,
    onBackground         = FatumColors.TextPrimary,
    onSurface            = FatumColors.TextPrimary,
    onSurfaceVariant     = FatumColors.TextSecondary,
    error                = FatumColors.Error,
    outline              = FatumColors.Border
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography
// ─────────────────────────────────────────────────────────────────────────────
val FatumTypography = Typography(
    displayLarge   = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold,    letterSpacing = (-1.5).sp),
    displayMedium  = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold,    letterSpacing = (-1).sp),
    headlineLarge  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold,    letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    headlineSmall  = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleLarge     = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    titleMedium    = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    titleSmall     = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    bodyLarge      = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal,  lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal,  lineHeight = 20.sp),
    bodySmall      = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal,  lineHeight = 18.sp),
    labelLarge     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium,  letterSpacing = 0.2.sp),
    labelMedium    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,  letterSpacing = 0.3.sp),
    labelSmall     = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium,  letterSpacing = 0.5.sp)
)

@Composable
fun FatumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // FATUM is always dark — aligned with loggd.life's default dark aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = FatumTypography,
        content     = content
    )
}
