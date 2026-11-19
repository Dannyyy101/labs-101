package tech.project101.labs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// the yard ops tokens of the web (frontend/app/globals.css) and of ios/labs-101/Core/Design/Theme.swift

@Immutable
data class LabsColors(
    val canvas: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val line: Color,
    val track: Color,
    val ink: Color,
    val inkMuted: Color,
    val brand: Color,
    val brandStrong: Color,
    val brandSoft: Color,
    val onBrand: Color,
    val success: Color,
    val successSoft: Color,
    val successBar: Color,
    val warning: Color,
    val warningSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    /** categorical series for charts with several series */
    val series1: Color,
    val series2: Color,
    val series3: Color,
    val series4: Color,
) {
    val protein get() = series1
    val carbohydrates get() = series2
    val fat get() = series3
}

private val Light = LabsColors(
    canvas = Color(0xFFD6DEF6), surfaceRaised = Color(0xFFFFFFFF), surfaceSunken = Color(0xFFEEF1F7),
    line = Color(0xFFE3E7F0), track = Color(0xFFDFE4EE), ink = Color(0xFF0F1424), inkMuted = Color(0xFF5F677A),
    brand = Color(0xFF3560E0), brandStrong = Color(0xFF2747C2), brandSoft = Color(0xFFE6ECFD), onBrand = Color(0xFFFFFFFF),
    success = Color(0xFF23763C), successSoft = Color(0xFFDEF3E5), successBar = Color(0xFF338548),
    warning = Color(0xFF9A5208), warningSoft = Color(0xFFFDF0D6), danger = Color(0xFFC42D2D), dangerSoft = Color(0xFFFDE4E4),
    series1 = Color(0xFF3560E0), series2 = Color(0xFFC4721F), series3 = Color(0xFF13917F), series4 = Color(0xFF8B5CF6),
)

private val Dark = LabsColors(
    canvas = Color(0xFF1E2742), surfaceRaised = Color(0xFF19213A), surfaceSunken = Color(0xFF0F1526),
    line = Color(0xFF2B3552), track = Color(0xFF2F3A58), ink = Color(0xFFEEF1F8), inkMuted = Color(0xFFA3ACC2),
    brand = Color(0xFF6F8FF7), brandStrong = Color(0xFF9DB2FF), brandSoft = Color(0xFF283A7A), onBrand = Color(0xFF0B1020),
    success = Color(0xFF6FD38B), successSoft = Color(0xFF163824), successBar = Color(0xFF4FBF6C),
    warning = Color(0xFFF2AE5C), warningSoft = Color(0xFF3B2A10), danger = Color(0xFFF47A7A), dangerSoft = Color(0xFF3F1A1C),
    series1 = Color(0xFF5B7CF0), series2 = Color(0xFFC97D2C), series3 = Color(0xFF2A9D8A), series4 = Color(0xFF8F74E6),
)

val LocalLabsColors = staticCompositionLocalOf { Light }

object Labs {
    val colors: LabsColors
        @Composable get() = LocalLabsColors.current
}

@Composable
fun LabsTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val c = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.brand, onPrimary = c.onBrand, primaryContainer = c.brandSoft, onPrimaryContainer = c.brandStrong,
            secondary = c.brandStrong, background = c.canvas, onBackground = c.ink, surface = c.surfaceRaised,
            onSurface = c.ink, surfaceVariant = c.surfaceSunken, onSurfaceVariant = c.inkMuted,
            surfaceContainer = c.surfaceRaised, surfaceContainerHigh = c.surfaceRaised, surfaceContainerLow = c.surfaceRaised,
            outline = c.line, outlineVariant = c.line, error = c.danger, errorContainer = c.dangerSoft,
        )
    } else {
        lightColorScheme(
            primary = c.brand, onPrimary = c.onBrand, primaryContainer = c.brandSoft, onPrimaryContainer = c.brandStrong,
            secondary = c.brandStrong, background = c.canvas, onBackground = c.ink, surface = c.surfaceRaised,
            onSurface = c.ink, surfaceVariant = c.surfaceSunken, onSurfaceVariant = c.inkMuted,
            surfaceContainer = c.surfaceRaised, surfaceContainerHigh = c.surfaceRaised, surfaceContainerLow = c.surfaceRaised,
            outline = c.line, outlineVariant = c.line, error = c.danger, errorContainer = c.dangerSoft,
        )
    }
    CompositionLocalProvider(LocalLabsColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
