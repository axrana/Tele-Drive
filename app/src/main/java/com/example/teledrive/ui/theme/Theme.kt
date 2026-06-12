package com.example.teledrive.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Telegram-inspired blue palette
val TeleBluePrimary = Color(0xFF0088CC)
val TeleBlueLight = Color(0xFF54A8D5)
val TeleBlueDark = Color(0xFF005F8F)
val TeleBlueContainer = Color(0xFFD0EDFB)
val TeleBlueOnContainer = Color(0xFF003348)

val TeleSurface = Color(0xFFF8F9FA)
val TeleBackground = Color(0xFFFFFFFF)
val TeleOnSurface = Color(0xFF1C1C1E)
val TeleSubtext = Color(0xFF636366)
val TeleError = Color(0xFFFF3B30)
val TeleSuccess = Color(0xFF34C759)
val TeleWarning = Color(0xFFFF9500)

// File type colors
val ColorPDF = Color(0xFFE53E3E)
val ColorImage = Color(0xFF48BB78)
val ColorVideo = Color(0xFF9F7AEA)
val ColorAudio = Color(0xFFED8936)
val ColorArchive = Color(0xFFECC94B)
val ColorDoc = Color(0xFF4299E1)
val ColorFolder = Color(0xFFFFCC02)

private val LightColorScheme = lightColorScheme(
    primary = TeleBluePrimary,
    onPrimary = Color.White,
    primaryContainer = TeleBlueContainer,
    onPrimaryContainer = TeleBlueOnContainer,
    secondary = Color(0xFF007AFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5F1FF),
    onSecondaryContainer = Color(0xFF001D3D),
    tertiary = Color(0xFF5856D6),
    onTertiary = Color.White,
    background = TeleBackground,
    onBackground = TeleOnSurface,
    surface = TeleSurface,
    onSurface = TeleOnSurface,
    surfaceVariant = Color(0xFFE9E9EB),
    onSurfaceVariant = TeleSubtext,
    error = TeleError,
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFD1D1D6)
)

private val DarkColorScheme = darkColorScheme(
    primary = TeleBlueLight,
    onPrimary = Color(0xFF003348),
    primaryContainer = TeleBlueDark,
    onPrimaryContainer = Color(0xFFD0EDFB),
    secondary = Color(0xFF0A84FF),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003366),
    onSecondaryContainer = Color(0xFFCCE5FF),
    tertiary = Color(0xFF5E5CE6),
    onTertiary = Color.White,
    background = Color(0xFF0F172A), // Slate 900-like
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B), // Slate 800-like
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFFF453A),
    outline = Color(0xFF475569)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.2).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
