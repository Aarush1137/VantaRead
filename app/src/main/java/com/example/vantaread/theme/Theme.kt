package com.example.vantaread.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.vantaread.data.model.AppAccent

private val DarkColorScheme = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
  )

@Composable
fun VantaReadTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  accent: AppAccent = AppAccent.VANTA_PURPLE,
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val baseColorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val colorScheme = if (accent == AppAccent.MATERIAL_YOU) {
      baseColorScheme
  } else {
      baseColorScheme.withAccent(accent.color, darkTheme)
  }
  
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

private fun ColorScheme.withAccent(accent: Color, darkTheme: Boolean): ColorScheme {
  return copy(
    primary = accent,
    secondary = accent,
    tertiary = accent,
    primaryContainer = if (darkTheme) accent.copy(alpha = 0.28f) else accent.copy(alpha = 0.18f),
    onPrimaryContainer = if (darkTheme) Color.White else Color(0xFF17111C)
  )
}
