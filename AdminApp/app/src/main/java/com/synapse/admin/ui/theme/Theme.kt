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

private val SynapseDarkColorScheme = darkColorScheme(
    primary = SynapsePrimary,
    onPrimary = SynapseOnPrimary,
    primaryContainer = SynapsePrimaryContainer,
    onPrimaryContainer = SynapseOnPrimaryContainer,
    secondary = SynapseSecondary,
    onSecondary = SynapseOnSecondary,
    secondaryContainer = SynapseSecondaryContainer,
    onSecondaryContainer = SynapseOnSecondaryContainer,
    tertiary = SynapseTertiary,
    onTertiary = SynapseOnTertiary,
    background = SynapseDarkBackground,
    surface = SynapseDarkSurface,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SynapseDarkCard,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val SynapseLightColorScheme = lightColorScheme(
    primary = SynapsePrimary,
    onPrimary = SynapseOnPrimary,
    primaryContainer = SynapsePrimaryContainer,
    onPrimaryContainer = SynapseOnPrimaryContainer,
    secondary = SynapseSecondary,
    onSecondary = SynapseOnSecondary,
    secondaryContainer = SynapseSecondaryContainer,
    onSecondaryContainer = SynapseOnSecondaryContainer,
    tertiary = SynapseTertiary,
    onTertiary = SynapseOnTertiary,
    background = SynapseLightBackground,
    surface = SynapseLightSurface,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SynapseLightCard,
    onSurfaceVariant = Color(0xFF475569)
)

private val AdminDarkColorScheme = darkColorScheme(
    primary = AdminPrimary,
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = AdminSecondary,
    onSecondary = Color(0xFF082F49),
    background = AdminDarkBackground,
    surface = AdminDarkSurface,
    onBackground = Color(0xFFF9FAFB),
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFF9CA3AF),
    error = AdminError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAdmin: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isAdmin -> AdminDarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SynapseDarkColorScheme
        else -> SynapseLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
