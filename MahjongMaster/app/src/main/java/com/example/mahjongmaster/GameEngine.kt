package com.example.mahjongmaster

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// ============================================================
// OYUN AYARLARI — Puanlama ve animasyon sabitleri (TEK yerden yönetim)
// ============================================================
object GameTuning {
    // PUANLAMA (doğrusal ve sınırlı):
    //   puan = TABAN + (ardışık_eşleşme_sayısı * COMBO_ADIMI) + (katman_bonusu)
    const val BASE_MATCH_SCORE = 15L
    const val COMBO_BONUS_STEP = 8L
    const val MAX_COMBO_STREAK = 15L
    const val LAYER_BONUS_PER_LEVEL = 4L

    // Eşleşme animasyonu — taşların havalanıp ortada çarpışması
    const val MATCH_ANIM_DURATION_MS = 460

    // Kapalı (henüz seçilemeyen) taşların görünümü
    const val LOCKED_TILE_DIM = 0.30f
    const val LOCKED_TILE_SATURATION = 0.35f
    const val LOCKED_TILE_FROST_ALPHA = 0.10f

    // Kullanım hakları (reklam izlenince yenilenen miktarlar)
    const val HINTS_PER_REFILL = 3
    const val SHUFFLES_PER_REFILL = 10

    // Süre, zaman bonusu ve yıldız hesabı
    const val PAR_SECONDS_PER_PAIR = 7
    const val TIME_BONUS_PER_SECOND = 2L
    const val ASSISTS_FOR_STAR_PENALTY = 3
}

// ============================================================
// İNTERNET DURUMU YARDIMCISI
// ============================================================
object NetworkUtils {
    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            false
        }
    }
}

// ============================================================
// VERİ MODELLERİ
// ============================================================
data class Tile(
    val id: Int,
    val type: String,
    val gridX: Int,
    val gridY: Int,
    val layer: Int,
    val isVisible: Boolean = true,
    val isSelected: Boolean = false,
    val isSelectable: Boolean = true,
    val isHinted: Boolean = false,
    // Eşleşen çift önce isMatched=true olarak işaretlenip havalanma/çarpışma
    // animasyonu oynatılır, ardından gerçekten gizlenir (isVisible=false).
    // matchPairId aynı çiftin iki taşını birbirine bağlar.
    val isMatched: Boolean = false,
    val matchPairId: Int? = null
)

// comboBefore bir "çarpan" değil, ardışık eşleşme SAYACIdır (streak).
data class MoveRecord(
    val tile1: Tile,
    val tile2: Tile,
    val scoreBefore: Long,
    val comboBefore: Long
)

object TileAssets {
    val allTileNames = listOf(
        "Man1", "Man2", "Man3", "Man4", "Man5", "Man6", "Man7", "Man8", "Man9",
        "Pin1", "Pin2", "Pin3", "Pin4", "Pin5", "Pin6", "Pin7", "Pin8", "Pin9",
        "Sou1", "Sou2", "Sou3", "Sou4", "Sou5", "Sou6", "Sou7", "Sou8", "Sou9",
        "Ton", "Nan", "Shaa", "Pei", "Haku", "Hatsu", "Chun"
    )
}

/** Zorluk grubu — LevelPatterns.randomLayoutForLevel ile aynı sınırlar. */
enum class Difficulty(@StringRes val titleRes: Int) {
    EASY(R.string.difficulty_easy),
    MEDIUM(R.string.difficulty_medium),
    HARD(R.string.difficulty_hard);

    companion object {
        fun forLevel(level: Int): Difficulty = when {
            level <= 10 -> EASY
            level <= 30 -> MEDIUM
            else -> HARD
        }
    }
}

data class GameSettings(
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val dimBlockedTiles: Boolean = true,
    val highlightPairs: Boolean = false,
    val boardTheme: BoardTheme = BoardTheme.JADE,
    val tileStyle: TileStyle = TileStyle.IVORY
)

data class GameStats(
    val levelsWon: Int = 0,
    val totalMatches: Int = 0,
    val bestCombo: Int = 0,
    val fastestLevelMs: Long = 0L,
    val totalPlayMs: Long = 0L,
    val totalStars: Int = 0,
    val threeStarLevels: Int = 0,
    val hintsUsed: Int = 0,
    val shufflesUsed: Int = 0,
    val highestLevel: Int = 1
)

/** Bir level'ın süre/yardım durumu — kayıtla birlikte saklanır. */
data class LevelProgress(
    val elapsedMs: Long = 0L,
    val hintsUsed: Int = 0,
    val shufflesUsed: Int = 0,
    val bestCombo: Int = 0
)

data class WinSummary(
    val level: Int,
    val matchScore: Long,
    val timeBonus: Long,
    val totalScore: Long,
    val elapsedSeconds: Int,
    val parSeconds: Int,
    val stars: Int,
    val bestCombo: Int,
    val isNewRecord: Boolean
)

/** Arayüzün tek seferlik tepki verdiği oyun olayları (titreşim, puan balonu, sallanma). */
sealed interface GameEvent {
    data object Selected : GameEvent
    data class Matched(val tileId1: Int, val tileId2: Int, val points: Long, val streak: Int) : GameEvent
    data class Blocked(val tileId: Int) : GameEvent
    data object Won : GameEvent
}

// ============================================================
// LEVEL SİSTEMİ — Çok Sayıda Rastgele Dizilim (kompakt aralık 0..10 / 0..12)
// ============================================================
object LevelPatterns {

    // -- 1: Klasik Piramit (kompakt) --
    fun layout1(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        for (x in 0..10 step 2) for (y in 0..10 step 2) p.add(Triple(x, y, 0))
        for (x in 2..8 step 2) for (y in 2..8 step 2) p.add(Triple(x, y, 1))
        for (x in 4..6 step 2) for (y in 4..6 step 2) p.add(Triple(x, y, 2))
        // tepe — yan yana iki taş
        p.add(Triple(4, 5, 3))
        p.add(Triple(6, 5, 3))
        return p
    }

