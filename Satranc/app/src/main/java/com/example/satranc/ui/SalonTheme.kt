package com.example.satranc.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF070A12)
val InkRaised = Color(0xFF121826)
val InkLine = Color(0xFF243044)
val Gold = Color(0xFFE4C27A)
val GoldDeep = Color(0xFF8C6A32)
val Ivory = Color(0xFFF4EFE4)
val Mist = Color(0xFF9AA3B8)
val Danger = Color(0xFFE15B64)
val Good = Color(0xFF3DDC97)

private val scheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1408),
    secondary = Color(0xFF8EB4FF),
    background = Ink,
    surface = InkRaised,
    onBackground = Ivory,
    onSurface = Ivory,
    surfaceVariant = Color(0xFF1C2433),
    outline = InkLine
)

private val type = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp,
        letterSpacing = 4.sp,
        color = Gold
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        color = Ivory
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = Ivory
    ),
    bodyMedium = TextStyle(fontSize = 14.sp, color = Mist, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 11.sp, letterSpacing = 0.6.sp, color = Mist)
)

@Composable
fun SalonTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}

data class BoardTheme(
    val id: String,
    val light: Color,
    val dark: Color,
    val frame: Color,
    val last: Color,
    val select: Color,
    val dot: Color,
    val coord: Color
)

val BoardThemes = listOf(
    BoardTheme("walnut", Color(0xFFF0D9B5), Color(0xFFB58863), Color(0xFF5C3B22), Color(0xFFCDD26A), Gold, Color(0xFF3E2A16), Color(0xFFF8E7C9)),
    BoardTheme("tournament", Color(0xFFEEEED2), Color(0xFF769656), Color(0xFF2E3D24), Color(0xFFF6F669), Color(0xFFE8C547), Color(0xFF1E2A14), Color(0xFFF4F4D8)),
    BoardTheme("emerald", Color(0xFFE7F6EF), Color(0xFF1F6B4A), Color(0xFF0E3324), Color(0xFFF2E38A), Color(0xFFFFE08A), Color(0xFF083222), Color(0xFFD7F3E6)),
    BoardTheme("ocean", Color(0xFFD7E7F5), Color(0xFF1E4E79), Color(0xFF0E2438), Color(0xFF8FD0FF), Color(0xFFFFD56A), Color(0xFF082033), Color(0xFFE7F2FB)),
    BoardTheme("royal", Color(0xFFE8DCF6), Color(0xFF5A3D86), Color(0xFF2A1844), Color(0xFFE7B6FF), Gold, Color(0xFF1A0E2C), Color(0xFFF3E9FF)),
    BoardTheme("marble", Color(0xFFF7F4EF), Color(0xFF8D8D8D), Color(0xFF2C2C2C), Color(0xFFFFF3A3), Color(0xFF1A1A1A), Color(0xFF222222), Color(0xFFFFFFFF)),
    BoardTheme("rose", Color(0xFFFBE4E6), Color(0xFF8E4B55), Color(0xFF3D2226), Color(0xFFFFF0A8), Color(0xFFFFE1E4), Color(0xFF3A1C22), Color(0xFFFFF1F2)),
    BoardTheme("ice", Color(0xFFF4FBFF), Color(0xFF7F97A8), Color(0xFF1C2A33), Color(0xFFB9F3FF), Color(0xFF123040), Color(0xFF102028), Color(0xFFFFFFFF)),
    BoardTheme("coral", Color(0xFFFFF1E6), Color(0xFFC4623A), Color(0xFF3F2418), Color(0xFFFFE08A), Color(0xFF2A140C), Color(0xFF3A1E12), Color(0xFFFFF6EE)),
    BoardTheme("obsidian", Color(0xFF6E7484), Color(0xFF1A1E28), Color(0xFF05060A), Color(0xFF3DDC97), Gold, Color(0xFFE4C27A), Color(0xFFD5DBE8))
)

fun boardTheme(id: String): BoardTheme = BoardThemes.firstOrNull { it.id == id } ?: BoardThemes.first()

enum class PieceStyle { CLASSIC, GOLD, NIGHT }

data class PiecePalette(val light: Color, val dark: Color, val lightRim: Color, val darkRim: Color)

fun palette(style: PieceStyle): PiecePalette = when (style) {
    PieceStyle.CLASSIC -> PiecePalette(Color(0xFFF7F3EA), Color(0xFF1A1D27), Color(0xFFC6A15B), Color(0xFF9AA3B5))
    PieceStyle.GOLD -> PiecePalette(Color(0xFFF8E2A8), Color(0xFF6B4218), Color(0xFFFFF6D8), Color(0xFFE7B15A))
    PieceStyle.NIGHT -> PiecePalette(Color(0xFFF4FBFF), Color(0xFF101820), Color(0xFF7EE0FF), Color(0xFF3DDC97))
}
