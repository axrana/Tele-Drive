package com.example.teledrive.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val TeleBluePrimary = Color(0xFF0088CC)
val TeleBlueLight = Color(0xFF40B3E0)
val TeleBlueDark = Color(0xFF006699)
val TeleBlueContainer = Color(0xFFE1F5FE)
val TeleSuccess = Color(0xFF4CAF50)

val ColorFolder = Color(0xFFFFC107)
val ColorImage = Color(0xFFE91E63)
val ColorVideo = Color(0xFF9C27B0)
val ColorAudio = Color(0xFF673AB7)
val ColorDoc = Color(0xFF2196F3)
val ColorPDF = Color(0xFFF44336)
val ColorArchive = Color(0xFF795548)

private val DarkColorScheme = darkColorScheme(
    primary = TeleBlueLight,
    secondary = TeleBluePrimary,
    tertiary = TeleSuccess,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1)
)

private val LightColorScheme = lightColorScheme(
    primary = TeleBluePrimary,
    secondary = TeleBlueDark,
    tertiary = TeleSuccess,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    primaryContainer = TeleBlueContainer,
    onPrimaryContainer = TeleBlueDark
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