    // -- 2: Kelebek/Çift Kanat --
    fun layout2(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            2 to 0, 4 to 0, 6 to 0, 8 to 0,
            1 to 2, 3 to 2, 5 to 2, 7 to 2, 9 to 2,
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            0 to 6, 2 to 6, 4 to 6, 6 to 6, 8 to 6, 10 to 6,
            1 to 8, 3 to 8, 5 to 8, 7 to 8, 9 to 8,
            2 to 10, 4 to 10, 6 to 10, 8 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 2..8 step 2) for (y in 2..8 step 2) p.add(Triple(x, y, 1))
        for (x in 4..6 step 2) p.add(Triple(x, 5, 2))
        return p
    }

    // -- 3: Yatay Köprü/Çubuk --
    fun layout3(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        for (x in 0..10 step 2) {
            p.add(Triple(x, 2, 0)); p.add(Triple(x, 4, 0))
            p.add(Triple(x, 6, 0)); p.add(Triple(x, 8, 0))
        }
        for (x in 2..8 step 2) {
            p.add(Triple(x, 3, 1)); p.add(Triple(x, 5, 1)); p.add(Triple(x, 7, 1))
        }
        for (x in 4..6 step 2) p.add(Triple(x, 5, 2))
        // tepe — yan yana iki taş
        p.add(Triple(3, 5, 3))
        p.add(Triple(7, 5, 3))
        return p
    }

    // -- 4: Çatı/Üçgen --
    fun layout4(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            0 to 8, 2 to 8, 4 to 8, 6 to 8, 8 to 8, 10 to 8,
            1 to 6, 3 to 6, 5 to 6, 7 to 6, 9 to 6,
            2 to 4, 4 to 4, 6 to 4, 8 to 4,
            3 to 2, 5 to 2, 7 to 2,
            4 to 0, 6 to 0,
            // alt taban
            0 to 10, 2 to 10, 4 to 10, 6 to 10, 8 to 10, 10 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 2..8 step 2) p.add(Triple(x, 6, 1))
        for (x in 4..6 step 2) p.add(Triple(x, 4, 1))
        // tepe — yan yana iki taş
        p.add(Triple(4, 5, 2))
        p.add(Triple(6, 5, 2))
        return p
    }

    // -- 5: Ejderha (asimetrik) --
    fun layout5(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // baş
            0 to 0, 2 to 0,
            1 to 2, 3 to 2,
            // gövde
            2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            3 to 6, 5 to 6, 7 to 6, 9 to 6,
            2 to 8, 4 to 8, 6 to 8, 8 to 8, 10 to 8,
            // kuyruk
            6 to 10, 8 to 10, 10 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 4..8 step 2) p.add(Triple(x, 5, 1))
        for (x in 4..8 step 2) p.add(Triple(x, 7, 1))
        // tepe — yan yana iki taş
        p.add(Triple(5, 6, 2))
        p.add(Triple(7, 6, 2))
        return p
    }

    // -- 6: Çift Kule --
    fun layout6(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        for (y in 0..10 step 2) {
            p.add(Triple(0, y, 0)); p.add(Triple(2, y, 0))
            p.add(Triple(8, y, 0)); p.add(Triple(10, y, 0))
        }
        for (x in 4..6 step 2) for (y in 2..8 step 2) p.add(Triple(x, y, 0))
        // 2. kat - kuleleri kalınlaştırır (yatay komşuları olan, sığ stack)
        for (y in 4..6 step 2) {
            p.add(Triple(0, y, 1)); p.add(Triple(2, y, 1))
            p.add(Triple(8, y, 1)); p.add(Triple(10, y, 1))
        }
        // tepe — yan yana iki taş
        p.add(Triple(4, 5, 1))
        p.add(Triple(6, 5, 1))
        return p
    }

    // -- 7: Diamond / Eşkenar --
    fun layout7(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            5 to 0,
            3 to 2, 5 to 2, 7 to 2,
            1 to 4, 3 to 4, 5 to 4, 7 to 4, 9 to 4,
            0 to 6, 2 to 6, 4 to 6, 6 to 6, 8 to 6, 10 to 6,
            1 to 8, 3 to 8, 5 to 8, 7 to 8, 9 to 8,
            3 to 10, 5 to 10, 7 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 3..7 step 2) for (y in 4..8 step 2) p.add(Triple(x, y, 1))
        for (x in 4..6 step 2) p.add(Triple(x, 6, 2))
        return p
    }

    // -- 8: Yengeç (geniş yan) --
    fun layout8(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // sol kıskaç - genişletildi
            0 to 0, 0 to 2, 0 to 4,
            2 to 0, 2 to 2, 2 to 4,
            // sağ kıskaç - genişletildi
            10 to 0, 10 to 2, 10 to 4,
            8 to 0, 8 to 2, 8 to 4,
            // gövde
            3 to 4, 5 to 4, 7 to 4,
            2 to 6, 4 to 6, 6 to 6, 8 to 6,
            3 to 8, 5 to 8, 7 to 8,
            4 to 10, 6 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 4..6 step 2) for (y in 5..7 step 2) p.add(Triple(x, y, 1))
        // tepe — yan yana iki taş
        p.add(Triple(4, 6, 2))
        p.add(Triple(6, 6, 2))
        return p
    }

    // -- 9: Yıldız / Patlama --
    fun layout9(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        // gövde 5x5
        for (x in 2..8 step 2) for (y in 2..8 step 2) p.add(Triple(x, y, 0))
        // dış uçlar
        listOf(5 to 0, 0 to 5, 10 to 5, 5 to 10).forEach { p.add(Triple(it.first, it.second, 0)) }
        listOf(1 to 1, 9 to 1, 1 to 9, 9 to 9).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 4..6 step 2) for (y in 4..6 step 2) p.add(Triple(x, y, 1))
        // tepe — yan yana iki taş (üst üste değil)
        p.add(Triple(4, 5, 2))
        p.add(Triple(6, 5, 2))
        return p
    }

    // -- 10: Yılan / S Şekli --
    fun layout10(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            0 to 0, 2 to 0, 4 to 0, 6 to 0, 8 to 0,
            8 to 2, 8 to 4,
            6 to 4, 4 to 4, 2 to 4,
            2 to 6, 2 to 8,
            4 to 8, 6 to 8, 8 to 8, 10 to 8,
            10 to 10, 8 to 10, 6 to 10, 4 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 2..8 step 2) p.add(Triple(x, 1, 1))
        for (x in 4..6 step 2) p.add(Triple(x, 5, 1))
        for (x in 6..8 step 2) p.add(Triple(x, 9, 1))
        // tepe — yan yana iki taş
        p.add(Triple(4, 5, 2))
        p.add(Triple(6, 5, 2))
        return p
    }

    // -- 11: Tam Doluluk + Kule --
    fun layout11(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        for (x in 0..10 step 2) for (y in 0..10 step 2) p.add(Triple(x, y, 0))
        for (x in 2..8 step 2) for (y in 2..8 step 2) p.add(Triple(x, y, 1))
        for (x in 4..6 step 2) for (y in 4..6 step 2) p.add(Triple(x, y, 2))
        // tepe — yan yana iki taş
        p.add(Triple(4, 5, 3))
        p.add(Triple(6, 5, 3))
        return p
    }

    // -- 12: Plus + (Artı) --
    fun layout12(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        for (x in 4..6 step 2) for (y in 0..10 step 2) p.add(Triple(x, y, 0))
        for (y in 4..6 step 2) for (x in 0..10 step 2) p.add(Triple(x, y, 0))
        // ikinci kat
        for (x in 4..6 step 2) for (y in 2..8 step 2) p.add(Triple(x, y, 1))
        for (x in 2..8 step 2) p.add(Triple(x, 5, 1))
        // tepe — yan yana iki taş
        p.add(Triple(4, 5, 2))
        p.add(Triple(6, 5, 2))
        return p
    }

    // -- 13: H Şekli --
    fun layout13(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        for (y in 0..10 step 2) {
            p.add(Triple(0, y, 0)); p.add(Triple(2, y, 0))
            p.add(Triple(8, y, 0)); p.add(Triple(10, y, 0))
        }
        for (x in 4..6 step 2) for (y in 4..6 step 2) p.add(Triple(x, y, 0))
        // 2. kat - direkleri kalınlaştırır (yatay komşuları olan, sığ stack)
        for (y in 4..6 step 2) {
            p.add(Triple(0, y, 1)); p.add(Triple(2, y, 1))
            p.add(Triple(8, y, 1)); p.add(Triple(10, y, 1))
        }
        for (x in 4..6 step 2) p.add(Triple(x, 5, 1))
        // tepe — yan yana iki taş
        p.add(Triple(4, 4, 2))
        p.add(Triple(6, 4, 2))
        return p
    }

    // -- 14: Sandviç (alt-üst dolgu, ortası boşaltılmış) --
    fun layout14(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        for (x in 0..10 step 2) {
            p.add(Triple(x, 0, 0)); p.add(Triple(x, 2, 0))
            p.add(Triple(x, 8, 0)); p.add(Triple(x, 10, 0))
        }
        for (x in 4..6 step 2) for (y in 4..6 step 2) p.add(Triple(x, y, 0))
        // yan duvarlar
        p.add(Triple(0, 4, 0)); p.add(Triple(0, 6, 0))
        p.add(Triple(10, 4, 0)); p.add(Triple(10, 6, 0))
        for (x in 1..9 step 2) p.add(Triple(x, 1, 1))
        for (x in 1..9 step 2) p.add(Triple(x, 9, 1))
        for (x in 4..6 step 2) p.add(Triple(x, 5, 1))
        // tepe — yan yana iki taş
        p.add(Triple(4, 5, 2))
        p.add(Triple(6, 5, 2))
        return p
    }

    // -- 15: Mantar/Şemsiye --
    fun layout15(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // şapka
            0 to 0, 2 to 0, 4 to 0, 6 to 0, 8 to 0, 10 to 0,
            1 to 2, 3 to 2, 5 to 2, 7 to 2, 9 to 2,
            2 to 4, 4 to 4, 6 to 4, 8 to 4,
            // sap
            4 to 6, 6 to 6,
            4 to 8, 6 to 8,
            4 to 10, 6 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 2..8 step 2) p.add(Triple(x, 2, 1))
        for (x in 4..6 step 2) p.add(Triple(x, 4, 1))
        for (x in 4..6 step 2) p.add(Triple(x, 7, 1))
        // tepe — yan yana iki taş
        p.add(Triple(4, 3, 2))
        p.add(Triple(6, 3, 2))
        return p
    }

    // -- 16: Kalp --
    fun layout16(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            1 to 0, 3 to 0, 7 to 0, 9 to 0,
            0 to 2, 2 to 2, 4 to 2, 6 to 2, 8 to 2, 10 to 2,
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            1 to 6, 3 to 6, 5 to 6, 7 to 6, 9 to 6,
            2 to 8, 4 to 8, 6 to 8, 8 to 8,
            3 to 10, 5 to 10, 7 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        for (x in 2..8 step 2) for (y in 3..5 step 2) p.add(Triple(x, y, 1))
        for (x in 4..6 step 2) p.add(Triple(x, 5, 2))
        return p
    }

    // -- 17: Araba (Kolay) --
    fun layout17(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Gövde 2 sıra
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            0 to 6, 2 to 6, 4 to 6, 6 to 6, 8 to 6, 10 to 6,
            // Kabin
            2 to 2, 4 to 2, 6 to 2, 8 to 2,
            // Tekerlekler
            0 to 8, 2 to 8, 8 to 8, 10 to 8
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — yan yana iki taş (kabin üstü)
        p.add(Triple(4, 2, 1)); p.add(Triple(6, 2, 1))
        return p
    }

    // -- 18: Ev (Orta) --
    fun layout18(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Çatı tepesi
            4 to 0, 6 to 0,
            // Çatı
            2 to 2, 4 to 2, 6 to 2, 8 to 2,
            // Üst duvar
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            // Orta duvar
            0 to 6, 2 to 6, 4 to 6, 6 to 6, 8 to 6, 10 to 6,
            // Alt duvar
            0 to 8, 2 to 8, 4 to 8, 6 to 8, 8 to 8, 10 to 8,
            // Kapı
            4 to 10, 6 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — yan yana iki taş (pencere)
        p.add(Triple(4, 6, 1)); p.add(Triple(6, 6, 1))
        return p
    }

    // -- 19: Ağaç (Orta) --
    fun layout19(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Tepe
            4 to 0, 6 to 0,
            // Taç üst
            2 to 2, 4 to 2, 6 to 2, 8 to 2,
            // Taç en geniş
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            // Taç alt
            2 to 6, 4 to 6, 6 to 6, 8 to 6,
            // Gövde
            4 to 8, 6 to 8,
            4 to 10, 6 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — yan yana iki taş (taç süs)
        p.add(Triple(4, 3, 1)); p.add(Triple(6, 3, 1))
        return p
    }

    // -- 20: İnsan (Zor) --
    fun layout20(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Baş
            4 to 0, 6 to 0,
            4 to 2, 6 to 2,
            // Omuzlar + kollar
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            // Gövde
            4 to 6, 6 to 6,
            4 to 8, 6 to 8,
            // Bacaklar
            2 to 10, 4 to 10, 6 to 10, 8 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — yan yana iki taş (şapka)
        p.add(Triple(4, 0, 1)); p.add(Triple(6, 0, 1))
        return p
    }

    // -- 21: Üçgen (Zor) --
    fun layout21(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            4 to 0, 6 to 0,
            3 to 2, 5 to 2, 7 to 2,
            2 to 4, 4 to 4, 6 to 4, 8 to 4,
            1 to 6, 3 to 6, 5 to 6, 7 to 6, 9 to 6,
            0 to 8, 2 to 8, 4 to 8, 6 to 8, 8 to 8, 10 to 8
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — yan yana iki taş
        p.add(Triple(4, 4, 1)); p.add(Triple(6, 4, 1))
        return p
    }

    // -- 22: Halka (Kolay) --
    fun layout22(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Üst kavis
            4 to 0, 6 to 0,
            2 to 2, 4 to 2, 6 to 2, 8 to 2,
            // Sol & sağ kenar
            0 to 4, 2 to 4, 8 to 4, 10 to 4,
            0 to 6, 2 to 6, 8 to 6, 10 to 6,
            // Alt kavis
            2 to 8, 4 to 8, 6 to 8, 8 to 8,
            4 to 10, 6 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — yan yana iki taş
        p.add(Triple(4, 0, 1)); p.add(Triple(6, 0, 1))
        return p
    }

    // -- 23: Çiçek (Orta) --
    fun layout23(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Merkez 2x2
            4 to 4, 6 to 4,
            4 to 6, 6 to 6,
            // Üst yaprak
            4 to 0, 6 to 0,
            // Sol üst yaprak
            0 to 2, 2 to 2,
            // Sağ üst yaprak
            8 to 2, 10 to 2,
            // Sol alt yaprak
            0 to 8, 2 to 8,
            // Sağ alt yaprak
            8 to 8, 10 to 8,
            // Alt yaprak
            4 to 10, 6 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — yan yana iki taş (çiçek özü)
        p.add(Triple(4, 5, 1)); p.add(Triple(6, 5, 1))
        return p
    }

    // ============================================================
    // YENİ ŞEKİLLER (24-32) — "tablo" çeşitliliğini artırmak için eklendi.
    // Mevcut layout1..23 fonksiyonlarının hiçbiri değiştirilmedi/silinmedi.
    // ============================================================

    // -- 24: Kum Saati (Kolay) --
    fun layout24(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            0 to 0, 2 to 0, 4 to 0, 6 to 0, 8 to 0, 10 to 0,
            2 to 2, 4 to 2, 6 to 2, 8 to 2,
            4 to 4, 6 to 4,
            4 to 6, 6 to 6,
            2 to 8, 4 to 8, 6 to 8, 8 to 8,
            0 to 10, 2 to 10, 4 to 10, 6 to 10, 8 to 10, 10 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — bel kısmında yan yana iki taş
        p.add(Triple(4, 5, 1)); p.add(Triple(6, 5, 1))
        return p
    }

    // -- 25: Altıgen (Kolay) --
    fun layout25(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            4 to 0, 6 to 0,
            2 to 2, 4 to 2, 6 to 2, 8 to 2,
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            0 to 6, 2 to 6, 4 to 6, 6 to 6, 8 to 6, 10 to 6,
            2 to 8, 4 to 8, 6 to 8, 8 to 8,
            4 to 10, 6 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — yan yana iki taş
        p.add(Triple(4, 5, 1)); p.add(Triple(6, 5, 1))
        return p
    }

    // -- 26: Uçurtma (Kolay) --
    fun layout26(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Uçurtma gövdesi (baklava dilimi)
            4 to 0, 6 to 0,
            2 to 2, 4 to 2, 6 to 2, 8 to 2,
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            2 to 6, 4 to 6, 6 to 6, 8 to 6,
            4 to 8, 6 to 8,
            // Kuyruk (zikzak kurdele)
            3 to 10, 7 to 10,
            4 to 12, 6 to 12,
            3 to 14, 7 to 14
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — gövde üstünde küçük mücevher
        p.add(Triple(4, 4, 1)); p.add(Triple(6, 4, 1))
        return p
    }

    // -- 27: Balık (Orta) --
    fun layout27(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Kuyruk (üçgen yüzgeç)
            0 to 2, 0 to 8,
            2 to 4, 2 to 6,
            // Gövde (oval)
            4 to 2, 6 to 2,
            4 to 4, 6 to 4, 8 to 4, 10 to 4,
            4 to 6, 6 to 6, 8 to 6, 10 to 6,
            4 to 8, 6 to 8,
            // Baş / ağız
            10 to 2, 10 to 8,
            // Sırt yüzgeci
            6 to 0, 8 to 0
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — göz / mücevher
        p.add(Triple(8, 4, 1)); p.add(Triple(8, 6, 1))
        return p
    }

    // -- 28: Kaplumbağa (Orta) --
    fun layout28(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Baş
            4 to 0, 6 to 0,
            // Kabuk (gövde)
            2 to 2, 4 to 2, 6 to 2, 8 to 2,
            0 to 4, 2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            0 to 6, 2 to 6, 4 to 6, 6 to 6, 8 to 6, 10 to 6,
            2 to 8, 4 to 8, 6 to 8, 8 to 8,
            // Bacaklar (yanlara taşan)
            -2 to 4, -2 to 6, 12 to 4, 12 to 6,
            // Kuyruk
            4 to 10, 6 to 10
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — kabuk deseni
        p.add(Triple(4, 5, 1)); p.add(Triple(6, 5, 1))
        return p
    }

    // -- 29: Merdiven (Zor — 5 katmanlı çıkan basamaklar) --
    fun layout29(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(0 to 8, 2 to 8, 0 to 10, 2 to 10).forEach { p.add(Triple(it.first, it.second, 0)) }
        listOf(2 to 6, 4 to 6, 2 to 8, 4 to 8).forEach { p.add(Triple(it.first, it.second, 1)) }
        listOf(4 to 4, 6 to 4, 4 to 6, 6 to 6).forEach { p.add(Triple(it.first, it.second, 2)) }
        listOf(6 to 2, 8 to 2, 6 to 4, 8 to 4).forEach { p.add(Triple(it.first, it.second, 3)) }
        listOf(8 to 0, 10 to 0, 8 to 2, 10 to 2).forEach { p.add(Triple(it.first, it.second, 4)) }
        return p
    }

    // -- 30: Sarmal (Zor — iç içe halkalar) --
    fun layout30(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        // Dış halka
        for (x in 0..10 step 2) { p.add(Triple(x, 0, 0)); p.add(Triple(x, 10, 0)) }
        for (y in 2..8 step 2) { p.add(Triple(0, y, 0)); p.add(Triple(10, y, 0)) }
        // Orta halka
        for (x in 2..8 step 2) { p.add(Triple(x, 2, 1)); p.add(Triple(x, 8, 1)) }
        for (y in 4..6 step 2) { p.add(Triple(2, y, 1)); p.add(Triple(8, y, 1)) }
        // İç çekirdek
        p.add(Triple(4, 4, 2)); p.add(Triple(6, 4, 2))
        p.add(Triple(4, 6, 2)); p.add(Triple(6, 6, 2))
        // tepe — merkez mücevher
        p.add(Triple(4, 5, 3)); p.add(Triple(6, 5, 3))
        return p
    }

    // -- 31: Kale (Zor) --
    fun layout31(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        // Kale duvarı (kalın taban)
        for (x in 0..12 step 2) for (y in 6..12 step 2) p.add(Triple(x, y, 0))
        // Mazgallar (surun dişleri)
        for (x in 0..12 step 4) p.add(Triple(x, 4, 1))
        // Merkez kule gövdesi
        p.add(Triple(4, 2, 2)); p.add(Triple(6, 2, 2))
        // Kule tepesi / bayrak
        p.add(Triple(4, 0, 3)); p.add(Triple(6, 0, 3))
        return p
    }

    // -- 32: Taç (Orta) --
    fun layout32(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(
            // Taç bandı (taban)
            0 to 8, 2 to 8, 4 to 8, 6 to 8, 8 to 8, 10 to 8,
            0 to 10, 2 to 10, 4 to 10, 6 to 10, 8 to 10, 10 to 10,
            // Geçiş sırası
            0 to 6, 2 to 6, 4 to 6, 6 to 6, 8 to 6, 10 to 6,
            // Yan sivri uçlar (kısa)
            0 to 4, 10 to 4,
            // Orta sivri uç (uzun)
            4 to 4, 6 to 4,
            4 to 2, 6 to 2
        ).forEach { p.add(Triple(it.first, it.second, 0)) }
        // tepe — mücevher
        p.add(Triple(4, 0, 1)); p.add(Triple(6, 0, 1))
        return p
    }

    // -- 33: Haç — üstte basamaklı gövde, altta dar sap (28 taş) --
    fun layout33(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        p.add(Triple(0, 0, 0))
        listOf(-2, 0, 2).forEach { p.add(Triple(it, 2, 0)) }
        listOf(-4, -2, 0, 2, 4).forEach { p.add(Triple(it, 4, 0)) }
        listOf(-5, -3, -1, 1, 3, 5).forEach { p.add(Triple(it, 6, 0)) }
        listOf(-6, -4, -2, 0, 2, 4, 6).forEach { p.add(Triple(it, 8, 0)) }
        for (y in listOf(10, 12, 14)) {
            p.add(Triple(-1, y, 0))
            p.add(Triple(1, y, 0))
        }
        return p
    }

    // -- 34: Pencere — ortası açık çerçeve, yan kulaklar ve ayaklar (46 taş) --
    fun layout34(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(-5, 5).forEach { p.add(Triple(it, 0, 0)) }
        listOf(-8, -6, -4, -2, 0, 2, 4, 6, 8).forEach { p.add(Triple(it, 2, 0)) }
        listOf(-6, -4, -2, 0, 2, 4, 6).forEach { p.add(Triple(it, 4, 0)) }
        for (y in listOf(6, 8)) {
            listOf(-6, -4, 4, 6).forEach { p.add(Triple(it, y, 0)) }
        }
        listOf(-6, -4, -2, 0, 2, 4, 6).forEach { p.add(Triple(it, 10, 0)) }
        listOf(-8, -6, -4, -2, 0, 2, 4, 6, 8).forEach { p.add(Triple(it, 12, 0)) }
        for (y in listOf(14, 16)) {
            listOf(-5, 5).forEach { p.add(Triple(it, y, 0)) }
        }
        return p
    }

    // -- 35: Elmas — baklava gövde ve altta iki ayak (34 taş) --
    fun layout35(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        val rows = listOf(
            0 to listOf(0),
            2 to listOf(-2, 0, 2),
            4 to listOf(-4, -2, 0, 2, 4),
            6 to listOf(-6, -4, -2, 0, 2, 4, 6),
            8 to listOf(-6, -4, -2, 0, 2, 4, 6),
            10 to listOf(-4, -2, 0, 2, 4),
            12 to listOf(-2, 0, 2),
            14 to listOf(0)
        )
        rows.forEach { (y, xs) -> xs.forEach { p.add(Triple(it, y, 0)) } }
        p.add(Triple(-1, 16, 0))
        p.add(Triple(1, 16, 0))
        return p
    }

    // -- 36: Kale — dişli tepe, dolu gövde, alt kapı ve üst kat (54 taş) --
    fun layout36(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(-6, -2, 2, 6).forEach { p.add(Triple(it, 0, 0)) }
        for (y in listOf(2, 4, 6, 8, 10, 12)) {
            listOf(-6, -4, -2, 0, 2, 4, 6).forEach { p.add(Triple(it, y, 0)) }
        }
        listOf(-6, -4, 4, 6).forEach { p.add(Triple(it, 14, 0)) }
        listOf(-1, 1).forEach { x ->
            p.add(Triple(x, 4, 1))
            p.add(Triple(x, 6, 1))
        }
        return p
    }

    // -- 37: Figür — baş, yan kollar, gövde ve ayrık bacaklar (58 taş) --
    fun layout37(): List<Triple<Int, Int, Int>> {
        val p = mutableListOf<Triple<Int, Int, Int>>()
        listOf(-2, 0, 2).forEach { p.add(Triple(it, 0, 0)) }
        listOf(-4, -2, 0, 2, 4).forEach { p.add(Triple(it, 2, 0)) }
        listOf(-8, -6, -4, -2, 0, 2, 4, 6, 8).forEach { p.add(Triple(it, 4, 0)) }
        listOf(-6, -4, -2, 0, 2, 4, 6).forEach { p.add(Triple(it, 6, 0)) }
        for (y in listOf(8, 10)) {
            listOf(-4, -2, 0, 2, 4).forEach { p.add(Triple(it, y, 0)) }
        }
        for (y in listOf(12, 14, 16, 18)) {
            listOf(-6, -4, -2, 2, 4, 6).forEach { p.add(Triple(it, y, 0)) }
        }
        return p
    }

    // ZORLUK SEVİYELERİNE GÖRE GRUPLAR
    // Kolay: az katman, kompakt yapılar
    private val easyLayouts = listOf(
        ::layout7,   // Diamond (kompakt, az katman)
        ::layout3,   // Köprü/Çubuk (basit dikey katmanlar)
        ::layout4,   // Çatı/Üçgen
        ::layout15,  // Mantar (orta-küçük)
        ::layout16,  // Kalp
        ::layout17,  // Araba (YENİ)
        ::layout22,  // Halka (YENİ)
        ::layout24,  // Kum Saati (YENİ)
        ::layout25,  // Altıgen (YENİ)
        ::layout26,  // Uçurtma (YENİ)
        ::layout35   // Elmas
    )

    // Orta: dengeli katman ve genişlik
    private val mediumLayouts = listOf(
        ::layout1,   // Klasik Piramit
        ::layout2,   // Kelebek
        ::layout5,   // Ejderha
        ::layout8,   // Yengeç
        ::layout10,  // Yılan
        ::layout12,  // Plus
        ::layout13,  // H
        ::layout18,  // Ev (YENİ)
        ::layout19,  // Ağaç (YENİ)
        ::layout23,  // Çiçek (YENİ)
        ::layout27,  // Balık (YENİ)
        ::layout28,  // Kaplumbağa (YENİ)
        ::layout32,  // Taç (YENİ)
        ::layout33,  // Haç
        ::layout34   // Pencere
    )

    // Zor: çok katman, geniş gövde, dolgun yapılar
    private val hardLayouts = listOf(
        ::layout6,   // Çift Kule
        ::layout9,   // Yıldız (çok katman)
        ::layout11,  // Tam Doluluk + Kule (en yoğun)
        ::layout14,  // Sandviç
        ::layout20,  // İnsan (YENİ)
        ::layout21,  // Üçgen (YENİ)
        ::layout29,  // Merdiven (YENİ — 5 katmanlı)
        ::layout30,  // Sarmal (YENİ — iç içe halkalar)
        ::layout31,  // Kale (YENİ)
        ::layout36,  // Kale burcu
        ::layout37   // Figür
    )

    val allLevels = easyLayouts + mediumLayouts + hardLayouts

    // ============================================================
    // YENİ SEÇİM MANTIĞI — "ÇANTA" (bag) yöntemi
    // Eskisi: pool.random() + sadece "bir öncekiyle aynı olmasın" kontrolü.
    // Sorunu: aynı zorluktaki bazı şekiller şans eseri arka arkaya sık sık
    // çıkabiliyor, bazıları ise uzun süre hiç gelmeyebiliyordu.
    // Yenisi: her zorluk grubu için bir "çanta" tutulur; çantadaki TÜM
    // şekiller birer kez oynatılmadan hiçbiri ikinci kez gelmez. Çanta
    // boşalınca yeniden karıştırılıp doldurulur. Böylece tablo çeşitliliği
    // garanti altına alınır ve rastgelelik daha "adil" hale gelir.
    // ============================================================
    private val easyBag = mutableListOf<Int>()
    private val mediumBag = mutableListOf<Int>()
    private val hardBag = mutableListOf<Int>()
    private var lastGlobalPick: Any? = null

    private fun drawIndex(bag: MutableList<Int>, poolSize: Int): Int {
        if (bag.isEmpty()) bag.addAll((0 until poolSize).shuffled())
        return bag.removeAt(0)
    }

    /**
     * Level'a göre uygun zorluk grubundan, "çanta" yöntemiyle bir layout döndürür.
     * Level 1-10  → kolay
     * Level 11-30 → orta
     * Level 31+   → zor
     */
    fun randomLayoutForLevel(level: Int): List<Triple<Int, Int, Int>> {
        val pool = when {
            level <= 10 -> easyLayouts
            level <= 30 -> mediumLayouts
            else -> hardLayouts
        }
        val bag = when {
            level <= 10 -> easyBag
            level <= 30 -> mediumBag
            else -> hardBag
        }
        var pick = pool[drawIndex(bag, pool.size)]
        // Çanta sınırında bile art arda AYNI şekil gelmesin
        if (pool.size > 1 && pick === lastGlobalPick) {
            pick = pool[drawIndex(bag, pool.size)]
        }
        lastGlobalPick = pick
        return pick.invoke()
    }

    // Geri uyumluluk: parametresiz çağrı varsayılan kolay grup ile devam etsin
    fun randomLayout(): List<Triple<Int, Int, Int>> = randomLayoutForLevel(1)
}

