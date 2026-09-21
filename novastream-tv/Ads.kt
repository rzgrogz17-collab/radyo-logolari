package tv.garden.global.webapp

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

@Composable
fun YandexBannerAdView() {
    AndroidView(
        factory = { ctx ->
            BannerAdView(ctx).apply {
                setAdUnitId(AppConfig.YANDEX_BANNER_ID)
                setAdSize(BannerAdSize.fixedSize(ctx, 320, 50))
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}

private var interstitialCounter = 0
private var loadedInterstitial: InterstitialAd? = null

fun showInterstitialAd(act: Activity, onDone: (() -> Unit)? = null) {
    interstitialCounter++
    if (onDone == null && interstitialCounter % 5 != 0) return
    val callback = onDone ?: {}
    val prefs = act.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getString("consent_type", "NONE") == "NONE") {
        callback(); return
    }

    val loader = InterstitialAdLoader(act)
    loader.setAdLoadListener(object : InterstitialAdLoadListener {
        override fun onAdLoaded(ad: InterstitialAd) {
            loadedInterstitial = ad
            ad.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {}
                override fun onAdFailedToShow(err: com.yandex.mobile.ads.common.AdError) {
                    callback()
                }

                override fun onAdDismissed() {
                    callback(); loadedInterstitial = null
                }

                override fun onAdClicked() {}
                override fun onAdImpression(data: ImpressionData?) {}
            })
            ad.show(act)
        }

        override fun onAdFailedToLoad(err: AdRequestError) {
            callback()
        }
    })
    loader.loadAd(AdRequestConfiguration.Builder(AppConfig.YANDEX_INTERSTITIAL_ID).build())
}

private var loadedRewarded: RewardedAd? = null

fun showRewardedAd(act: Activity, onDone: () -> Unit) {
    val prefs = act.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getString("consent_type", "NONE") == "NONE") {
        onDone(); return
    }

    val loader = RewardedAdLoader(act)
    loader.setAdLoadListener(object : RewardedAdLoadListener {
        override fun onAdLoaded(ad: RewardedAd) {
            loadedRewarded = ad
            ad.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() {}
                override fun onAdFailedToShow(err: com.yandex.mobile.ads.common.AdError) {
                    onDone()
                }

                override fun onAdDismissed() {
                    onDone(); loadedRewarded = null
                }

                override fun onAdClicked() {}
                override fun onAdImpression(data: ImpressionData?) {}
                override fun onRewarded(reward: Reward) {}
            })
            ad.show(act)
        }

        override fun onAdFailedToLoad(err: AdRequestError) {
            onDone()
        }
    })
    loader.loadAd(AdRequestConfiguration.Builder(AppConfig.YANDEX_REWARDED_ID).build())
}
