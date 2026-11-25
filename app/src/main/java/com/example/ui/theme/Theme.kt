package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EchoOrange,
    secondary = EchoCyan,
    background = EchoDarkBg,
    surface = EchoSurface,
    surfaceVariant = EchoSurfaceVariant,
    onPrimary = EchoTextPrimary,
    onSecondary = EchoDarkBg,
    onBackground = EchoTextPrimary,
    onSurface = EchoTextPrimary,
    onSurfaceVariant = EchoTextSecondary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EchoOrange,
    secondary = EchoCyan,
    background = EchoDarkBg,
    surface = EchoSurface,
    surfaceVariant = EchoSurfaceVariant,
    onPrimary = EchoTextPrimary,
    onSecondary = EchoDarkBg,
    onBackground = EchoTextPrimary,
    onSurface = EchoTextPrimary,
    onSurfaceVariant = EchoTextSecondary
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      else -> DarkColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
