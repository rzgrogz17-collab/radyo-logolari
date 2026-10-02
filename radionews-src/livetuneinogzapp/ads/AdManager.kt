package com.globalradio.livetuneinogzapp.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

/**
 * Yandex Mobile Ads yöneticisi.
 *
 * Banner  → mini player'ın altına yerleştirilen 320×50 (BANNER) boyutu.
 * Interstitial → her 10 istasyon tıklamasında bir kez gösterilir;
 *                kapatıldıktan sonra otomatik yeniden yüklenir.
 * Rewarded → uyku zamanlayıcısı süresi bitince gösterilir (bkz. showRewardedThenRun);
 *            kapatıldıktan sonra otomatik yeniden yüklenir.
 */
object AdManager {

    private const val TAG = "AdManager"

    // ── Yandex Ad Unit ID'leri ────────────────────────────────────────────────
    private const val BANNER_UNIT_ID        = "R-M-19532953-1"
    private const val INTERSTITIAL_UNIT_ID  = "R-M-19532953-2"

    // TODO: Bunu Yandex Reklam Ağı arayüzünden aldığınız GERÇEK "rewarded"
    // ad unit ID'niz ile değiştirin (banner "-1", interstitial "-2" ile aynı
    // hesaba ait "-3" varsayımsaldır — yayına almadan önce mutlaka kontrol edin).
    // Test için Yandex'in resmi demo ID'si: "demo-rewarded-yandex"
    private const val REWARDED_UNIT_ID      = "R-M-19532953-3"

    // ── Durum ─────────────────────────────────────────────────────────────────
    private var isInitialized   = false
    private var isLoadingInter  = false
    private var isLoadingRewarded = false

    private var interstitialAd  : InterstitialAd? = null
    private var interstitialLoader: InterstitialAdLoader? = null

    private var rewardedAd      : RewardedAd? = null
    private var rewardedLoader  : RewardedAdLoader? = null

    // Her 10 tıklamada bir interstitial göster
    private var clickCount = 0
    const val INTERSTITIAL_EVERY_N = 10

    // ── Başlatma ──────────────────────────────────────────────────────────────

    /**
     * Application.onCreate() veya Activity.onCreate() içinden çağır.
     * Yandex SDK'sını başlatır ve ilk interstitial + rewarded reklamları
     * arka planda yükler.
     */
    fun initialize(activity: Activity, onReady: () -> Unit = {}) {
        if (isInitialized) {
            onReady()
            return
        }
        MobileAds.initialize(activity) {
            isInitialized = true
            Log.d(TAG, "Yandex MobileAds initialized")
            // Interstitial'ı önceden yükle
            createInterstitialLoader(activity)
            preloadInterstitial(activity)
            // Rewarded'ı önceden yükle (uyku zamanlayıcısı bitişinde kullanılır)
            createRewardedLoader(activity)
            preloadRewarded(activity)
            onReady()
        }
    }

    // ── Banner ────────────────────────────────────────────────────────────────

    /**
     * Mini player'ın altındaki container ViewGroup içine 320×50 banner yükler.
     * AdMob'daki BANNER boyutuyla tam uyumludur.
     *
     * Kullanım (Activity/Fragment):
     *   AdManager.loadBanner(this, binding.adBannerContainer)
     */
    fun loadBanner(activity: Activity, container: ViewGroup, collapseIfEmpty: Boolean = true) {
        try {
            val bannerView = BannerAdView(activity).apply {
                setAdUnitId(BANNER_UNIT_ID)
                // 320×50 — AdMob standart BANNER boyutu
                setAdSize(BannerAdSize.fixedSize(activity, 320, 50))
                setBannerAdEventListener(object : BannerAdEventListener {
                    override fun onAdLoaded() {
                        Log.d(TAG, "Banner loaded")
                        container.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        Log.w(TAG, "Banner failed: ${error.description}")
                        container.visibility = if (collapseIfEmpty) View.GONE else View.INVISIBLE
                    }

                    override fun onAdClicked() {
                        Log.d(TAG, "Banner clicked")
                    }

                    override fun onLeftApplication() {}
                    override fun onReturnedToApplication() {}
                    override fun onImpression(data: ImpressionData?) {}
                })
            }

            container.removeAllViews()
            container.addView(
                bannerView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (56 * activity.resources.displayMetrics.density).toInt()
                )
            )
            container.visibility = if (collapseIfEmpty) View.GONE else View.INVISIBLE

            val adRequest = AdRequest.Builder().build()
            bannerView.loadAd(adRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Banner error: ${e.message}")
        }
    }

    // ── Interstitial ──────────────────────────────────────────────────────────

