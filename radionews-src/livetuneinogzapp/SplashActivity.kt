package com.globalradio.livetuneinogzapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.globalradio.livetuneinogzapp.ads.AdManager
import com.globalradio.livetuneinogzapp.databinding.ActivitySplashBinding
import com.globalradio.livetuneinogzapp.utils.LocaleCountryMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)

        // 1. Tasarımı yüklüyoruz
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1b. Alt başlığı cihaz diline göre ayarlıyoruz
        // (Türkçe: "Türk ve Dünya Radyo İstasyonları", diğer diller: o dildeki
        // "Dünya Radyo İstasyonları", çevirisi yoksa İngilizce)
        binding.tvSplashSubtitle.text = LocaleCountryMapper.getHomeSubtitle()

        // 2. Şık bir giriş: logo + yazılar + noktalar sırayla belirir,
        // ardından logo "nefes alır" gibi sürekli hafifçe büyüyüp küçülür
        // ve noktalar dalga hâlinde yanıp söner.
        runEntranceSequence()

        // 3. Reklamları başlatıyoruz
        AdManager.initialize(this)

        // 4. Arka planda 3 saniye bekleyip MainActivity'e yönlendiriyoruz
        lifecycleScope.launch {
            delay(3000)

            // Ana ekrana geçiş yap
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))

            // Splash ekranını kapat
            finish()
        }
    }

    /**
     * Logo, başlık, alt başlık ve yükleme noktalarını sırayla (kademeli)
     * belirginleştirir. Logo belirdikten sonra sürekli "nefes alma" animasyonu,
     * noktalar belirdikten sonra ise dalga animasyonu başlar.
     */
    private fun runEntranceSequence() {
        val logo = binding.logoWrapper
        logo.alpha = 1f
        logo.scaleX = 1f
        logo.scaleY = 1f
        startBreathingAnimation(logo)

        binding.tvAppName.translationY = 24f
        binding.tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(260)
            .setDuration(420)
            .start()

        binding.tvSplashSubtitle.translationY = 18f
        binding.tvSplashSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(420)
            .start()

        binding.loadingDots.animate()
            .alpha(1f)
            .setStartDelay(560)
            .setDuration(380)
            .withEndAction { startDotAnimation() }
            .start()

        startAmbientGlowAnimation()
    }

    /** Logonun sürekli, yumuşak bir şekilde büyüyüp küçülmesi: "nefes alma" efekti. */
    private fun startBreathingAnimation(target: View) {
        val scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.07f, 1f).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.07f, 1f).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    /** Arka plandaki büyük parlamanın da logoyla uyumlu, hafif nefes alması. */
    private fun startAmbientGlowAnimation() {
        val glow = binding.ambientGlow
        ObjectAnimator.ofFloat(glow, View.ALPHA, 0.5f, 0.9f, 0.5f).apply {
            duration = 3200
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun startDotAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)

        dots.forEachIndexed { i, dot ->
            // Daha havalı, nabız gibi büyüyüp küçülen dot_wave animasyonunu çağırıyoruz
            val anim = AnimationUtils.loadAnimation(this, R.anim.dot_wave)
            anim.startOffset = (i * 150).toLong()
            dot.startAnimation(anim)
        }
    }
}
