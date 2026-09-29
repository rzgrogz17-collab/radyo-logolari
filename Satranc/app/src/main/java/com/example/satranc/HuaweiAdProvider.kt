package com.example.satranc

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import com.huawei.hms.ads.AdListener
import com.huawei.hms.ads.AdParam
import com.huawei.hms.ads.BannerAdSize
import com.huawei.hms.ads.HwAds
import com.huawei.hms.ads.InterstitialAd
import com.huawei.hms.ads.banner.BannerView
import com.huawei.hms.ads.reward.Reward
import com.huawei.hms.ads.reward.RewardAd
import com.huawei.hms.ads.reward.RewardAdLoadListener
import com.huawei.hms.ads.reward.RewardAdStatusListener

/**
 * HUAWEI PETAL ADS (HMS Ads Kit — ads-lite) — AdConfig.YANDEX_COUNTRIES listesinde
 * OLMAYAN tüm ülkelerde kullanılır. Huawei olmayan (Samsung, Xiaomi vb.) cihazlarda da çalışır.
 * Reklam birim ID'leri: strings.xml -> huawei_banner_id / huawei_interstitial_id / huawei_rewarded_id
 */
internal class HuaweiAdProvider(private val appContext: Context) : AdProvider {
    override val network = AdNetwork.HUAWEI

    private var interstitialAd: InterstitialAd? = null
    private var pendingInterstitial: FullscreenCallbacks? = null

    private var rewardAd: RewardAd? = null
    private var rewardedLoaded = false
    private var rewardedLoading = false

    override fun initialize(activity: Activity, onReady: () -> Unit) {
        HwAds.init(appContext)

        interstitialAd = InterstitialAd(appContext).apply {
            adId = appContext.getString(R.string.huawei_interstitial_id)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.i(TAG, "Interstitial loaded")
                }

                override fun onAdFailed(errorCode: Int) {
                    Log.w(TAG, "Interstitial failed: $errorCode")
                    pendingInterstitial?.let { cb ->
                        pendingInterstitial = null
                        cb.onFailedToShow()
                    }
                }

                override fun onAdOpened() {
                    pendingInterstitial?.onShown?.invoke()
                }

                override fun onAdClosed() {
                    val cb = pendingInterstitial
                    pendingInterstitial = null
                    cb?.onClosed?.invoke()
                }
            }
        }

        loadInterstitial()
        loadRewarded()
        onReady()
    }

    override fun isInterstitialReady(): Boolean = interstitialAd?.isLoaded == true

    override fun loadInterstitial() {
        val ad = interstitialAd ?: return
        if (ad.isLoaded || ad.isLoading) return
        try {
            ad.loadAd(AdParam.Builder().build())
        } catch (e: Exception) {
            Log.e(TAG, "loadInterstitial error", e)
        }
    }

    override fun showInterstitial(activity: Activity, callbacks: FullscreenCallbacks) {
        val ad = interstitialAd
        if (ad == null || !ad.isLoaded) {
            callbacks.onFailedToShow()
            return
        }
        pendingInterstitial = callbacks
        ad.show(activity)
    }

    override fun isRewardedReady(): Boolean = rewardAd != null && rewardedLoaded

    override fun loadRewarded() {
        if (rewardedLoading || (rewardAd != null && rewardedLoaded)) return
        try {
            val ad = RewardAd(appContext, appContext.getString(R.string.huawei_rewarded_id))
            rewardAd = ad
            rewardedLoaded = false
            rewardedLoading = true
            ad.loadAd(AdParam.Builder().build(), object : RewardAdLoadListener() {
                override fun onRewardedLoaded() {
                    if (rewardAd === ad) rewardedLoaded = true
                    rewardedLoading = false
                }

                override fun onRewardAdFailedToLoad(errorCode: Int) {
                    Log.w(TAG, "Rewarded failed: $errorCode")
                    if (rewardAd === ad) rewardAd = null
                    rewardedLoading = false
                }
            })
        } catch (e: Exception) {
            rewardedLoading = false
            Log.e(TAG, "loadRewarded error", e)
        }
    }

    override fun showRewarded(activity: Activity, callbacks: FullscreenCallbacks) {
        val ad = rewardAd
        if (ad == null || !rewardedLoaded || !ad.isLoaded) {
            rewardAd = null
            rewardedLoaded = false
            callbacks.onFailedToShow()
            return
        }
        rewardAd = null
        rewardedLoaded = false
        ad.show(activity, object : RewardAdStatusListener() {
            override fun onRewardAdOpened() = callbacks.onShown()

            override fun onRewardAdFailedToShow(errorCode: Int) {
                Log.w(TAG, "Rewarded failed to show: $errorCode")
                callbacks.onFailedToShow()
            }

            override fun onRewardAdClosed() = callbacks.onClosed()

            override fun onRewarded(reward: Reward) = callbacks.onRewarded()
        })
    }

    override fun createBannerView(context: Context, adWidthDp: Int): View =
        BannerView(context).apply {
            adId = context.getString(R.string.huawei_banner_id)
            // 320x50 standart yerine tam genişlik 100dp büyük banner.
            bannerAdSize = BannerAdSize(adWidthDp.coerceAtLeast(320), AdConfig.LARGE_BANNER_HEIGHT_DP)
            setBannerRefresh(BANNER_REFRESH_SECONDS)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.i(TAG, "Banner loaded successfully")
                }

                override fun onAdFailed(errorCode: Int) {
                    Log.w(TAG, "Banner failed: $errorCode")
                }
            }
            loadAd(AdParam.Builder().build())
        }

    override fun destroyBannerView(view: View) {
        (view as? BannerView)?.destroy()
    }

    private companion object {
        const val TAG = "HuaweiAds"
        const val BANNER_REFRESH_SECONDS = 60L
    }
}
