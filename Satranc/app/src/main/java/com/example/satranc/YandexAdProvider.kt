package com.example.satranc

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import com.yandex.mobile.ads.banner.BannerAdView as YandexBannerAdView

/**
 * YANDEX MOBILE ADS 8 — AdConfig.YANDEX_COUNTRIES listesindeki ülkelerde kullanılır.
 * Reklam birim ID'leri: strings.xml -> yandex_banner_id / yandex_interstitial_id / yandex_rewarded_id
 *
 * SDK 8: MobileAds -> YandexAds, AdRequestConfiguration kalktı,
 * loadAd(AdRequest, listener). Alt bant: standart 50dp yerine 100dp büyük banner
 * (BannerAdSize.inline, en fazla bir standart banner kadar yukarı uzar).
 */
internal class YandexAdProvider(private val appContext: Context) : AdProvider {
    override val network = AdNetwork.YANDEX

    private var interstitialAdLoader: InterstitialAdLoader? = null
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    private var rewardedAdLoader: RewardedAdLoader? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false

    override fun initialize(activity: Activity, onReady: () -> Unit) {
        YandexAds.initialize(appContext) {
            Log.i(TAG, "Yandex Mobile Ads SDK initialized")
        }

        interstitialAdLoader = InterstitialAdLoader(appContext)
        rewardedAdLoader = RewardedAdLoader(appContext)

        loadInterstitial()
        loadRewarded()
        onReady()
    }

    override fun isInterstitialReady(): Boolean = interstitialAd != null

    override fun loadInterstitial() {
        val loader = interstitialAdLoader ?: return
        if (interstitialAd != null || interstitialLoading) return
        try {
            val adUnitId = appContext.getString(R.string.yandex_interstitial_id)
            interstitialLoading = true
            loader.loadAd(
                AdRequest.Builder(adUnitId).build(),
                object : InterstitialAdLoadListener {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        this@YandexAdProvider.interstitialAd = interstitialAd
                        interstitialLoading = false
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        Log.w(TAG, "Interstitial failed: ${error.description}")
                        interstitialAd = null
                        interstitialLoading = false
                    }
                }
            )
        } catch (e: Exception) {
            interstitialLoading = false
            Log.e(TAG, "loadInterstitial error", e)
        }
    }

    override fun showInterstitial(activity: Activity, callbacks: FullscreenCallbacks) {
        val ad = interstitialAd
        if (ad == null) {
            callbacks.onFailedToShow()
            return
        }
        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() = callbacks.onShown()

            override fun onAdFailedToShow(adError: AdError) {
                interstitialAd?.setAdEventListener(null)
                interstitialAd = null
                callbacks.onFailedToShow()
            }

            override fun onAdDismissed() {
                interstitialAd?.setAdEventListener(null)
                interstitialAd = null
                callbacks.onClosed()
            }

            override fun onAdClicked() {}

            override fun onAdImpression(impressionData: ImpressionData?) {}
        })
        ad.show(activity)
    }

    override fun isRewardedReady(): Boolean = rewardedAd != null

    override fun loadRewarded() {
        val loader = rewardedAdLoader ?: return
        if (rewardedAd != null || rewardedLoading) return
        try {
            val adUnitId = appContext.getString(R.string.yandex_rewarded_id)
            rewardedLoading = true
            loader.loadAd(
                AdRequest.Builder(adUnitId).build(),
                object : RewardedAdLoadListener {
                    override fun onAdLoaded(rewarded: RewardedAd) {
                        rewardedAd = rewarded
                        rewardedLoading = false
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        Log.w(TAG, "Rewarded failed: ${error.description}")
                        rewardedAd = null
                        rewardedLoading = false
                    }
                }
            )
        } catch (e: Exception) {
            rewardedLoading = false
            Log.e(TAG, "loadRewarded error", e)
        }
    }

    override fun showRewarded(activity: Activity, callbacks: FullscreenCallbacks) {
        val ad = rewardedAd
        if (ad == null) {
            callbacks.onFailedToShow()
            return
        }
        ad.setAdEventListener(object : RewardedAdEventListener {
            override fun onAdShown() = callbacks.onShown()

            override fun onAdFailedToShow(adError: AdError) {
                rewardedAd?.setAdEventListener(null)
                rewardedAd = null
                callbacks.onFailedToShow()
            }

            override fun onAdDismissed() {
                rewardedAd?.setAdEventListener(null)
                rewardedAd = null
                callbacks.onClosed()
            }

            override fun onAdClicked() {}

            override fun onAdImpression(impressionData: ImpressionData?) {}

            override fun onRewarded(reward: Reward) = callbacks.onRewarded()
        })
        ad.show(activity)
    }

    override fun createBannerView(context: Context, adWidthDp: Int): View =
        YandexBannerAdView(context).apply {
            val width = adWidthDp.coerceAtLeast(320)
            // Büyük banner: standart 50dp + bir standart banner yüksekliği = 100dp.
            setAdSize(BannerAdSize.inline(context, width, AdConfig.LARGE_BANNER_HEIGHT_DP))
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                (AdConfig.LARGE_BANNER_HEIGHT_DP * context.resources.displayMetrics.density).toInt()
            )
            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    Log.i(TAG, "Banner loaded successfully")
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.w(TAG, "Banner failed: ${error.description}")
                }

                override fun onAdClicked() {}

                override fun onImpression(impressionData: ImpressionData?) {}
            })
            loadAd(AdRequest.Builder(context.getString(R.string.yandex_banner_id)).build())
        }

    override fun destroyBannerView(view: View) {
        (view as? YandexBannerAdView)?.destroy()
    }

    private companion object {
        const val TAG = "YandexAds"
    }
}
