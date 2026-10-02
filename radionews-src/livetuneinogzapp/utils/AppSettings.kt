package com.globalradio.livetuneinogzapp.utils

import android.content.Context
import android.media.AudioManager
import androidx.appcompat.app.AppCompatDelegate
import com.globalradio.livetuneinogzapp.model.RadioStation
import com.google.gson.Gson

/** Uygulama genel ayarları (dil hariç). Varsayılanlar mevcut davranışı korur. */
class AppSettings(context: Context) {

    private val app = context.applicationContext
    private val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    var darkTheme: Boolean
        get() = p.getBoolean(KEY_DARK, true)
        set(value) = p.edit().putBoolean(KEY_DARK, value).apply()

    var resumeLastStation: Boolean
        get() = p.getBoolean(KEY_RESUME, false)
        set(value) = p.edit().putBoolean(KEY_RESUME, value).apply()

    var lockScreenControls: Boolean
        get() = p.getBoolean(KEY_LOCK, true)
        set(value) = p.edit().putBoolean(KEY_LOCK, value).apply()

    var applyDefaultVolume: Boolean
        get() = p.getBoolean(KEY_APPLY_VOL, false)
        set(value) = p.edit().putBoolean(KEY_APPLY_VOL, value).apply()

    var defaultVolumePercent: Int
        get() = p.getInt(KEY_VOL, 70).coerceIn(0, 100)
        set(value) = p.edit().putInt(KEY_VOL, value.coerceIn(0, 100)).apply()

    var dataSaver: Boolean
        get() = p.getBoolean(KEY_DATA, false)
        set(value) = p.edit().putBoolean(KEY_DATA, value).apply()

    var timerFadeOut: Boolean
        get() = p.getBoolean(KEY_FADE, true)
        set(value) = p.edit().putBoolean(KEY_FADE, value).apply()

    var timerMuteInsteadOfStop: Boolean
        get() = p.getBoolean(KEY_MUTE, false)
        set(value) = p.edit().putBoolean(KEY_MUTE, value).apply()

    var lastTimerMinutes: Int
        get() = p.getInt(KEY_LAST_TIMER, 15).coerceIn(1, 180)
        set(value) = p.edit().putInt(KEY_LAST_TIMER, value.coerceIn(1, 180)).apply()

    fun saveLastStation(station: RadioStation) {
        p.edit().putString(KEY_LAST_JSON, gson.toJson(station)).apply()
    }

    fun lastStation(): RadioStation? {
        val json = p.getString(KEY_LAST_JSON, null) ?: return null
        return try {
            gson.fromJson(json, RadioStation::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun applyNightMode() {
        AppCompatDelegate.setDefaultNightMode(
            if (darkTheme) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun applyDefaultVolumeIfEnabled() {
        if (!applyDefaultVolume) return
        try {
            val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val target = (max * defaultVolumePercent) / 100
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val PREFS = "app_settings"
        private const val KEY_DARK = "dark_theme"
        private const val KEY_RESUME = "resume_last"
        private const val KEY_LOCK = "lock_controls"
        private const val KEY_APPLY_VOL = "apply_vol"
        private const val KEY_VOL = "vol_pct"
        private const val KEY_DATA = "data_saver"
        private const val KEY_FADE = "timer_fade"
        private const val KEY_MUTE = "timer_mute"
        private const val KEY_LAST_TIMER = "last_timer_min"
        private const val KEY_LAST_JSON = "last_station_json"

        fun applyNightMode(context: Context) = AppSettings(context).applyNightMode()
    }
}
