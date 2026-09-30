package com.gamelogic.satrancpro

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class MainActivity : AppCompatActivity() {

    private var bannerView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var adsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = resources.getIdentifier("activity_main", "layout", packageName)
        if (layoutId != 0) {
            setContentView(layoutId)
        } else {
            setContentView(FrameLayout(this))
        }
        MobileAds.initialize(this) {
            adsReady = true
            loadBanner()
            loadInterstitial()
            loadRewarded()
        }
    }

    private fun loadBanner() {
        bannerView?.destroy()
        val adView = AdView(this)
        val widthDp = (resources.displayMetrics.widthPixels / resources.displayMetrics.density)
            .toInt()
            .coerceAtLeast(320)
        adView.adUnitId = BANNER_ID
        adView.setAdSize(AdSize.getInlineAdaptiveBannerAdSize(widthDp, BANNER_HEIGHT_DP))
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.i(TAG, "Banner loaded")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.w(TAG, "Banner: ${error.code} ${error.message}")
            }
        }
        adView.loadAd(AdRequest.Builder().build())
        bannerView = adView

        val heightPx = (BANNER_HEIGHT_DP * resources.displayMetrics.density).toInt()
        val container = findBannerContainer()
        if (container != null) {
            container.removeAllViews()
            container.addView(
                adView,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
            )
        } else {
            findViewById<ViewGroup>(android.R.id.content).addView(
                adView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx,
                    Gravity.BOTTOM
                )
            )
        }
    }

    private fun findBannerContainer(): ViewGroup? {
        val names = arrayOf(
            "banner_container",
            "ad_container",
            "bannerContainer",
            "adContainer",
            "banner",
            "adView"
        )
        for (name in names) {
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val view = findViewById<View>(id)
                if (view is ViewGroup) return view
            }
        }
        return null
    }

    fun loadInterstitial() {
        if (!adsReady || interstitialAd != null) return
        InterstitialAd.load(
            this,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial: ${error.code} ${error.message}")
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial(onClosed: () -> Unit = {}) {
        val ad = interstitialAd
        if (!adsReady || ad == null) {
            loadInterstitial()
            onClosed()
            return
        }
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadInterstitial()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Interstitial show: ${adError.message}")
                loadInterstitial()
                onClosed()
            }
        }
        ad.show(this)
    }

    fun loadRewarded() {
        if (!adsReady || rewardedAd != null) return
        RewardedAd.load(
            this,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded: ${error.code} ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun showRewarded(onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewardedAd
        if (!adsReady || ad == null) {
            Toast.makeText(this, "Reklam hazır değil, tekrar dene", Toast.LENGTH_SHORT).show()
            loadRewarded()
            onUnavailable()
            return
        }
        rewardedAd = null
        var rewarded = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded()
                if (!rewarded) onUnavailable()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Rewarded show: ${adError.message}")
                loadRewarded()
                onUnavailable()
            }
        }
        ad.show(this) {
            rewarded = true
            onReward()
        }
    }

    override fun onDestroy() {
        bannerView?.destroy()
        bannerView = null
        interstitialAd = null
        rewardedAd = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "AdMob"
        const val BANNER_HEIGHT_DP = 100
        const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
