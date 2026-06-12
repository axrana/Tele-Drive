package com.example.teledrive.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Telegram-inspired blue palette (Seed)
val TeleBlue = Color(0xFF0088CC)

// Legacy names for compatibility during migration
val TeleBluePrimary = Color(0xFF0088CC)
val TeleBlueLight = Color(0xFF54A8D5)
val TeleBlueDark = Color(0xFF005F8F)
val TeleBlueContainer = Color(0xFFD0EDFB)
val TeleBlueOnContainer = Color(0xFF003348)

val TeleSuccess = Color(0xFF34C759)
val TeleWarning = Color(0xFFFF9500)

// File type colors - Kept for consistency but refined
val ColorPDF = Color(0xFFE53E3E)
val ColorImage = Color(0xFF9F7AEA) // Soft violet
val ColorVideo = Color(0xFFF687B3) // Soft pink/magenta
val ColorAudio = Color(0xFF48BB78) // Emerald
val ColorArchive = Color(0xFFECC94B) // Amber
val ColorDoc = Color(0xFF4299E1) // Blue
val ColorFolder = Color(0xFF0088CC) // Telegram Blue

// Custom palettes for older versions
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006495),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCE5FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF50606E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E5F5),
    onSecondaryContainer = Color(0xFF0C1D29),
    tertiary = Color(0xFF65587E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF201637),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF72777F),
    error = Color(0xFFBA1A1A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF91CDFF),
    onPrimary = Color(0xFF003450),
    primaryContainer = Color(0xFF004B71),
    onPrimaryContainer = Color(0xFFCCE5FF),
    secondary = Color(0xFFB7C9D9),
    onSecondary = Color(0xFF22323F),
    secondaryContainer = Color(0xFF384956),
    onSecondaryContainer = Color(0xFFD3E5F5),
    tertiary = Color(0xFFCFC0E8),
    onTertiary = Color(0xFF362B4D),
    tertiaryContainer = Color(0xFF4D4165),
    onTertiaryContainer = Color(0xFFEBDDFF),
    background = Color(0xFF0F1418), // Rich blue-black
    onBackground = Color(0xFFE1E2E5),
    surface = Color(0xFF0F1418),
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199),
    error = Color(0xFFFFB4AB)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun TeleDriveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        typography = AppTypography,
        content = content
    )
}