// ============================================================
// REPOSITORY — kayıt, ayarlar ve istatistikler (SharedPreferences)
// Eski sürümlerin anahtarları aynen korunur; güncelleme sonrası
// oyuncunun kaydı, hakları ve rekoru kaybolmaz.
// ============================================================
class GameRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("MahjongSave", Context.MODE_PRIVATE)
    private val settingsPrefs: SharedPreferences =
        context.getSharedPreferences("MahjongSettings", Context.MODE_PRIVATE)

    fun saveGame(tiles: List<Tile>, score: Long, level: Int, combo: Long, progress: LevelProgress) {
        prefs.edit().apply {
            putString("tiles_data_v7", tiles.joinToString(";") {
                "${it.id}:${it.type}:${it.isVisible}:${it.gridX}:${it.gridY}:${it.layer}"
            })
            putLong("score_long", score)
            putInt("level", level)
            putLong("combo", combo)
            putBoolean("has_save", true)
            putLong(KEY_ELAPSED, progress.elapsedMs)
            putInt(KEY_LEVEL_HINTS, progress.hintsUsed)
            putInt(KEY_LEVEL_SHUFFLES, progress.shufflesUsed)
            putInt(KEY_LEVEL_BEST_COMBO, progress.bestCombo)
            apply()
        }
    }

    fun getSavedString(): String? = prefs.getString("tiles_data_v7", null)
    fun getSavedScore(): Long = prefs.getLong("score_long", 0L)
    fun getSavedLevel(): Int = prefs.getInt("level", 1)
    fun getSavedCombo(): Long = prefs.getLong("combo", 0L)
    fun getSavedProgress() = LevelProgress(
        elapsedMs = prefs.getLong(KEY_ELAPSED, 0L),
        hintsUsed = prefs.getInt(KEY_LEVEL_HINTS, 0),
        shufflesUsed = prefs.getInt(KEY_LEVEL_SHUFFLES, 0),
        bestCombo = prefs.getInt(KEY_LEVEL_BEST_COMBO, 0)
    )

    fun saveElapsed(ms: Long) {
        if (prefs.contains("tiles_data_v7")) prefs.edit().putLong(KEY_ELAPSED, ms).apply()
    }

    fun clearSave() = prefs.edit().clear().apply()

    // Hint ve Shuffle sayaçları (kullanım hakları)
    fun getHintCount(): Int = settingsPrefs.getInt("hint_count", GameTuning.HINTS_PER_REFILL)
    fun saveHintCount(c: Int) = settingsPrefs.edit().putInt("hint_count", c).apply()

    fun getShuffleCount(): Int = settingsPrefs.getInt("shuffle_count", GameTuning.SHUFFLES_PER_REFILL)
    fun saveShuffleCount(c: Int) = settingsPrefs.edit().putInt("shuffle_count", c).apply()

    fun getHighScore(): Long = settingsPrefs.getLong("highscore", 0L)
    fun saveHighScore(score: Long) {
        if (score > getHighScore()) settingsPrefs.edit().putLong("highscore", score).apply()
    }

    fun loadSettings() = GameSettings(
        musicEnabled = settingsPrefs.getBoolean(KEY_MUSIC, true),
        sfxEnabled = settingsPrefs.getBoolean(KEY_SFX, true),
        hapticsEnabled = settingsPrefs.getBoolean(KEY_HAPTICS, true),
        dimBlockedTiles = settingsPrefs.getBoolean(KEY_DIM_BLOCKED, true),
        highlightPairs = settingsPrefs.getBoolean(KEY_HIGHLIGHT_PAIRS, false),
        boardTheme = BoardTheme.fromName(settingsPrefs.getString(KEY_BOARD_THEME, null)),
        tileStyle = TileStyle.fromName(settingsPrefs.getString(KEY_TILE_STYLE, null))
    )

    fun saveSettings(s: GameSettings) = settingsPrefs.edit().apply {
        putBoolean(KEY_MUSIC, s.musicEnabled)
        putBoolean(KEY_SFX, s.sfxEnabled)
        putBoolean(KEY_HAPTICS, s.hapticsEnabled)
        putBoolean(KEY_DIM_BLOCKED, s.dimBlockedTiles)
        putBoolean(KEY_HIGHLIGHT_PAIRS, s.highlightPairs)
        putString(KEY_BOARD_THEME, s.boardTheme.name)
        putString(KEY_TILE_STYLE, s.tileStyle.name)
        apply()
    }

    fun isTutorialSeen(): Boolean = settingsPrefs.getBoolean(KEY_TUTORIAL_SEEN, false)
    fun setTutorialSeen() = settingsPrefs.edit().putBoolean(KEY_TUTORIAL_SEEN, true).apply()

    fun loadStats() = GameStats(
        levelsWon = settingsPrefs.getInt("stat_levels_won", 0),
        totalMatches = settingsPrefs.getInt("stat_total_matches", 0),
        bestCombo = settingsPrefs.getInt("stat_best_combo", 0),
        fastestLevelMs = settingsPrefs.getLong("stat_fastest_ms", 0L),
        totalPlayMs = settingsPrefs.getLong("stat_total_play_ms", 0L),
        totalStars = settingsPrefs.getInt("stat_total_stars", 0),
        threeStarLevels = settingsPrefs.getInt("stat_three_stars", 0),
        hintsUsed = settingsPrefs.getInt("stat_hints_used", 0),
        shufflesUsed = settingsPrefs.getInt("stat_shuffles_used", 0),
        highestLevel = settingsPrefs.getInt("stat_highest_level", 1)
    )

    fun saveStats(s: GameStats) = settingsPrefs.edit().apply {
        putInt("stat_levels_won", s.levelsWon)
        putInt("stat_total_matches", s.totalMatches)
        putInt("stat_best_combo", s.bestCombo)
        putLong("stat_fastest_ms", s.fastestLevelMs)
        putLong("stat_total_play_ms", s.totalPlayMs)
        putInt("stat_total_stars", s.totalStars)
        putInt("stat_three_stars", s.threeStarLevels)
        putInt("stat_hints_used", s.hintsUsed)
        putInt("stat_shuffles_used", s.shufflesUsed)
        putInt("stat_highest_level", s.highestLevel)
        apply()
    }

    companion object {
        const val KEY_MUSIC = "music_enabled"
        const val KEY_SFX = "sfx_enabled"
        private const val KEY_HAPTICS = "haptics_enabled"
        private const val KEY_DIM_BLOCKED = "dim_blocked_tiles"
        private const val KEY_HIGHLIGHT_PAIRS = "highlight_pairs"
        private const val KEY_BOARD_THEME = "board_theme"
        private const val KEY_TILE_STYLE = "tile_style"
        private const val KEY_TUTORIAL_SEEN = "tutorial_seen"
        private const val KEY_ELAPSED = "elapsed_ms"
        private const val KEY_LEVEL_HINTS = "level_hints_used"
        private const val KEY_LEVEL_SHUFFLES = "level_shuffles_used"
        private const val KEY_LEVEL_BEST_COMBO = "level_best_combo"
    }
}

