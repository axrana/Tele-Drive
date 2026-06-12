package com.example.teledrive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Telegram-inspired blue palette
val TeleBluePrimary = Color(0xFF0088CC)
val TeleBlueLight = Color(0xFF54A8D5)
val TeleBlueDark = Color(0xFF005F8F)
val TeleBlueContainer = Color(0xFFD0EDFB)
val TeleBlueOnContainer = Color(0xFF003348)

val TeleSurface = Color(0xFFF5F5F5)
val TeleBackground = Color(0xFFFFFFFF)
val TeleOnSurface = Color(0xFF1C1C1E)
val TeleSubtext = Color(0xFF8E8E93)
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
    secondary = TeleBlueLight,
    onSecondary = Color.White,
    background = TeleBackground,
    onBackground = TeleOnSurface,
    surface = TeleSurface,
    onSurface = TeleOnSurface,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = TeleSubtext,
    error = TeleError,
    outline = Color(0xFFD1D1D6)
)

private val DarkColorScheme = darkColorScheme(
    primary = TeleBlueLight,
    onPrimary = Color(0xFF003348),
    primaryContainer = TeleBlueDark,
    onPrimaryContainer = Color(0xFFD0EDFB),
    secondary = TeleBlueLight,
    onSecondary = Color.Black,
    background = Color(0xFF1C1C1E),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF2C2C2E),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFF8E8E93),
    error = Color(0xFFFF6B6B),
    outline = Color(0xFF48484A)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun TeleDriveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