    private fun createInterstitialLoader(context: Context) {
        interstitialLoader = InterstitialAdLoader(context).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInter = false
                    Log.d(TAG, "Interstitial loaded")

                    // Gösterim / kapatma sonrası otomatik yeniden yükle
                    ad.setAdEventListener(object : InterstitialAdEventListener {
                        override fun onAdShown() {
                            Log.d(TAG, "Interstitial shown")
                        }

                        override fun onAdFailedToShow(error: AdError) {
                            Log.w(TAG, "Interstitial failed to show: ${error.description}")
                            interstitialAd = null
                            // Bir sonraki gösterim için yeniden yükle
                            ad.setAdEventListener(null)
                            preloadInterstitial(context)
                        }

                        override fun onAdDismissed() {
                            Log.d(TAG, "Interstitial dismissed")
                            interstitialAd = null
                            ad.setAdEventListener(null)
                            // Kapatıldıktan sonra yeniden yükle
                            preloadInterstitial(context)
                        }

                        override fun onAdClicked()               { Log.d(TAG, "Interstitial clicked") }
                        override fun onAdImpression(data: ImpressionData?) {}
                    })
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    isLoadingInter = false
                    interstitialAd = null
                    Log.w(TAG, "Interstitial failed to load: ${error.description}")
                }
            })
        }
    }

    /**
     * Interstitial'ı arka planda önceden yükler.
     * initialize() çağrıldıktan sonra otomatik tetiklenir;
     * gerekirse manuel çağırabilirsin.
     */
    fun preloadInterstitial(context: Context) {
        if (isLoadingInter || interstitialAd != null) return
        isLoadingInter = true

        if (interstitialLoader == null) {
            createInterstitialLoader(context)
        }

        val config = AdRequestConfiguration.Builder(INTERSTITIAL_UNIT_ID).build()
        interstitialLoader?.loadAd(config)
    }

    // ── İstasyon tıklama sayacı ───────────────────────────────────────────────

    /**
     * Her istasyon tıklamasında çağır.
     * Her INTERSTITIAL_EVERY_N (10) tıklamada bir interstitial gösterir.
     *
     * Kullanım:
     *   AdManager.onStationClicked(this)
     */
    fun onStationClicked(activity: Activity) {
        clickCount++
        Log.d(TAG, "Station click: $clickCount")
        if (clickCount % INTERSTITIAL_EVERY_N == 0) {
            showInterstitial(activity)
        }
    }

    private fun showInterstitial(activity: Activity) {
        val ad = interstitialAd
        if (ad != null) {
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial not ready yet, preloading…")
            preloadInterstitial(activity)
        }
    }

    // ── Rewarded (Uyku zamanlayıcısı süresi bitince) ────────────────────────────

    private fun createRewardedLoader(context: Context) {
        rewardedLoader = RewardedAdLoader(context).apply {
            setAdLoadListener(object : RewardedAdLoadListener {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    this@AdManager.rewardedAd = rewardedAd
                    isLoadingRewarded = false
                    Log.d(TAG, "Rewarded loaded")
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    isLoadingRewarded = false
                    rewardedAd = null
                    Log.w(TAG, "Rewarded failed to load: ${adRequestError.description}")
                }
            })
        }
    }

    /**
     * Ödüllü reklamı arka planda önceden yükler.
     * initialize() çağrıldıktan sonra otomatik tetiklenir; ayrıca her
     * gösterimden sonra bir sonraki için otomatik yeniden çağrılır.
     */
    fun preloadRewarded(context: Context) {
        if (isLoadingRewarded || rewardedAd != null) return
        isLoadingRewarded = true

        if (rewardedLoader == null) {
            createRewardedLoader(context)
        }

        val config = AdRequestConfiguration.Builder(REWARDED_UNIT_ID).build()
        rewardedLoader?.loadAd(config)
    }

    /**
     * Uyku zamanlayıcısı süresi bitince çağrılır.
     *
     * Reklam hazırsa gösterir; kullanıcı ödülü kazansın ya da kazanmasın,
     * reklam kapatılır kapatılmaz (ya da gösterilemezse HEMEN) [onFinished]
     * çalışır. Böylece reklam ağı yanıt vermese/yüklenmemiş olsa bile uyku
     * zamanlayıcısı her zaman güvenilir şekilde radyoyu durdurur — reklam
     * sadece "varsa bonus", zamanlayıcının çalışmasını asla bloklamaz.
     *
     * [activity] o anda ön planda olan, geçerli bir Activity olmalıdır
     * (tam ekran reklamlar arka planda/ekran kapalıyken gösterilemez).
     */
    fun showRewardedThenRun(activity: Activity, onFinished: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded not ready — radyo direkt durduruluyor")
            preloadRewarded(activity)
            onFinished()
            return
        }

        var alreadyFinished = false
        fun finishOnce() {
            if (alreadyFinished) return
            alreadyFinished = true
            onFinished()
        }

        ad.setAdEventListener(object : RewardedAdEventListener {
            override fun onAdShown() {
                Log.d(TAG, "Rewarded shown")
            }

            override fun onAdFailedToShow(adError: AdError) {
                Log.w(TAG, "Rewarded failed to show: ${adError.description}")
                rewardedAd = null
                ad.setAdEventListener(null)
                preloadRewarded(activity)
                finishOnce()
            }

            override fun onAdDismissed() {
                Log.d(TAG, "Rewarded dismissed")
                rewardedAd = null
                ad.setAdEventListener(null)
                // Kapatıldıktan sonra bir sonraki gösterim için yeniden yükle
                preloadRewarded(activity)
                finishOnce()
            }

            override fun onAdClicked() {
                Log.d(TAG, "Rewarded clicked")
            }

            override fun onAdImpression(impressionData: ImpressionData?) {}

            override fun onRewarded(reward: Reward) {
                Log.d(TAG, "Reward earned: ${reward.amount} ${reward.type}")
            }
        })

        ad.show(activity)
    }
}