// ============================================================
// VIEW MODEL — oyun kuralları, deadlock kontrolü, Undo, rekor,
// süre sayacı, yıldız hesabı ve istatistikler
// ============================================================
class GameViewModel(private val repository: GameRepository) : ViewModel() {
    private val TAG = "GameViewModel"

    private val _tiles = MutableStateFlow<List<Tile>>(emptyList())
    val tiles = _tiles.asStateFlow()
    private val _score = MutableStateFlow(0L)
    val score = _score.asStateFlow()
    private val _currentLevel = MutableStateFlow(1)
    val currentLevel = _currentLevel.asStateFlow()
    private val _isGameWon = MutableStateFlow(false)
    val isGameWon = _isGameWon.asStateFlow()
    private val _isDeadlocked = MutableStateFlow(false)
    val isDeadlocked = _isDeadlocked.asStateFlow()
    private val _remainingPairs = MutableStateFlow(0)
    val remainingPairs = _remainingPairs.asStateFlow()
    private val _totalPairs = MutableStateFlow(0)
    val totalPairs = _totalPairs.asStateFlow()
    private val _highScore = MutableStateFlow(0L)
    val highScore = _highScore.asStateFlow()
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    // O an açıkta (seçilebilir) VE eşi de açıkta olan taş TİPLERİ.
    private val _matchableTypes = MutableStateFlow<Set<String>>(emptySet())
    val matchableTypes = _matchableTypes.asStateFlow()

