package com.example.mahjongmaster

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// ============================================================
// SES YÖNETİCİSİ — efektler (SoundPool) + arka plan müziği (MediaPlayer)
//
// res/raw klasöründe click / match / win / music (mp3, ogg, wav) dosyaları
// varsa ONLAR kullanılır. Yoksa oyun kendi seslerini ilk açılışta sentezler
// (cache klasörüne küçük WAV dosyaları yazılır), böylece ek dosya gerekmez.
// ============================================================
class SoundManager(context: Context) {
    private val TAG = "SoundManager"
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(audioAttributes)
        .build()

    private enum class Sfx(val rawName: String?) {
        CLICK("click"), MATCH("match"), WIN("win"), HINT(null),
        SHUFFLE(null), UNDO(null), BLOCKED(null), BUTTON(null)
    }

    private val soundIds = ConcurrentHashMap<Sfx, Int>()
    private val loadedIds = ConcurrentHashMap.newKeySet<Int>()

    private var mediaPlayer: MediaPlayer? = null
    private var inForeground = false
    @Volatile
    private var released = false

    @Volatile
    var sfxEnabled: Boolean = true

    var isMusicEnabled: Boolean
        private set

    init {
        val prefs = appContext.getSharedPreferences("MahjongSettings", Context.MODE_PRIVATE)
        isMusicEnabled = prefs.getBoolean(GameRepository.KEY_MUSIC, true)
        sfxEnabled = prefs.getBoolean(GameRepository.KEY_SFX, true)
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedIds.add(sampleId)
        }
        worker.execute { loadEffects() }
        worker.execute { prepareMusic() }
    }

    private fun rawId(name: String): Int =
        appContext.resources.getIdentifier(name, "raw", appContext.packageName)

    private fun loadEffects() {
        val dir = File(appContext.cacheDir, "sfx").apply { mkdirs() }
        for (sfx in Sfx.entries) {
            if (released) return
            try {
                val raw = sfx.rawName?.let { rawId(it) } ?: 0
                val id = if (raw != 0) {
                    soundPool.load(appContext, raw, 1)
                } else {
                    val file = File(dir, "${SynthAudio.VERSION}_${sfx.name.lowercase()}.wav")
                    if (!file.exists() || file.length() <= 44L) {
                        SynthAudio.writeWav(file, SynthAudio.effect(sfx.name), SynthAudio.SFX_RATE)
                    }
                    soundPool.load(file.absolutePath, 1)
                }
                soundIds[sfx] = id
            } catch (e: Throwable) {
                Log.e(TAG, "Sound ${sfx.name} failed", e)
            }
        }
    }

    private fun prepareMusic() {
        try {
            val raw = rawId("music")
            val player = if (raw != 0) {
                MediaPlayer.create(appContext, raw)
            } else {
                val file = File(appContext.cacheDir, "${SynthAudio.VERSION}_ambient.wav")
                if (!file.exists() || file.length() <= 44L) {
                    SynthAudio.writeWav(file, SynthAudio.ambientMusic(), SynthAudio.MUSIC_RATE)
                }
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    prepare()
                }
            } ?: return
            player.isLooping = true
            player.setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
            mainHandler.post {
                if (released) {
                    player.release()
                } else {
                    mediaPlayer = player
                    updateMusicPlayback()
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Music init failed", e)
        }
    }

    private fun play(sfx: Sfx, volume: Float, rate: Float = 1f) {
        if (!sfxEnabled || released) return
        val id = soundIds[sfx] ?: return
        if (id == 0 || id !in loadedIds) return
        soundPool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun playClick() = play(Sfx.CLICK, 0.7f)

    /** Ardışık eşleşmelerde ses perdesi hafifçe yükselir (combo hissi). */
    fun playMatch(streak: Int = 1) = play(Sfx.MATCH, 1f, 1f + 0.035f * (streak - 1).coerceIn(0, 12))

    fun playWin() = play(Sfx.WIN, 1f)
    fun playHint() = play(Sfx.HINT, 0.8f)
    fun playShuffle() = play(Sfx.SHUFFLE, 0.8f)
    fun playUndo() = play(Sfx.UNDO, 0.7f)
    fun playBlocked() = play(Sfx.BLOCKED, 0.6f)
    fun playButton() = play(Sfx.BUTTON, 0.5f)

    fun applySettings(settings: GameSettings) {
        sfxEnabled = settings.sfxEnabled
        setMusicEnabled(settings.musicEnabled)
    }

    fun setMusicEnabled(enabled: Boolean) {
        isMusicEnabled = enabled
        updateMusicPlayback()
    }

    fun toggleMusic() = setMusicEnabled(!isMusicEnabled)

    /** Activity.onResume */
    fun startMusic() {
        inForeground = true
        updateMusicPlayback()
    }

    /** Activity.onPause */
    fun stopMusic() {
        inForeground = false
        updateMusicPlayback()
    }

    private fun updateMusicPlayback() {
        val player = mediaPlayer ?: return
        try {
            if (isMusicEnabled && inForeground) {
                if (!player.isPlaying) player.start()
            } else if (player.isPlaying) {
                player.pause()
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Music state error", e)
        }
    }

    fun release() {
        released = true
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            soundPool.release()
            worker.shutdownNow()
        } catch (e: Exception) {
            Log.e(TAG, "Release error", e)
        }
    }

    private companion object {
        const val MUSIC_VOLUME = 0.45f
    }
}

/**
 * Küçük bir ses sentezleyici: taş "tık" sesi, marimba benzeri eşleşme
 * çanları ve pentatonik (Doğu Asya) gamında, koto/guzheng tınılı
 * Karplus–Strong telli çalgı sentezi ile kesintisiz dönen ambiyans müziği.
 */
internal object SynthAudio {
    const val VERSION = "v1"
    const val SFX_RATE = 44100
    const val MUSIC_RATE = 24000

    fun effect(name: String): FloatArray = when (name) {
        "CLICK" -> click()
        "MATCH" -> match()
        "WIN" -> win()
        "HINT" -> hint()
        "SHUFFLE" -> shuffle()
        "UNDO" -> undo()
        "BLOCKED" -> blocked()
        else -> button()
    }.also { finish(it, 0.85f) }

    private fun buffer(seconds: Double) = FloatArray((seconds * SFX_RATE).toInt())

    private fun addTone(
        out: FloatArray, startSec: Double, freq: Double, amp: Double,
        decaySec: Double, attackSec: Double = 0.002, rate: Int = SFX_RATE
    ) {
        val start = (startSec * rate).toInt()
        if (start >= out.size) return
        val len = min(out.size - start, (decaySec * 7 * rate).toInt())
        val w = 2 * PI * freq / rate
        for (i in 0 until len) {
            val t = i.toDouble() / rate
            val env = (1 - exp(-t / attackSec)) * exp(-t / decaySec)
            out[start + i] += (amp * env * sin(w * i)).toFloat()
        }
    }

    private fun addMarimba(out: FloatArray, startSec: Double, freq: Double, amp: Double, decay: Double) {
        addTone(out, startSec, freq, amp, decay)
        addTone(out, startSec, freq * 3.93, amp * 0.22, decay * 0.28)
        addTone(out, startSec, freq * 9.1, amp * 0.06, decay * 0.1)
    }

    /** İki sert taşın çarpışması: sönümlü rezonanslar + kısa gürültü darbesi. */
    private fun addClack(out: FloatArray, startSec: Double, amp: Double, pitch: Double, rnd: Random) {
        addTone(out, startSec, 2350.0 * pitch, amp * 0.55, 0.018, 0.0004)
        addTone(out, startSec, 3650.0 * pitch, amp * 0.35, 0.012, 0.0004)
        addTone(out, startSec, 5400.0 * pitch, amp * 0.18, 0.008, 0.0003)
        addTone(out, startSec, 900.0 * pitch, amp * 0.25, 0.02, 0.0005)
        val start = (startSec * SFX_RATE).toInt()
        val n = min(out.size - start, (0.006 * SFX_RATE).toInt())
        var prev = 0.0
        for (i in 0 until n) {
            val white = rnd.nextDouble(-1.0, 1.0)
            val highPassed = white - prev
            prev = white
            out[start + i] += (amp * 0.3 * highPassed * exp(-i / (0.0012 * SFX_RATE))).toFloat()
        }
    }

    private fun click() = buffer(0.12).also { addClack(it, 0.0, 0.9, 1.0, Random(1)) }

    private fun match() = buffer(0.9).also {
        addClack(it, 0.0, 0.55, 1.08, Random(2))
        addMarimba(it, 0.02, 880.0, 0.45, 0.22)
        addMarimba(it, 0.09, 1318.51, 0.4, 0.26)
    }

    private fun win() = buffer(2.6).also {
        val arpeggio = doubleArrayOf(587.33, 739.99, 880.0, 1174.66)
        arpeggio.forEachIndexed { i, f -> addMarimba(it, i * 0.11, f, 0.38, 0.35) }
        for (f in doubleArrayOf(587.33, 739.99, 880.0, 1174.66, 1479.98)) addMarimba(it, 0.5, f, 0.2, 0.7)
        for (k in 0 until 10) {
            addTone(it, 0.55 + k * 0.07, if (k % 2 == 0) 2349.32 else 2959.96, 0.05, 0.12)
        }
    }

    private fun hint() = buffer(1.0).also {
        addTone(it, 0.0, 1567.98, 0.28, 0.35, 0.004)
        addTone(it, 0.0, 3135.96, 0.07, 0.2, 0.004)
        addTone(it, 0.08, 2093.0, 0.2, 0.3, 0.004)
    }

    private fun shuffle() = buffer(0.55).also {
        val r = Random(3)
        var t = 0.0
        repeat(11) { _ ->
            addClack(it, t, 0.45 + r.nextDouble() * 0.25, 0.85 + r.nextDouble() * 0.35, r)
            t += 0.025 + r.nextDouble() * 0.03
        }
    }

    private fun undo() = buffer(0.5).also {
        addMarimba(it, 0.0, 659.25, 0.35, 0.12)
        addMarimba(it, 0.08, 493.88, 0.35, 0.16)
    }

    private fun blocked() = buffer(0.2).also {
        addTone(it, 0.0, 196.0, 0.5, 0.045, 0.001)
        addTone(it, 0.0, 392.0, 0.18, 0.02, 0.001)
        addClack(it, 0.0, 0.25, 0.6, Random(4))
    }

    private fun button() = buffer(0.08).also {
        addTone(it, 0.0, 1244.51, 0.3, 0.012, 0.0005)
        addTone(it, 0.0, 2489.02, 0.1, 0.008, 0.0005)
    }

    /** Normalize + sonda kısa fade (tıkırtısız bitiş). */
    private fun finish(data: FloatArray, peak: Float) {
        var max = 0f
        for (v in data) max = maxOf(max, abs(v))
        if (max > 0f) {
            val gain = peak / max
            for (i in data.indices) data[i] *= gain
        }
        val fade = min(data.size, (0.005 * SFX_RATE).toInt())
        for (i in 0 until fade) data[data.size - 1 - i] *= i.toFloat() / fade
    }

    // ------------------------------------------------------------
    // AMBİYANS MÜZİK — D majör pentatonik, 72 BPM, 40 sn kesintisiz döngü
    // ------------------------------------------------------------
    fun ambientMusic(): FloatArray {
        val sr = MUSIC_RATE
        val beat = 60.0 / 72.0
        val beats = 48
        val loopLen = (beats * beat * sr).toInt()
        val tail = 5 * sr
        val dry = FloatArray(loopLen + tail)
        val rnd = Random(20240917)

        val penta = intArrayOf(0, 2, 4, 7, 9)
        fun freq(semisFromD4: Int) = 293.66 * 2.0.pow(semisFromD4 / 12.0)
        fun scaleSemis(idx: Int): Int = Math.floorDiv(idx, 5) * 12 + penta[Math.floorMod(idx, 5)]

        // Akorlar (her biri 12 vuruş): D — Bm — G — A
        val roots = intArrayOf(-12, -15, -19, -17)
        val chordTones = arrayOf(setOf(0, 4, 7), setOf(9, 0, 4), setOf(9, 0), setOf(7, 2))

        var idx = 5
        for (b in 0 until beats) {
            val chord = b / 12
            val t = b * beat
            if (b % 4 == 0) {
                pluck(dry, t, freq(roots[chord]), 0.30, 5.0, 0.9975, rnd)
            }
            if (b % 4 == 2) {
                pluck(dry, t, freq(roots[chord] + 7), 0.16, 4.0, 0.997, rnd)
            }
            val isPhraseEnd = b % 12 == 11
            if (isPhraseEnd || (b % 4 != 0 && rnd.nextDouble() < 0.18)) continue

            val steps = intArrayOf(-2, -1, -1, 0, 1, 1, 2)
            idx = (idx + steps[rnd.nextInt(steps.size)]).coerceIn(2, 10)
            if (b % 4 == 0) {
                var best = idx
                for (d in 0..3) {
                    val up = (idx + d).coerceAtMost(10)
                    val down = (idx - d).coerceAtLeast(2)
                    if (Math.floorMod(scaleSemis(up), 12) in chordTones[chord]) { best = up; break }
                    if (Math.floorMod(scaleSemis(down), 12) in chordTones[chord]) { best = down; break }
                }
                idx = best
            }
            val amp = if (b % 4 == 0) 0.36 else 0.28
            pluck(dry, t, freq(scaleSemis(idx)), amp, 3.5, 0.996, rnd)
            if (rnd.nextDouble() < 0.3) {
                val passing = (idx + if (rnd.nextBoolean()) 1 else -1).coerceIn(2, 10)
                pluck(dry, t + beat / 2, freq(scaleSemis(passing)), 0.2, 3.0, 0.996, rnd)
            }
        }

        // Yumuşak pad — her akorda kök + beşli, yavaş giriş/çıkış
        for (c in 0 until 4) {
            val start = c * 12 * beat
            val length = 12 * beat + 2.0
            for (semis in intArrayOf(roots[c], roots[c] + 7)) {
                addPad(dry, start, length, freq(semis), 0.045, sr)
            }
        }

        for (i in 0 until tail) dry[i] += dry[loopLen + i]
        val loop = dry.copyOf(loopLen)
        val out = reverbCircular(loop, sr)

        var max = 0f
        for (v in out) max = maxOf(max, abs(v))
        if (max > 0f) {
            val gain = 0.85f / max
            for (i in out.indices) out[i] *= gain
        }
        return out
    }

    /** Karplus–Strong telli çalgı (koto/guzheng benzeri), allpass ile hassas akort. */
    private fun pluck(
        out: FloatArray, startSec: Double, freq: Double, amp: Double,
        durationSec: Double, decay: Double, rnd: Random
    ) {
        val sr = MUSIC_RATE
        val period = sr / freq
        val n = floor(period - 0.5).toInt().coerceAtLeast(2)
        val frac = (period - 0.5 - n).coerceIn(0.0, 0.999)
        val c = (1 - frac) / (1 + frac)
        val buf = DoubleArray(n) { rnd.nextDouble(-1.0, 1.0) }
        repeat(2) { for (i in 1 until n) buf[i] = 0.5 * (buf[i] + buf[i - 1]) }
        var mean = 0.0
        for (v in buf) mean += v
        mean /= n
        for (i in 0 until n) buf[i] -= mean

        val start = (startSec * sr).toInt()
        if (start >= out.size) return
        val len = min(out.size - start, (durationSec * sr).toInt())
        var idx = 0
        var lastX = 0.0
        var apX = 0.0
        var apY = 0.0
        val attack = (0.004 * sr).toInt().coerceAtLeast(1)
        for (i in 0 until len) {
            val x = buf[idx]
            val lp = decay * 0.5 * (x + lastX)
            lastX = x
            val ap = c * lp + apX - c * apY
            apX = lp
            apY = ap
            buf[idx] = ap
            idx = if (idx + 1 == n) 0 else idx + 1
            val env = if (i < attack) i.toDouble() / attack else 1.0
            val fadeOut = if (i > len - 2000) (len - i) / 2000.0 else 1.0
            out[start + i] += (amp * x * env * fadeOut).toFloat()
        }
    }

    private fun addPad(out: FloatArray, startSec: Double, lengthSec: Double, freq: Double, amp: Double, sr: Int) {
        val start = (startSec * sr).toInt()
        val len = min(out.size - start, (lengthSec * sr).toInt())
        val fade = 2.0 * sr
        val w1 = 2 * PI * freq / sr
        val w2 = 2 * PI * freq * 1.003 / sr
        for (i in 0 until len) {
            val env = min(1.0, min(i / fade, (len - i) / fade))
            val v = sin(w1 * i) + 0.6 * sin(w2 * i) + 0.15 * sin(2 * w1 * i)
            out[start + i] += (amp * env * v).toFloat()
        }
    }

    /** Freeverb tarzı basit yankı; döngü iki kez işlenir ki yankı kuyruğu başa sarılsın. */
    private fun reverbCircular(input: FloatArray, sr: Int): FloatArray {
        val scale = sr / 44100.0
        val combs = intArrayOf(1116, 1188, 1277, 1356).map { Comb((it * scale).toInt()) }
        val allPasses = intArrayOf(556, 441).map { AllPass((it * scale).toInt()) }
        val out = FloatArray(input.size)
        for (pass in 0..1) {
            for (i in input.indices) {
                val x = input[i] * 0.2f
                var wet = 0f
                for (comb in combs) wet += comb.process(x)
                for (ap in allPasses) wet = ap.process(wet)
                if (pass == 1) out[i] = input[i] + wet * 0.55f
            }
        }
        return out
    }

    private class Comb(size: Int) {
        private val buf = FloatArray(size.coerceAtLeast(1))
        private var idx = 0
        private var store = 0f
        fun process(input: Float): Float {
            val output = buf[idx]
            store = output * 0.75f + store * 0.25f
            buf[idx] = input + store * 0.8f
            idx = if (idx + 1 == buf.size) 0 else idx + 1
            return output
        }
    }

    private class AllPass(size: Int) {
        private val buf = FloatArray(size.coerceAtLeast(1))
        private var idx = 0
        fun process(input: Float): Float {
            val bufOut = buf[idx]
            buf[idx] = input + bufOut * 0.5f
            idx = if (idx + 1 == buf.size) 0 else idx + 1
            return bufOut - input
        }
    }

    fun writeWav(file: File, data: FloatArray, sampleRate: Int) {
        val byteCount = data.size * 2
        val bytes = ByteBuffer.allocate(44 + byteCount).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray(Charsets.US_ASCII))
        bytes.putInt(36 + byteCount)
        bytes.put("WAVE".toByteArray(Charsets.US_ASCII))
        bytes.put("fmt ".toByteArray(Charsets.US_ASCII))
        bytes.putInt(16)
        bytes.putShort(1)
        bytes.putShort(1)
        bytes.putInt(sampleRate)
        bytes.putInt(sampleRate * 2)
        bytes.putShort(2)
        bytes.putShort(16)
        bytes.put("data".toByteArray(Charsets.US_ASCII))
        bytes.putInt(byteCount)
        for (v in data) bytes.putShort((v.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(bytes.array())
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
