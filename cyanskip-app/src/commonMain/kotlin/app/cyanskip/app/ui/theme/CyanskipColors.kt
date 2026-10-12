package app.cyanskip.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Shared brand palette keeps Android and the desktop preview consistent.
fun cyanskipColors(dark: Boolean) = if (dark) darkColorScheme(
    primary = Color(0xFF66D9DF), onPrimary = Color(0xFF00373A),
    primaryContainer = Color(0xFF005055), onPrimaryContainer = Color(0xFF9CF1F5),
    inversePrimary = Color(0xFF006970),
    secondary = Color(0xFFB0CCCC), onSecondary = Color(0xFF1B3435),
    secondaryContainer = Color(0xFF324B4C), onSecondaryContainer = Color(0xFFCCE8E8),
    tertiary = Color(0xFFA9C7E8), onTertiary = Color(0xFF10324E),
    tertiaryContainer = Color(0xFF2C4966), onTertiaryContainer = Color(0xFFD0E4FF),
    background = Color(0xFF0E1415), onBackground = Color(0xFFDEE4E4),
    surface = Color(0xFF0E1415), onSurface = Color(0xFFDEE4E4),
    surfaceVariant = Color(0xFF3F4949), onSurfaceVariant = Color(0xFFBEC8C8),
    surfaceContainerLowest = Color(0xFF090F10), surfaceContainerLow = Color(0xFF171D1E),
    surfaceContainer = Color(0xFF1B2122), surfaceContainerHigh = Color(0xFF252C2C),
    surfaceContainerHighest = Color(0xFF303737), surfaceTint = Color(0xFF66D9DF),
    outline = Color(0xFF889393), outlineVariant = Color(0xFF3F4949),
) else lightColorScheme(
    primary = Color(0xFF006970), onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F5), onPrimaryContainer = Color(0xFF002022),
    inversePrimary = Color(0xFF66D9DF),
    secondary = Color(0xFF496364), onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E8), onSecondaryContainer = Color(0xFF042021),
    tertiary = Color(0xFF44617E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0E4FF), onTertiaryContainer = Color(0xFF001D34),
    background = Color(0xFFF4FAFA), onBackground = Color(0xFF161D1D),
    surface = Color(0xFFF4FAFA), onSurface = Color(0xFF161D1D),
    surfaceVariant = Color(0xFFDAE5E5), onSurfaceVariant = Color(0xFF3F4949),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFEEF4F4),
    surfaceContainer = Color(0xFFE8EEEE), surfaceContainerHigh = Color(0xFFE2E8E8),
    surfaceContainerHighest = Color(0xFFDCE2E2), surfaceTint = Color(0xFF006970),
    outline = Color(0xFF6F7979), outlineVariant = Color(0xFFBEC8C8),
)