    // En az bir eşleşme animasyonu oynuyor mu? Bu sırada Karıştır/Geri Al kapalıdır.
    private val animatingPairIds = mutableSetOf<Int>()
    private val _isMatchAnimating = MutableStateFlow(false)
    val isMatchAnimating = _isMatchAnimating.asStateFlow()

    private val _hintCount = MutableStateFlow(GameTuning.HINTS_PER_REFILL)
    val hintCount = _hintCount.asStateFlow()
    private val _shuffleCount = MutableStateFlow(GameTuning.SHUFFLES_PER_REFILL)
    val shuffleCount = _shuffleCount.asStateFlow()

    // Karıştırma sonrası hala kilitli ise kullanıcıya yeni oyun teklif et
    private val _showRescueDialog = MutableStateFlow(false)
    val showRescueDialog = _showRescueDialog.asStateFlow()
    fun dismissRescueDialog() {
        _showRescueDialog.value = false
    }

    private val _comboStreak = MutableStateFlow(0)
    val comboStreak = _comboStreak.asStateFlow()
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()
    private val _winSummary = MutableStateFlow<WinSummary?>(null)
    val winSummary = _winSummary.asStateFlow()
    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings = _settings.asStateFlow()
    private val _stats = MutableStateFlow(repository.loadStats())
    val stats = _stats.asStateFlow()

