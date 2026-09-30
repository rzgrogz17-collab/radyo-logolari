package com.gamelogic.satrancpro

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds

/**
 * Yalnızca Google AdMob. Açılışta SDK başlar, ardından MainActivity açılır.
 */
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val openMain = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = resources.getIdentifier("activity_splash", "layout", packageName)
        if (layoutId != 0) {
            setContentView(layoutId)
        } else {
            setContentView(FrameLayout(this))
        }
        MobileAds.initialize(this) {}
        handler.postDelayed(openMain, 1500)
    }

    override fun onDestroy() {
        handler.removeCallbacks(openMain)
        super.onDestroy()
    }
}
