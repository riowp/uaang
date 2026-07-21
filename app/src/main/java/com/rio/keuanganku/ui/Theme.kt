package com.rio.keuanganku.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AdaroGreen = Color(0xFF009540)
val AdaroDark = Color(0xFF005840)
val AdaroYellow = Color(0xFFDDD40E)
val RedMoney = Color(0xFFE05252)
val GreenBright = Color(0xFF34C77B)

private val LightScheme = lightColorScheme(
    primary = AdaroGreen,
    onPrimary = Color.White,
    secondary = AdaroDark,
    tertiary = AdaroYellow,
    primaryContainer = Color(0xFFD8F1E2),
    onPrimaryContainer = AdaroDark,
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFF5F1),
    onSurfaceVariant = Color(0xFF5A6B60)
)

private val DarkScheme = darkColorScheme(
    primary = GreenBright,
    onPrimary = Color(0xFF00391C),
    secondary = Color(0xFF8FD9B6),
    tertiary = AdaroYellow,
    primaryContainer = Color(0xFF00402C),
    onPrimaryContainer = Color(0xFFB6F0D2),
    background = Color(0xFF101413),
    surface = Color(0xFF181D1B),
    surfaceVariant = Color(0xFF232926),
    onSurfaceVariant = Color(0xFF9DB0A5)
)

@Composable
fun KeuanganKuTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