    /** Yeni dizilim dağıtıldığında artar (dağıtma animasyonu için). */
    private val _boardEpoch = MutableStateFlow(0)
    val boardEpoch = _boardEpoch.asStateFlow()

    /** Karıştırma yapıldığında artar (çevirme animasyonu için). */
    private val _shuffleEpoch = MutableStateFlow(0)
    val shuffleEpoch = _shuffleEpoch.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    // Ardışık eşleşme SAYACI (streak) — 0'dan başlar.
    private var comboMultiplier = 0L
        set(value) {
            field = value
            _comboStreak.value = value.toInt()
        }
    private var selectedTileId: Int? = null
    var soundManager: SoundManager? = null
        set(value) {
            field = value
            value?.applySettings(_settings.value)
        }
    private val undoStack = mutableListOf<MoveRecord>()

    private var levelHintsUsed = 0
    private var levelShufflesUsed = 0
    private var levelBestCombo = 0

    private var accumulatedMs = 0L
    private var runningSince = 0L
    private var timerRunning = false
    private var timerRequested = false
    private var tickerJob: Job? = null

    init {
        _highScore.value = repository.getHighScore()
        _hintCount.value = repository.getHintCount()
        _shuffleCount.value = repository.getShuffleCount()
        loadOrNewGame()
    }

    // Hint sonuç durumları
    enum class HintResult {
        SHOWN,           // İpucu gösterildi, hak eksildi
        NO_HINTS,        // Hak yok
        NO_MATCH         // Açıkta eşleşen çift yok — hak HARCANMADI
    }

    // Hint kullanım hakkı: önce eşleşme var mı kontrol et, sonra hak eksilt.
    fun useHintIfAvailable(): HintResult {
        val selectables = _tiles.value.filter { it.isVisible && it.isSelectable }
        val hasMatch = selectables.groupBy { it.type }.any { it.value.size >= 2 }
        if (!hasMatch) return HintResult.NO_MATCH

        if (_hintCount.value <= 0) return HintResult.NO_HINTS

        _hintCount.value -= 1
        repository.saveHintCount(_hintCount.value)
        showHint()
        return HintResult.SHOWN
    }

    fun refillHints() {
        _hintCount.value = GameTuning.HINTS_PER_REFILL
        repository.saveHintCount(GameTuning.HINTS_PER_REFILL)
        showHint()
    }

    // Shuffle hakkı: > 0 ise tüket, karıştır. 0 ise dialog → reklam → refill.
    fun useShuffleIfAvailable(): Boolean {
        if (_shuffleCount.value <= 0) return false
        _shuffleCount.value -= 1
        repository.saveShuffleCount(_shuffleCount.value)
        shuffleCurrentTiles()
        return true
    }

    fun refillShuffles() {
        _shuffleCount.value = GameTuning.SHUFFLES_PER_REFILL
        repository.saveShuffleCount(GameTuning.SHUFFLES_PER_REFILL)
        shuffleCurrentTiles()
    }

    // Reklam yüklenemezse acil 1 hak ver (kullanıcı kilitlenmesin)
    fun emergencyShuffle() {
        _shuffleCount.value = 1
        repository.saveShuffleCount(1)
        shuffleCurrentTiles()
    }

    fun startNewGame(levelIndex: Int = 1) {
        stopTimer()
        repository.clearSave()
        _isGameWon.value = false
        _winSummary.value = null
        _isDeadlocked.value = false
        _showRescueDialog.value = false
        _currentLevel.value = levelIndex
        comboMultiplier = 0L
        _score.value = 0L
        selectedTileId = null
        undoStack.clear()
        _canUndo.value = false
        animatingPairIds.clear()
        _isMatchAnimating.value = false
        accumulatedMs = 0L
        _elapsedSeconds.value = 0
        levelHintsUsed = 0
        levelShufflesUsed = 0
        levelBestCombo = 0

        // Level'a göre zorluk grubundan rastgele dizilim
        var positions = LevelPatterns.randomLayoutForLevel(levelIndex)
        if (positions.size % 2 != 0) positions = positions.dropLast(1)

        val pairsNeeded = positions.size / 2
        val names = TileAssets.allTileNames

        // KİLİTSİZ BAŞLANGIÇ: Açıkta en az bir eşleşen çift olana kadar deck'i yeniden karıştır.
        var attempts = 0
        var finalTiles: List<Tile>
        do {
            val deck = mutableListOf<String>()
            for (i in 0 until pairsNeeded) {
                val n = names[i % names.size]; deck.add(n); deck.add(n)
            }
            deck.shuffle()
            finalTiles = positions.mapIndexed { index, pos ->
                Tile(
                    index,
                    if (index < deck.size) deck[index] else names[0],
                    pos.first,
                    pos.second,
                    pos.third
                )
            }
            attempts++
        } while (!hasMatchableSelectableTiles(finalTiles) && attempts < 50)

        _tiles.value = finalTiles
        _totalPairs.value = finalTiles.size / 2
        updateSelectables()
        updateRemainingPairs()
        saveGameState()
        if (levelIndex > _stats.value.highestLevel) {
            updateStats { it.copy(highestLevel = levelIndex) }
        }
        _boardEpoch.value += 1
        refreshTimer()
    }

    /** Aynı level numarasında yeni bir dizilimle yeniden başlar. */
    fun restartLevel() = startNewGame(_currentLevel.value)

    /**
     * Verilen tile listesinde — şu an seçilebilir olan taşlar arasında
     * eşleşen en az bir çift var mı? updateSelectables() ile BİREBİR AYNI
     * kuralı (isTileSelectable) kullanır.
     */
    private fun hasMatchableSelectableTiles(tileList: List<Tile>): Boolean {
        val activeTiles = tileList.filter { it.isVisible }
        val selectables = activeTiles.filter { isTileSelectable(it, activeTiles) }
        val typeCounts = selectables.groupingBy { it.type }.eachCount()
        return typeCounts.values.any { it >= 2 }
    }

    /**
     * Bir taşın seçilebilir olup olmadığını hesaplayan TEK ve ORTAK kural.
     * HERHANGİ BİR üst katmanda (layer > tile.layer) o taşla örtüşen görünür
     * bir taş varsa taş seçilemez; aynı katmanda solu VE sağı doluysa seçilemez.
     */
    private fun isTileSelectable(tile: Tile, activeTiles: List<Tile>): Boolean {
        val isCovered = activeTiles.any { o ->
            o.layer > tile.layer &&
                    kotlin.math.abs(o.gridX - tile.gridX) < 2 &&
                    kotlin.math.abs(o.gridY - tile.gridY) < 2
        }
        if (isCovered) return false
        val bL = activeTiles.any {
            it.layer == tile.layer && it.gridX == tile.gridX - 2 && kotlin.math.abs(it.gridY - tile.gridY) < 2
        }
        val bR = activeTiles.any {
            it.layer == tile.layer && it.gridX == tile.gridX + 2 && kotlin.math.abs(it.gridY - tile.gridY) < 2
        }
        return !bL || !bR
    }

