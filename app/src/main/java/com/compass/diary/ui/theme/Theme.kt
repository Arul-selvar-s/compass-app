package com.compass.diary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── COLOUR PALETTE ──────────────────────────────────────────────
object CompassColors {
    // Primary – deep compass blue
    val Blue900 = Color(0xFF0D2B5C)
    val Blue800 = Color(0xFF1A3C7C)
    val Blue700 = Color(0xFF1E4DA6)
    val Blue600 = Color(0xFF2563C8)
    val Blue500 = Color(0xFF3B82F6)
    val Blue400 = Color(0xFF60A5FA)
    val Blue300 = Color(0xFF93C5FD)
    val Blue100 = Color(0xFFDBEAFE)
    val Blue50  = Color(0xFFEFF6FF)

    // Accent – compass gold / amber needle
    val Gold500 = Color(0xFFF59E0B)
    val Gold400 = Color(0xFFFBBF24)
    val Gold300 = Color(0xFFFCD34D)
    val Gold200 = Color(0xFFFDE68A)

    // Neutrals
    val Silver900 = Color(0xFF111827)
    val Silver800 = Color(0xFF1F2937)
    val Silver700 = Color(0xFF374151)
    val Silver600 = Color(0xFF4B5563)
    val Silver400 = Color(0xFF9CA3AF)
    val Silver200 = Color(0xFFE5E7EB)
    val Silver100 = Color(0xFFF3F4F6)
    val Silver50  = Color(0xFFF9FAFB)
    val White     = Color(0xFFFFFFFF)

    // Semantic
    val Success  = Color(0xFF22C55E)
    val Warning  = Color(0xFFF59E0B)
    val Error    = Color(0xFFEF4444)
    val Star     = Color(0xFFFBBF24)
}

// ─── DARK COLOUR SCHEME ──────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = CompassColors.Blue400,
    onPrimary        = CompassColors.Silver900,
    primaryContainer = CompassColors.Blue800,
    onPrimaryContainer = CompassColors.Blue100,
    secondary        = CompassColors.Gold400,
    onSecondary      = CompassColors.Silver900,
    secondaryContainer = Color(0xFF4A3500),
    onSecondaryContainer = CompassColors.Gold200,
    background       = Color(0xFF0A0F1E),
    onBackground     = CompassColors.Silver100,
    surface          = Color(0xFF111827),
    onSurface        = CompassColors.Silver100,
    surfaceVariant   = Color(0xFF1F2937),
    onSurfaceVariant = CompassColors.Silver400,
    outline          = CompassColors.Silver700,
    error            = CompassColors.Error,
)

// ─── LIGHT COLOUR SCHEME ─────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = CompassColors.Blue700,
    onPrimary        = CompassColors.White,
    primaryContainer = CompassColors.Blue100,
    onPrimaryContainer = CompassColors.Blue900,
    secondary        = CompassColors.Gold500,
    onSecondary      = CompassColors.White,
    secondaryContainer = CompassColors.Gold200,
    onSecondaryContainer = Color(0xFF3B2000),
    background       = CompassColors.Silver50,
    onBackground     = CompassColors.Silver900,
    surface          = CompassColors.White,
    onSurface        = CompassColors.Silver900,
    surfaceVariant   = CompassColors.Silver100,
    onSurfaceVariant = CompassColors.Silver600,
    outline          = CompassColors.Silver200,
    error            = CompassColors.Error,
)

// ─── TYPOGRAPHY ──────────────────────────────────────────────────
val CompassTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontWeight = FontWeight.Bold,   fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

// ─── THEME COMPOSABLE ─────────────────────────────────────────────
@Composable
fun CompassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CompassTypography,
        content     = content
    )
}
