package com.fatum.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Colour palette ─────────────────────────────────────────────────────────
object FatumColors {
    val Background      = Color(0xFF0D0D0D)
    val Surface         = Color(0xFF1A1A1A)
    val SurfaceVariant  = Color(0xFF252525)
    val Primary         = Color(0xFFE0E0E0)
    val PrimaryVariant  = Color(0xFFAAAAAA)
    val Accent          = Color(0xFFB08CF8)   // Soft violet accent
    val AccentSecondary = Color(0xFF6EC6A0)   // Teal for success / streaks
    val Error           = Color(0xFFCF6679)
    val Divider         = Color(0xFF2E2E2E)

    // Heat-map intensity scale (GitHub-style)
    val HeatLevel0 = Color(0xFF1E1E1E)
    val HeatLevel1 = Color(0xFF1A3B2A)
    val HeatLevel2 = Color(0xFF2A6040)
    val HeatLevel3 = Color(0xFF3E9060)
    val HeatLevel4 = Color(0xFF5DC389)

    // Priority stars
    val Star1 = Color(0xFF888888)
    val Star2 = Color(0xFFB08CF8)
    val Star3 = Color(0xFFFFD700)
}

// ── Dark colour scheme (mandatory per RNF) ────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = FatumColors.Accent,
    onPrimary        = Color.Black,
    primaryContainer = FatumColors.SurfaceVariant,
    secondary        = FatumColors.AccentSecondary,
    background       = FatumColors.Background,
    surface          = FatumColors.Surface,
    surfaceVariant   = FatumColors.SurfaceVariant,
    onBackground     = FatumColors.Primary,
    onSurface        = FatumColors.Primary,
    onSurfaceVariant = FatumColors.PrimaryVariant,
    error            = FatumColors.Error,
    outline          = FatumColors.Divider
)

// ── Light colour scheme (fallback — dark is the default) ─────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF6B4EBF),
    background       = Color(0xFFF5F5F5),
    surface          = Color.White,
    onBackground     = Color(0xFF111111),
    onSurface        = Color(0xFF111111)
)

// ── Typography ────────────────────────────────────────────────────────────
val FatumTypography = Typography(
    displayLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold,   letterSpacing = (-0.5).sp),
    headlineMedium= TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium   = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium    = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    labelSmall    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

// ── Theme composable ──────────────────────────────────────────────────────
@Composable
fun FatumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = FatumTypography,
        content     = content
    )
}
