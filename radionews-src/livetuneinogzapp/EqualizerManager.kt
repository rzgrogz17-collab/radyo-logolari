package com.globalradio.livetuneinogzapp

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object EqualizerManager {

    private const val TAG = "EqualizerManager"
    private const val PREFS_NAME = "equalizer_prefs"
    private const val KEY_ENABLED = "eq_enabled"
    private const val KEY_PRESET = "eq_preset"
    private const val KEY_BASS_STRENGTH = "eq_bass_strength"
    private const val KEY_VIRT_STRENGTH = "eq_virt_strength"
    private const val KEY_BAND_PREFIX = "eq_band_"
    private const val KEY_USER_PRESETS = "eq_user_presets"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentAudioSessionId: Int = 0

    var isAvailable: Boolean = false
        private set

    var isEnabled: Boolean = false
        private set

    var currentPresetIndex: Int = -1
        private set

    var bassStrength: Int = 0
        private set

    var virtualizerStrength: Int = 0
        private set

    data class Preset(val name: String, val gains: IntArray)
    data class UserPreset(
        val name: String,
        val gains: IntArray,
        val bass: Int = 0,
        val virt: Int = 0
    )

    val presets = listOf(
        Preset("Flat", intArrayOf(0, 0, 0, 0, 0)),
        Preset("Rock", intArrayOf(400, 200, -100, 200, 400)),
        Preset("Pop", intArrayOf(-100, 200, 400, 200, -100)),
        Preset("Hip-Hop", intArrayOf(400, 300, 0, 100, 300)),
        Preset("Jazz", intArrayOf(300, 100, -100, 100, 300)),
        Preset("Classical", intArrayOf(300, 200, -100, 200, 400)),
        Preset("Bass", intArrayOf(500, 400, 100, 0, 0)),
        Preset("Vocal", intArrayOf(-200, 0, 300, 300, 100))
    )

    var userPresets: MutableList<UserPreset> = mutableListOf()
        private set

    private val gson = Gson()

    fun init(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (audioSessionId == currentAudioSessionId && equalizer != null) return

        release()
        currentAudioSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = false }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = false }
            isAvailable = true
        } catch (e: Exception) {
            Log.e(TAG, "EQ init error: ${e.message}")
            isAvailable = false
            return
        }
        try {
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = false }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer unavailable: ${e.message}")
            virtualizer = null
        }
        loadSettings(context)
    }

    fun getNumberOfBands(): Int = equalizer?.numberOfBands?.toInt() ?: 5

    fun getBandLevelRange(): Pair<Int, Int> {
        val eq = equalizer ?: return Pair(-1500, 1500)
        val range = eq.bandLevelRange
        return Pair(range[0].toInt(), range[1].toInt())
    }

    fun getBandLevel(band: Int): Int =
        equalizer?.getBandLevel(band.toShort())?.toInt() ?: 0

    fun getBandFreq(band: Int): Int =
        equalizer?.getCenterFreq(band.toShort())?.div(1000) ?: 0

    fun trebleLevel(): Int {
        val last = (getNumberOfBands() - 1).coerceAtLeast(0)
        return getBandLevel(last)
    }

    fun setBandLevel(context: Context, band: Int, level: Int) {
        try {
            equalizer?.setBandLevel(band.toShort(), level.toShort())
            currentPresetIndex = -1
            saveSettings(context)
        } catch (e: Exception) {
            Log.e(TAG, "setBandLevel error: ${e.message}")
        }
    }

    fun setTreble(context: Context, level: Int) {
        val last = (getNumberOfBands() - 1).coerceAtLeast(0)
        setBandLevel(context, last, level)
    }

    fun applyPreset(context: Context, presetIndex: Int) {
        if (presetIndex < 0 || presetIndex >= presets.size) return
        val preset = presets[presetIndex]
        applyGains(preset.gains)
        currentPresetIndex = presetIndex
        saveSettings(context)
    }

    fun applyUserPreset(context: Context, index: Int) {
        val preset = userPresets.getOrNull(index) ?: return
        applyGains(preset.gains)
        setBassBoostStrength(context, preset.bass)
        setVirtualizerStrength(context, preset.virt)
        currentPresetIndex = USER_PRESET_BASE + index
        saveSettings(context)
    }

    fun resetFlat(context: Context) {
        applyPreset(context, 0)
        setBassBoostStrength(context, 0)
        setVirtualizerStrength(context, 0)
    }

    fun saveUserPreset(context: Context, name: String) {
        val gains = IntArray(getNumberOfBands()) { getBandLevel(it) }
        userPresets.add(UserPreset(name.trim().ifBlank { "Custom" }, gains, bassStrength, virtualizerStrength))
        currentPresetIndex = USER_PRESET_BASE + userPresets.lastIndex
        saveSettings(context)
    }

    fun deleteUserPreset(context: Context, index: Int) {
        if (index !in userPresets.indices) return
        userPresets.removeAt(index)
        if (currentPresetIndex >= USER_PRESET_BASE) currentPresetIndex = -1
        saveSettings(context)
    }

    fun isUserPresetSelected(): Boolean = currentPresetIndex >= USER_PRESET_BASE

    fun selectedUserPresetIndex(): Int =
        if (isUserPresetSelected()) currentPresetIndex - USER_PRESET_BASE else -1

    private fun applyGains(gains: IntArray) {
        val eq = equalizer ?: return
        val (min, max) = getBandLevelRange()
        val bands = eq.numberOfBands.toInt().coerceAtMost(gains.size)
        for (i in 0 until bands) {
            eq.setBandLevel(i.toShort(), gains[i].coerceIn(min, max).toShort())
        }
    }

    fun setBassBoostStrength(context: Context, strength: Int) {
        try {
            val s = strength.coerceIn(0, 1000)
            bassBoost?.setStrength(s.toShort())
            bassStrength = s
            saveSettings(context)
        } catch (e: Exception) {
            Log.e(TAG, "BassBoost error: ${e.message}")
        }
    }

    fun setVirtualizerStrength(context: Context, strength: Int) {
        try {
            val s = strength.coerceIn(0, 1000)
            virtualizer?.setStrength(s.toShort())
            virtualizerStrength = s
            saveSettings(context)
        } catch (e: Exception) {
            Log.e(TAG, "Virtualizer error: ${e.message}")
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        isEnabled = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled && virtualizer != null
        saveSettings(context)
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentAudioSessionId = 0
    }

    private fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, isEnabled)
            putInt(KEY_PRESET, currentPresetIndex)
            putInt(KEY_BASS_STRENGTH, bassStrength)
            putInt(KEY_VIRT_STRENGTH, virtualizerStrength)
            putString(KEY_USER_PRESETS, gson.toJson(userPresets))
            val eq = equalizer
            if (eq != null) {
                for (i in 0 until eq.numberOfBands) {
                    putInt("${KEY_BAND_PREFIX}$i", eq.getBandLevel(i.toShort()).toInt())
                }
            }
            apply()
        }
    }

    private fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean(KEY_ENABLED, false)
        currentPresetIndex = prefs.getInt(KEY_PRESET, 0)
        bassStrength = prefs.getInt(KEY_BASS_STRENGTH, 0)
        virtualizerStrength = prefs.getInt(KEY_VIRT_STRENGTH, 0)
        userPresets = try {
            val json = prefs.getString(KEY_USER_PRESETS, "[]") ?: "[]"
            val type = object : TypeToken<MutableList<UserPreset>>() {}.type
            gson.fromJson<MutableList<UserPreset>>(json, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }

        val eq = equalizer ?: return
        if (currentPresetIndex >= 0 && currentPresetIndex < presets.size) {
            applyPreset(context, currentPresetIndex)
        } else if (currentPresetIndex >= USER_PRESET_BASE) {
            val ui = currentPresetIndex - USER_PRESET_BASE
            if (ui in userPresets.indices) applyUserPreset(context, ui)
            else loadBandsFromPrefs(prefs, eq)
        } else {
            loadBandsFromPrefs(prefs, eq)
        }
        bassBoost?.setStrength(bassStrength.toShort())
        virtualizer?.setStrength(virtualizerStrength.toShort())
        eq.enabled = isEnabled
        bassBoost?.enabled = isEnabled
        virtualizer?.enabled = isEnabled
    }

    private fun loadBandsFromPrefs(prefs: android.content.SharedPreferences, eq: Equalizer) {
        for (i in 0 until eq.numberOfBands) {
            val level = prefs.getInt("${KEY_BAND_PREFIX}$i", 0)
            eq.setBandLevel(i.toShort(), level.toShort())
        }
        currentPresetIndex = -1
    }

    const val USER_PRESET_BASE = 1000
}
