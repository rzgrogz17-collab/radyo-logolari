package tv.garden.global.webapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import java.util.Locale

@SuppressLint("CustomSplashScreen")
class SplashActivity : Activity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var dotsRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val glow = findViewById<ImageView>(R.id.splashGlow)
        val logo = findViewById<ImageView>(R.id.splashLogo)
        val title = findViewById<TextView>(R.id.splashTitle)
        val tagline = findViewById<TextView>(R.id.splashTagline)
        val progress = findViewById<View>(R.id.splashProgress)
        val loadingLabel = findViewById<TextView>(R.id.splashLoadingText)
        val density = resources.displayMetrics.density

        // LanguageManager Compose tarafında (MainActivity.kt) yaşadığından,
        // bu saf View ekranında cihaz diline göre sade bir metin seçimi
        // kendi içinde yapılır.
        val lang = Locale.getDefault().language
        tagline.text = when (lang) {
            "tr" -> "Canlı TV Deneyimi"
            "de" -> "Live-TV Erlebnis"
            "fr" -> "Expérience TV en direct"
            "es" -> "Experiencia de TV en vivo"
            "pt" -> "Experiência de TV ao vivo"
            "ru" -> "Прямой эфир"
            "ar" -> "تجربة التلفزيون المباشر"
            "zh" -> "直播电视体验"
            else -> "Live TV Experience"
        }
        val loadingBase = when (lang) {
            "tr" -> "Yükleniyor"
            "de" -> "Wird geladen"
            "fr" -> "Chargement"
            "es" -> "Cargando"
            "pt" -> "Carregando"
            "ru" -> "Загрузка"
            "ar" -> "جار التحميل"
            "zh" -> "加载中"
            else -> "Loading"
        }
        loadingLabel.text = loadingBase

        // ── Kademeli (staggered) giriş animasyonları ──
        fadeIn(glow, duration = 700, startOffset = 0)
        entranceLogo(logo)
        fadeSlideIn(title, duration = 550, startOffset = 260, distancePx = 24 * density)
        fadeIn(tagline, duration = 500, startOffset = 480)
        fadeIn(progress, duration = 400, startOffset = 650)
        fadeIn(loadingLabel, duration = 400, startOffset = 650)

        // Giriş animasyonu bittikten hemen sonra logo ve parıltı sürekli
        // nabız atmaya (pulse) başlar — "havalı", canlı bir bekleme hissi.
        mainHandler.postDelayed({ startPulse(logo) }, 700)
        mainHandler.postDelayed({ startGlowPulse(glow) }, 700)

        // "Yükleniyor..." metnindeki noktaları döngüsel olarak günceller.
        var dotCount = 0
        val dotsUpdater = object : Runnable {
            override fun run() {
                dotCount = (dotCount + 1) % 4
                loadingLabel.text = loadingBase + ".".repeat(dotCount)
                mainHandler.postDelayed(this, 420)
            }
        }
        dotsRunnable = dotsUpdater
        mainHandler.postDelayed(dotsUpdater, 1100)

        // Bir süre sonra MainActivity'e geç.
        mainHandler.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 2800)
    }

    // fillBefore = true: startOffset ile gecikmeli başlayan animasyonlarda,
    // henüz animasyon başlamadan ÖNCE görünüm başlangıç durumunda (ör.
    // şeffaf) sabit kalır; bu sayede "önce tam görünüp sonra kaybolma"
    // türünden bir yanıp sönme (flaş) oluşmaz.
    private fun fadeIn(view: View, duration: Long, startOffset: Long) {
        val anim = AlphaAnimation(0f, 1f).apply {
            this.duration = duration
            this.startOffset = startOffset
            fillBefore = true
            fillAfter = true
            interpolator = DecelerateInterpolator()
        }
        view.startAnimation(anim)
    }

    private fun fadeSlideIn(view: View, duration: Long, startOffset: Long, distancePx: Float) {
        val slide = TranslateAnimation(0f, 0f, distancePx, 0f).apply {
            this.duration = duration
            fillBefore = true
            fillAfter = true
        }
        val fade = AlphaAnimation(0f, 1f).apply {
            this.duration = duration
            fillBefore = true
            fillAfter = true
        }
        val set = AnimationSet(true).apply {
            addAnimation(slide)
            addAnimation(fade)
            this.startOffset = startOffset
            interpolator = DecelerateInterpolator()
        }
        view.startAnimation(set)
    }

    private fun entranceLogo(view: View) {
        val scale = ScaleAnimation(
            0.55f, 1f, 0.55f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 650
            fillBefore = true
            fillAfter = true
        }
        val fade = AlphaAnimation(0f, 1f).apply {
            duration = 650
            fillBefore = true
            fillAfter = true
        }
        val set = AnimationSet(true).apply {
            addAnimation(scale)
            addAnimation(fade)
            interpolator = DecelerateInterpolator()
        }
        view.startAnimation(set)
    }

    private fun startPulse(view: View) {
        val pulse = ScaleAnimation(
            0.94f, 1.05f, 0.94f, 1.05f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 900
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        view.startAnimation(pulse)
    }

    private fun startGlowPulse(view: View) {
        val pulse = ScaleAnimation(
            0.85f, 1.12f, 0.85f, 1.12f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1400
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        view.startAnimation(pulse)
    }

    override fun onDestroy() {
        super.onDestroy()
        dotsRunnable?.let { mainHandler.removeCallbacks(it) }
        mainHandler.removeCallbacksAndMessages(null)
    }
}
