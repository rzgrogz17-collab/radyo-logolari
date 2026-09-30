package com.gamelogic.satrancpro

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.satranc.R

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION_MS: Long = 2800L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val launchMain = Runnable {
        if (!isFinishing && !isDestroyed) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        startAnimations()
        handler.postDelayed(launchMain, SPLASH_DURATION_MS)
    }

    private fun startAnimations() {
        val iconCircle = findViewById<LinearLayout>(R.id.icon_circle)
        val bottomGroup = findViewById<View>(R.id.bottom_group)

        iconCircle?.let {
            it.alpha = 0f
            it.translationY = 60f
            val fadeIn = ObjectAnimator.ofFloat(it, View.ALPHA, 0f, 1f).apply {
                duration = 700
                startDelay = 150
            }
            val slideUp = ObjectAnimator.ofFloat(it, View.TRANSLATION_Y, 60f, 0f).apply {
                duration = 700
                startDelay = 150
                interpolator = OvershootInterpolator(1.2f)
            }
            AnimatorSet().apply {
                playTogether(fadeIn, slideUp)
                start()
            }
        }

        iconCircle?.let {
            val scaleX = ObjectAnimator.ofFloat(it, View.SCALE_X, 1f, 1.06f, 1f).apply {
                duration = 1800
                startDelay = 900
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            val scaleY = ObjectAnimator.ofFloat(it, View.SCALE_Y, 1f, 1.06f, 1f).apply {
                duration = 1800
                startDelay = 900
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                start()
            }
        }

        bottomGroup?.let {
            it.alpha = 0f
            it.translationY = 40f
            val fadeIn = ObjectAnimator.ofFloat(it, View.ALPHA, 0f, 1f).apply {
                duration = 700
                startDelay = 500
            }
            val slideUp = ObjectAnimator.ofFloat(it, View.TRANSLATION_Y, 40f, 0f).apply {
                duration = 700
                startDelay = 500
                interpolator = AccelerateDecelerateInterpolator()
            }
            AnimatorSet().apply {
                playTogether(fadeIn, slideUp)
                start()
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(launchMain)
        super.onDestroy()
    }

    @Deprecated("Splash ekranında geri tuşunu engelliyoruz")
    override fun onBackPressed() {
    }
}