    /** Sadece kazanılmış bir level'dan sonra ilerler — çift dokunuşta iki level atlanmaz. */
    fun moveToNextLevel() {
        if (!_isGameWon.value) return
        startNewGame(_currentLevel.value + 1)
    }

    private fun loadOrNewGame() {
        val savedData = repository.getSavedString()
        if (savedData != null) {
            try {
                val loaded = savedData.split(";").map {
                    val p = it.split(":")
                    Tile(
                        p[0].toInt(),
                        p[1],
                        p[3].toInt(),
                        p[4].toInt(),
                        p[5].toInt(),
                        isVisible = p[2].toBoolean()
                    )
                }
                val savedLevel = repository.getSavedLevel()
                // Level bitirilip "Sonraki Level"e basılmadan uygulama kapandıysa
                // boş bir tahtada takılı kalınmaz; bir sonraki level başlar.
                if (loaded.none { it.isVisible }) {
                    startNewGame(savedLevel + 1)
                    return
                }
                _tiles.value = loaded
                _totalPairs.value = loaded.size / 2
                _score.value = repository.getSavedScore()
                _currentLevel.value = savedLevel
                comboMultiplier = repository.getSavedCombo()
                val progress = repository.getSavedProgress()
                accumulatedMs = progress.elapsedMs
                _elapsedSeconds.value = (accumulatedMs / 1000L).toInt()
                levelHintsUsed = progress.hintsUsed
                levelShufflesUsed = progress.shufflesUsed
                levelBestCombo = progress.bestCombo
                updateSelectables()
                updateRemainingPairs()
                checkForDeadlock()
                _boardEpoch.value += 1
            } catch (e: Exception) {
                Log.e(TAG, "Save load failed", e)
                startNewGame(1)
            }
        } else startNewGame(1)
    }

    private fun saveGameState() = repository.saveGame(
        _tiles.value, _score.value, _currentLevel.value, comboMultiplier,
        LevelProgress(currentElapsedMs(), levelHintsUsed, levelShufflesUsed, levelBestCombo)
    )

    fun shuffleCurrentTiles() {
        comboMultiplier = 0L
        _isDeadlocked.value = false
        animatingPairIds.clear()
        _isMatchAnimating.value = false
        val baseList = _tiles.value.toMutableList()
        val visibleTiles = baseList.filter { it.isVisible }
        val originalTypes = visibleTiles.map { it.type }

        // En az 1 eşleşen çift garantilenene kadar tekrar karıştır (max 50 deneme).
        var attempts = 0
        var bestList = baseList
        do {
            val candidateList = baseList.toMutableList()
            val typesShuffled = originalTypes.shuffled()
            visibleTiles.forEachIndexed { index, tile ->
                val oi = candidateList.indexOfFirst { it.id == tile.id }
                if (oi != -1) candidateList[oi] = candidateList[oi].copy(
                    type = typesShuffled[index],
                    isSelected = false,
                    isHinted = false,
                    isMatched = false,
                    matchPairId = null
                )
            }
            bestList = candidateList
            attempts++
        } while (!hasMatchableSelectableTiles(bestList) && attempts < 50)

        // 50 denemeden sonra hala kilitli kaldıysa yeni oyun teklif et.
        if (!hasMatchableSelectableTiles(bestList)) {
            _showRescueDialog.value = true
        }

        selectedTileId = null
        _tiles.value = bestList
        updateSelectables()
        checkForDeadlock()
        levelShufflesUsed++
        updateStats { it.copy(shufflesUsed = it.shufflesUsed + 1) }
        saveGameState()
        _shuffleEpoch.value += 1
        soundManager?.playShuffle()
    }

    fun showHint() {
        val currentList = _tiles.value.toMutableList()
        currentList.forEachIndexed { i, t ->
            if (t.isHinted) currentList[i] = t.copy(isHinted = false)
        }
        val selectables = currentList.filter { it.isVisible && it.isSelectable }
        val match =
            selectables.groupBy { it.type }.filter { it.value.size >= 2 }.values.firstOrNull()
        if (match != null) {
            match.take(2).forEach { mt ->
                val i = currentList.indexOfFirst { it.id == mt.id }
                if (i != -1) currentList[i] = currentList[i].copy(isHinted = true)
            }
            levelHintsUsed++
            updateStats { it.copy(hintsUsed = it.hintsUsed + 1) }
            soundManager?.playHint()
        }
        _tiles.value = currentList
    }

    fun undoLastMove() {
        if (undoStack.isEmpty()) return
        if (_isMatchAnimating.value) return
        val last = undoStack.removeAt(undoStack.size - 1)
        _canUndo.value = undoStack.isNotEmpty()
        val currentList = _tiles.value.toMutableList()
        val i1 = currentList.indexOfFirst { it.id == last.tile1.id }
        if (i1 != -1) currentList[i1] = last.tile1.copy(isSelected = false, isHinted = false)
        val i2 = currentList.indexOfFirst { it.id == last.tile2.id }
        if (i2 != -1) currentList[i2] = last.tile2.copy(isSelected = false, isHinted = false)
        // Önceki seçim/ipucu işaretleri temizlenir (geri gelen taşlarla karışmasın).
        currentList.forEachIndexed { i, t ->
            if (t.isSelected || t.isHinted) currentList[i] = t.copy(isSelected = false, isHinted = false)
        }
        _tiles.value = currentList
        _score.value = last.scoreBefore
        comboMultiplier = last.comboBefore
        selectedTileId = null
        _isDeadlocked.value = false
        if (animatingPairIds.remove(last.tile1.id)) {
            _isMatchAnimating.value = animatingPairIds.isNotEmpty()
        }
        updateSelectables()
        updateRemainingPairs()
        checkForDeadlock()
        saveGameState()
        soundManager?.playUndo()
    }

    /**
     * Taşların seçilebilirliğini VE ekranda gerçekten "eşleşebilir" olan taş
     * tiplerini tek seferde günceller. isMatched=true olan taşlar hiçbir zaman
     * seçilebilir sayılmaz.
     */
    private fun updateSelectables() {
        val currentList = _tiles.value
        val activeTiles = currentList.filter { it.isVisible }
        val updated = currentList.map { tile ->
            when {
                !tile.isVisible -> tile
                tile.isMatched -> tile.copy(isSelectable = false)
                else -> tile.copy(isSelectable = isTileSelectable(tile, activeTiles))
            }
        }
        _tiles.value = updated
        val selectables = updated.filter { it.isVisible && it.isSelectable }
        _matchableTypes.value = selectables.groupingBy { it.type }.eachCount()
            .filterValues { it >= 2 }.keys
    }

    private fun updateRemainingPairs() {
        _remainingPairs.value = _tiles.value.count { it.isVisible } / 2
    }

    private fun checkForDeadlock() {
        val selectables = _tiles.value.filter { it.isVisible && it.isSelectable }
        _isDeadlocked.value =
            selectables.isNotEmpty() && selectables.groupBy { it.type }.none { it.value.size >= 2 }
    }

    /** Taban puan + ardışık eşleşme (streak) bonusu (sınırlı) + katman bonusu. */
    private fun calculateMatchScore(tile1: Tile, tile2: Tile, streakBeforeThisMatch: Long): Long {
        val comboBonus =
            streakBeforeThisMatch.coerceAtMost(GameTuning.MAX_COMBO_STREAK) * GameTuning.COMBO_BONUS_STEP
        val layerBonus = (tile1.layer + tile2.layer).toLong() * GameTuning.LAYER_BONUS_PER_LEVEL
        return GameTuning.BASE_MATCH_SCORE + comboBonus + layerBonus
    }

