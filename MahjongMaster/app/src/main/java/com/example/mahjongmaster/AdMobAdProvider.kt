package com.example.mahjongmaster

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * GOOGLE ADMOB — tüm ülkelerde kullanılan tek reklam ağı.
 * Reklam birim ID'leri: strings.xml -> admob_app_id / admob_banner_id /
 * admob_interstitial_id / admob_rewarded_id
 *
 * AB/İngiltere kullanıcıları için Google UMP onay formu otomatik gösterilir
 * (AdMob panelinde "Gizlilik ve mesajlaşma" bölümünden GDPR mesajı oluşturulmalıdır).
 */
internal class AdMobAdProvider(private val appContext: Context) : AdProvider {
    private val sdkStarted = AtomicBoolean(false)
    private var consentInformation: ConsentInformation? = null
    private var onReady: (() -> Unit)? = null

    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false

    override fun initialize(activity: Activity, onReady: () -> Unit) {
        this.onReady = onReady
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { Log.w(TAG, "Consent form: ${it.errorCode} ${it.message}") }
                    if (info.canRequestAds()) startSdk()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.errorCode} ${requestError.message}")
                if (info.canRequestAds()) startSdk()
            }
        )
        // Önceki oturumda alınmış onay varsa reklamlar beklemeden başlasın.
        if (info.canRequestAds()) startSdk()
    }

    private fun startSdk() {
        if (!sdkStarted.compareAndSet(false, true)) return
        Thread {
            try {
                MobileAds.initialize(appContext) {
                    AdManager.runOnMain {
                        Log.i(TAG, "Google Mobile Ads SDK initialized")
                        loadInterstitial()
                        loadRewarded()
                        onReady?.invoke()
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "MobileAds.initialize failed", e)
            }
        }.start()
    }

    fun isPrivacyOptionsRequired(): Boolean =
        consentInformation?.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { Log.w(TAG, "Privacy options form: ${it.message}") }
        }
    }

    override fun isInterstitialReady(): Boolean = interstitialAd != null

    override fun loadInterstitial() {
        if (!sdkStarted.get() || interstitialAd != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            appContext,
            appContext.getString(R.string.admob_interstitial_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed: ${error.code} ${error.message}")
                    interstitialAd = null
                    interstitialLoading = false
                }
            }
        )
    }

    override fun showInterstitial(activity: Activity, callbacks: FullscreenCallbacks) {
        val ad = interstitialAd
        if (ad == null) {
            callbacks.onFailedToShow()
            return
        }
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = callbacks.onShown()
            override fun onAdDismissedFullScreenContent() = callbacks.onClosed()
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                callbacks.onFailedToShow()
            }
        }
        ad.show(activity)
    }

    override fun isRewardedReady(): Boolean = rewardedAd != null

    override fun loadRewarded() {
        if (!sdkStarted.get() || rewardedAd != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(
            appContext,
            appContext.getString(R.string.admob_rewarded_id),
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    rewardedLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed: ${error.code} ${error.message}")
                    rewardedAd = null
                    rewardedLoading = false
                }
            }
        )
    }

    override fun showRewarded(activity: Activity, callbacks: FullscreenCallbacks) {
        val ad = rewardedAd
        if (ad == null) {
            callbacks.onFailedToShow()
            return
        }
        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() = callbacks.onShown()
            override fun onAdDismissedFullScreenContent() = callbacks.onClosed()
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Rewarded failed to show: ${adError.message}")
                callbacks.onFailedToShow()
            }
        }
        ad.show(activity) { callbacks.onRewarded() }
    }

    override fun createBannerView(context: Context, adWidthDp: Int): View =
        AdView(context).apply {
            adUnitId = context.getString(R.string.admob_banner_id)
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp))
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.i(TAG, "Banner loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Banner failed: ${error.code} ${error.message}")
                }
            }
            loadAd(AdRequest.Builder().build())
        }

    override fun destroyBannerView(view: View) {
        (view as? AdView)?.destroy()
    }

    private companion object {
        const val TAG = "AdMobAds"
    }
}
