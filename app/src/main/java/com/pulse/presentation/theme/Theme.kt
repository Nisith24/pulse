package com.pulse.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.pulse.R

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val InterFont = GoogleFont("Inter")

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Light),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = fontProvider, weight = FontWeight.Bold),
)

private val PulseTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)

private val PulseShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Dark-first premium palette — deep navy + teal accents
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DD0E1),          // Teal accent
    onPrimary = Color(0xFF003238),
    primaryContainer = Color(0xFF004D56),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = Color(0xFF80CBC4),         // Soft teal
    onSecondary = Color(0xFF00332E),
    secondaryContainer = Color(0xFF1A4D47),
    onSecondaryContainer = Color(0xFFA7F5EC),
    tertiary = Color(0xFFB0BEC5),
    background = Color(0xFF0D1117),        // Deep navy
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF161B22),           // Slightly lighter navy
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E2530),
    onSurfaceVariant = Color(0xFFA8B5C4),
    error = Color(0xFFFF6B6B),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF2D3748),
    outlineVariant = Color(0xFF1E2530),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE8EDF2),
    onSurfaceVariant = Color(0xFF44474F),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFCBD5E0),
)

@Composable
fun PulseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        shapes = PulseShapes,
        content = content
    )
}