    fun onTileClick(clickedTile: Tile) {
        // Her zaman GÜNCEL durum kullanılır: hızlı çift dokunuşta eski (stale)
        // bir kopya yüzünden eşleşmiş/gizlenmiş bir taş tekrar seçilemez.
        val current = _tiles.value.firstOrNull { it.id == clickedTile.id } ?: return
        if (!current.isVisible || current.isMatched || !current.isSelectable) return
        if (_isGameWon.value) return
        soundManager?.playClick()
        val currentTiles = _tiles.value.toMutableList()
        val index = currentTiles.indexOfFirst { it.id == current.id }
        if (index == -1) return

        if (currentTiles[index].isHinted) {
            currentTiles.forEachIndexed { i, t ->
                if (t.isHinted) currentTiles[i] = t.copy(isHinted = false)
            }
        }

        if (selectedTileId == null) {
            currentTiles.forEachIndexed { i, t -> if (t.isSelected) currentTiles[i] = t.copy(isSelected = false) }
            currentTiles[index] = currentTiles[index].copy(isSelected = true)
            selectedTileId = current.id
            _tiles.value = currentTiles
            _events.tryEmit(GameEvent.Selected)
        } else if (current.id == selectedTileId) {
            currentTiles[index] = currentTiles[index].copy(isSelected = false)
            selectedTileId = null
            _tiles.value = currentTiles
        } else {
            val prevTile = currentTiles.find { it.id == selectedTileId }
            if (prevTile != null && prevTile.isVisible && !prevTile.isMatched && prevTile.type == current.type) {
                val prevIndex = currentTiles.indexOfFirst { it.id == prevTile.id }

                val matchScore = calculateMatchScore(prevTile, current, comboMultiplier)

                undoStack.add(
                    MoveRecord(
                        currentTiles[prevIndex],
                        currentTiles[index],
                        _score.value,
                        comboMultiplier
                    )
                )
                _canUndo.value = true

                // Taşlar hemen kaybolmaz: önce isMatched işaretlenip havalanma
                // animasyonu oynatılır, gizleme finalizeMatch'te yapılır.
                val pairId = currentTiles[prevIndex].id
                currentTiles[prevIndex] = currentTiles[prevIndex].copy(
                    isSelected = false,
                    isHinted = false,
                    isSelectable = false,
                    isMatched = true,
                    matchPairId = pairId
                )
                currentTiles[index] = currentTiles[index].copy(
                    isSelected = false,
                    isHinted = false,
                    isSelectable = false,
                    isMatched = true,
                    matchPairId = pairId
                )
                selectedTileId = null

                _score.value += matchScore
                comboMultiplier = (comboMultiplier + 1).coerceAtMost(GameTuning.MAX_COMBO_STREAK)
                val streak = comboMultiplier.toInt()
                soundManager?.playMatch(streak)
                if (streak > levelBestCombo) levelBestCombo = streak
                if (streak > _stats.value.bestCombo) updateStats { it.copy(bestCombo = streak) }

                _tiles.value = currentTiles
                updateSelectables()

                animatingPairIds.add(pairId)
                _isMatchAnimating.value = true
                _events.tryEmit(GameEvent.Matched(prevTile.id, current.id, matchScore, streak))

                val matchedId1 = prevTile.id
                val matchedId2 = current.id
                viewModelScope.launch {
                    delay(GameTuning.MATCH_ANIM_DURATION_MS.toLong())
                    finalizeMatch(matchedId1, matchedId2)
                }
            } else {
                comboMultiplier = 0L
                val pi = currentTiles.indexOfFirst { it.id == selectedTileId }
                if (pi != -1) currentTiles[pi] = currentTiles[pi].copy(isSelected = false)
                currentTiles[index] = currentTiles[index].copy(isSelected = true)
                selectedTileId = current.id
                _tiles.value = currentTiles
                _events.tryEmit(GameEvent.Selected)
            }
        }
    }

    /** Kapalı bir taşa dokunuldu — arayüz taşı sallar, kısa bir ses çalınır. */
    fun onBlockedTileTap(tile: Tile) {
        if (!tile.isVisible || tile.isMatched || _isGameWon.value) return
        soundManager?.playBlocked()
        _events.tryEmit(GameEvent.Blocked(tile.id))
    }

    /**
     * Eşleşme animasyonu bittikten sonra taşlar gerçekten gizlenir; oyun
     * bittiyse kazanma ekranı tetiklenir. Eşleşme bu arada Undo ile geri
     * alındıysa hiçbir şey yapılmaz.
     */
    private fun finalizeMatch(tileId1: Int, tileId2: Int) {
        animatingPairIds.remove(tileId1)
        _isMatchAnimating.value = animatingPairIds.isNotEmpty()

        val list = _tiles.value.toMutableList()
        val i1 = list.indexOfFirst { it.id == tileId1 }
        val i2 = list.indexOfFirst { it.id == tileId2 }
        val stillMatched = (i1 != -1 && list[i1].isMatched) || (i2 != -1 && list[i2].isMatched)
        if (!stillMatched) return

        if (i1 != -1) list[i1] = list[i1].copy(isVisible = false, isMatched = false, matchPairId = null)
        if (i2 != -1) list[i2] = list[i2].copy(isVisible = false, isMatched = false, matchPairId = null)
        _tiles.value = list
        updateSelectables()
        updateRemainingPairs()
        updateStats { it.copy(totalMatches = it.totalMatches + 1) }
        saveGameState()

        if (list.none { it.isVisible }) {
            soundManager?.playWin()
            comboMultiplier = 0L
            onLevelCompleted()
        } else {
            checkForDeadlock()
        }
    }

    private fun onLevelCompleted() {
        stopTimer()
        val level = _currentLevel.value
        val elapsedSec = (accumulatedMs / 1000L).toInt()
        val par = _totalPairs.value * GameTuning.PAR_SECONDS_PER_PAIR
        val timeBonus = (par - elapsedSec).coerceAtLeast(0).toLong() * GameTuning.TIME_BONUS_PER_SECOND
        val matchScore = _score.value
        val previousBest = repository.getHighScore()
        _score.value = matchScore + timeBonus
        repository.saveHighScore(_score.value)
        _highScore.value = repository.getHighScore()

        val stars = computeStars(elapsedSec, par, levelHintsUsed + levelShufflesUsed)
        updateStats {
            it.copy(
                levelsWon = it.levelsWon + 1,
                totalStars = it.totalStars + stars,
                threeStarLevels = it.threeStarLevels + if (stars == 3) 1 else 0,
                fastestLevelMs = if (it.fastestLevelMs == 0L || accumulatedMs < it.fastestLevelMs) {
                    accumulatedMs
                } else it.fastestLevelMs
            )
        }
        _winSummary.value = WinSummary(
            level = level,
            matchScore = matchScore,
            timeBonus = timeBonus,
            totalScore = _score.value,
            elapsedSeconds = elapsedSec,
            parSeconds = par,
            stars = stars,
            bestCombo = levelBestCombo,
            isNewRecord = _score.value > previousBest
        )
        saveGameState()
        _isGameWon.value = true
        _events.tryEmit(GameEvent.Won)
    }

    private fun computeStars(elapsedSec: Int, parSec: Int, assists: Int): Int {
        var stars = when {
            elapsedSec <= parSec -> 3
            elapsedSec <= parSec * 2 -> 2
            else -> 1
        }
        if (assists >= GameTuning.ASSISTS_FOR_STAR_PENALTY) stars -= 1
        return stars.coerceIn(1, 3)
    }

    // ------------------------------------------------------------
    // SÜRE SAYACI — oyun ekranı açık ve uygulama ön plandayken işler
    // ------------------------------------------------------------
    fun setTimerRequested(requested: Boolean) {
        timerRequested = requested
        refreshTimer()
    }

    private fun refreshTimer() {
        val shouldRun = timerRequested && !_isGameWon.value && _tiles.value.any { it.isVisible }
        if (shouldRun && !timerRunning) startTimer()
        else if (!shouldRun && timerRunning) stopTimer()
    }

    private fun startTimer() {
        timerRunning = true
        runningSince = SystemClock.elapsedRealtime()
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                _elapsedSeconds.value = (currentElapsedMs() / 1000L).toInt()
                delay(250)
            }
        }
    }

    private fun stopTimer() {
        if (!timerRunning) return
        val delta = (SystemClock.elapsedRealtime() - runningSince).coerceAtLeast(0L)
        accumulatedMs += delta
        timerRunning = false
        tickerJob?.cancel()
        tickerJob = null
        _elapsedSeconds.value = (accumulatedMs / 1000L).toInt()
        updateStats { it.copy(totalPlayMs = it.totalPlayMs + delta) }
        repository.saveElapsed(accumulatedMs)
    }

    private fun currentElapsedMs(): Long = accumulatedMs +
            if (timerRunning) (SystemClock.elapsedRealtime() - runningSince).coerceAtLeast(0L) else 0L

    // ------------------------------------------------------------
    // AYARLAR / İSTATİSTİK
    // ------------------------------------------------------------
    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val updated = transform(_settings.value)
        if (updated == _settings.value) return
        _settings.value = updated
        repository.saveSettings(updated)
        soundManager?.applySettings(updated)
    }

    fun isTutorialSeen(): Boolean = repository.isTutorialSeen()
    fun markTutorialSeen() = repository.setTutorialSeen()

    private fun updateStats(transform: (GameStats) -> GameStats) {
        val updated = transform(_stats.value)
        _stats.value = updated
        repository.saveStats(updated)
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }
}

// ============================================================
// FACTORY
// ============================================================
class GameViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return GameViewModel(GameRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
