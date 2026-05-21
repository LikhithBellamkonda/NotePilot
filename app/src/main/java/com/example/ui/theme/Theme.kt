package com.example.ui.theme

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

private val LightColorScheme =
  lightColorScheme(
    primary = OmniFeedPrimary,
    secondary = OmniFeedPrimary,
    tertiary = OmniFeedAlertRed,
    background = OmniFeedBackground,
    surface = OmniFeedCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = OmniFeedTextDark,
    onSurface = OmniFeedTextDark,
    surfaceVariant = OmniFeedBackground,
    onSurfaceVariant = OmniFeedTextSecondary,
    outline = OmniFeedBorderColor
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF818CF8),
    secondary = Color(0xFF6366F1),
    tertiary = Color(0xFFF43F5E),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force clean premium visual themed look by default
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
