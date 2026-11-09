package com.hairacademy.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RoseGold = Color(0xFFE8B4A0)
val Heart = Color(0xFFFF6B8B)

private val Dark = darkColorScheme(
    primary = RoseGold,
    onPrimary = Color(0xFF3B1F14),
    secondary = Color(0xFFC5A3FF),
    background = Color(0xFF141416),
    onBackground = Color(0xFFEDE6E3),
    surface = Color(0xFF1E1E22),
    onSurface = Color(0xFFEDE6E3),
    surfaceVariant = Color(0xFF2A2A30),
    onSurfaceVariant = Color(0xFFB9B2AF),
)

@Composable
fun HairTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, content = content)
}
