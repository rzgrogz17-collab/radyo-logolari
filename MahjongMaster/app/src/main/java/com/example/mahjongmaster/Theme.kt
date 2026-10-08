package com.example.mahjongmaster

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

// ============================================================
// RENK PALETİ — Merkezi renk yönetimi
// ============================================================
object GameColors {
    val dayTileSide = Color(0xFF1B5E20)
    val dayTileSideBorder = Color(0xFF003300)

    val cardBg = Color(0xFF37474F)
    val selectedBorder = Color(0xFF00E676)
    val hintBorder = Color(0xFFD500F9)
    val shadow = Color(0x80000000)
    val mutedText = Color(0xFFB0BEC5)

    val btnNew = Color(0xFF42A5F5)
    val btnHint = Color(0xFFEF5350)
    val btnMix = Color(0xFFAB47BC)
    val btnMode = Color(0xFFFFEE58)
    val btnMusic = Color(0xFF26A69A)
    val btnUndo = Color(0xFFFF7043)
    val btnWin = Color(0xFF43A047)

    val gold = Color(0xFFFFD54F)
    val goldLight = Color(0xFFFFF6D0)
    val goldDeep = Color(0xFFC9910F)
    val goldText = Color(0xFFFFE082)
    val ink = Color(0xFF1A1206)
}

/**
 * Oyun tahtası temaları — Ayarlar > Tema bölümünden seçilir.
 * JADE, oyunun önceki sürümündeki zümrüt/yeşim sahnenin geliştirilmiş hâlidir (varsayılan).
 */
enum class BoardTheme(
    @StringRes val titleRes: Int,
    val sceneTop: Color,
    val sceneMid: Color,
    val sceneBottom: Color,
    val glowA: Color,
    val glowB: Color,
    val glowC: Color,
    val accent: Color,
    val tileBack: Color,
    val tileBackDark: Color,
    val panelTop: Color,
    val panelBottom: Color
) {
    JADE(
        R.string.theme_jade,
        sceneTop = Color(0xFF0E4438), sceneMid = Color(0xFF0A6B57), sceneBottom = Color(0xFF061F1B),
        glowA = Color(0xFF3DECD1), glowB = Color(0xFFFFD54F), glowC = Color(0xFFFF8A80),
        accent = Color(0xFFFFD54F),
        tileBack = Color(0xFF1FA07C), tileBackDark = Color(0xFF0B5A45),
        panelTop = Color(0xFF15594A), panelBottom = Color(0xFF08201B)
    ),
    MIDNIGHT(
        R.string.theme_midnight,
        sceneTop = Color(0xFF151C44), sceneMid = Color(0xFF232F6B), sceneBottom = Color(0xFF070A1C),
        glowA = Color(0xFF7C8CFF), glowB = Color(0xFFFFD54F), glowC = Color(0xFFB388FF),
        accent = Color(0xFFFFD54F),
        tileBack = Color(0xFF4655C4), tileBackDark = Color(0xFF1C2378),
        panelTop = Color(0xFF28327A), panelBottom = Color(0xFF0A0E27)
    ),
    SAKURA(
        R.string.theme_sakura,
        sceneTop = Color(0xFF4A1942), sceneMid = Color(0xFF80305F), sceneBottom = Color(0xFF1C0A1B),
        glowA = Color(0xFFFFB7D5), glowB = Color(0xFFFFD54F), glowC = Color(0xFFFF8A80),
        accent = Color(0xFFFFCF6E),
        tileBack = Color(0xFFD23A78), tileBackDark = Color(0xFF7D1240),
        panelTop = Color(0xFF6E2658), panelBottom = Color(0xFF22091F)
    ),
    LACQUER(
        R.string.theme_lacquer,
        sceneTop = Color(0xFF5C1010), sceneMid = Color(0xFF921D1D), sceneBottom = Color(0xFF1C0404),
        glowA = Color(0xFFFF7043), glowB = Color(0xFFFFD54F), glowC = Color(0xFFFFAB91),
        accent = Color(0xFFFFD54F),
        tileBack = Color(0xFF2B2B2B), tileBackDark = Color(0xFF0E0E0E),
        panelTop = Color(0xFF7A1A1A), panelBottom = Color(0xFF250606)
    ),
    OCEAN(
        R.string.theme_ocean,
        sceneTop = Color(0xFF06344A), sceneMid = Color(0xFF0B607C), sceneBottom = Color(0xFF021822),
        glowA = Color(0xFF4DD0E1), glowB = Color(0xFFFFD54F), glowC = Color(0xFF80DEEA),
        accent = Color(0xFFFFD54F),
        tileBack = Color(0xFF0097A7), tileBackDark = Color(0xFF004F58),
        panelTop = Color(0xFF0D4F66), panelBottom = Color(0xFF031A24)
    );

    companion object {
        fun fromName(name: String?): BoardTheme = entries.firstOrNull { it.name == name } ?: JADE
    }
}

/** Taş yüzü stili — IVORY: klasik fildişi (assets/tiles/regular), ONYX: siyah taş (assets/tiles/black). */
enum class TileStyle(
    @StringRes val titleRes: Int,
    val assetFolder: String,
    val faceTop: Color,
    val faceBottom: Color,
    val body: Color,
    val faceBorder: Color,
    val hakuFrame: Color
) {
    IVORY(
        R.string.tile_ivory, "regular",
        faceTop = Color(0xFFFFFDF5), faceBottom = Color(0xFFF0E2C2),
        body = Color(0xFFE3D2AA), faceBorder = Color(0xFFD6C49B),
        hakuFrame = Color(0xFF4F83CC)
    ),
    ONYX(
        R.string.tile_onyx, "black",
        faceTop = Color(0xFF4A4A53), faceBottom = Color(0xFF1E1E24),
        body = Color(0xFF2C2C33), faceBorder = Color(0xFF5E5E68),
        hakuFrame = Color(0xFFB0BEC5)
    );

    fun assetPath(type: String) = "file:///android_asset/tiles/$assetFolder/$type.svg"

    companion object {
        fun fromName(name: String?): TileStyle = entries.firstOrNull { it.name == name } ?: IVORY
    }
}
