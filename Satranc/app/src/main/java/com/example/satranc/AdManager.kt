package com.example.satranc

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/** Tam ekran (geçiş / ödüllü) reklam olayları — her ağ bunları kendi SDK olaylarından çağırır. */
internal class FullscreenCallbacks(
    val onShown: () -> Unit,
    val onRewarded: () -> Unit,
    val onClosed: () -> Unit,
    val onFailedToShow: () -> Unit
)

/**
 * Google AdMob sağlayıcısı. Ortak davranış (internet kontrolü, Toast,
 * yeniden yükleme) AdManager içinde yönetilir.
 */
internal interface AdProvider {

    /** SDK'yı başlatır, geçiş + ödüllü reklamları önceden yükler, hazır olunca [onReady] çağrılır. */
    fun initialize(activity: Activity, onReady: () -> Unit)

    fun isInterstitialReady(): Boolean
    fun loadInterstitial()
    fun showInterstitial(activity: Activity, callbacks: FullscreenCallbacks)

    fun isRewardedReady(): Boolean
    fun loadRewarded()
    fun showRewarded(activity: Activity, callbacks: FullscreenCallbacks)

    fun createBannerView(context: Context, adWidthDp: Int): View
    fun destroyBannerView(view: View)
}

// ============================================================
// REKLAM YÖNETİCİSİ — yalnızca Google AdMob
// Oyunun geri kalanı initialize / showInterstitial / showRewarded
// ve BannerAdView() kullanır.
// ============================================================
object AdManager {
    private const val TAG = "AdManager"

    /** Reklam show() çağrısından sonra bu sürede açılmazsa "gösterilemedi" sayılır. */
    private const val SHOW_WATCHDOG_MS = 5_000L

    /** Kapanma olayı hiç gelmeyen bir reklam, kilidi en fazla bu kadar tutabilir. */
    private const val STALE_FULLSCREEN_MS = 90_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var provider: AdProvider? = null
    private var fullscreenInProgress = false
    private var fullscreenStartedAt = 0L

    /** SDK hazır ve kullanıcı onayı alınmış olduğunda true olur. */
    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    fun initialize(activity: Activity) {
        if (provider != null) return
        try {
            val created = AdMobAdProvider(activity.applicationContext)
            provider = created
            created.initialize(activity) { runOnMain { _adsReady.value = true } }
        } catch (e: Throwable) {
            Log.e(TAG, "Ad initialization failed", e)
        }
    }

    private fun isFullscreenBusy(): Boolean =
        fullscreenInProgress &&
                SystemClock.elapsedRealtime() - fullscreenStartedAt < STALE_FULLSCREEN_MS

    private fun markFullscreenStarted() {
        fullscreenInProgress = true
        fullscreenStartedAt = SystemClock.elapsedRealtime()
    }

    /**
     * Geçiş reklamı göster. Reklam yoksa / gösterilemezse [onAdDismissed] hemen,
     * gösterilirse reklam kapanınca çağrılır — her durumda TAM OLARAK BİR KEZ.
     */
    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        val p = provider
        val done = AtomicBoolean(false)
        val finish: () -> Unit = {
            if (done.compareAndSet(false, true)) runOnMain {
                fullscreenInProgress = false
                onAdDismissed()
            }
        }
        if (p == null || !_adsReady.value || isFullscreenBusy()) {
            runOnMain { onAdDismissed() }
            return
        }
        if (!p.isInterstitialReady()) {
            p.loadInterstitial()
            runOnMain { onAdDismissed() }
            return
        }

        val shown = AtomicBoolean(false)
        markFullscreenStarted()
        val failed: () -> Unit = {
            if (!done.get()) {
                p.loadInterstitial()
                finish()
            }
        }
        try {
            p.showInterstitial(
                activity,
                FullscreenCallbacks(
                    onShown = { shown.set(true) },
                    onRewarded = {},
                    onClosed = failed,
                    onFailedToShow = failed
                )
            )
            mainHandler.postDelayed({ if (!shown.get()) failed() }, SHOW_WATCHDOG_MS)
        } catch (e: Throwable) {
            Log.e(TAG, "showInterstitial error", e)
            failed()
        }
    }

    /**
     * Ödüllü reklam göster.
     * @param onRewardEarned reklam başarıyla izlendiğinde çağrılır (tam hak verir)
     * @param onAdNotAvailable reklam yüklenememişse çağrılır (acil/kısmi hak verilebilir).
     *        Null ise sadece Toast gösterilir.
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onAdNotAvailable: (() -> Unit)? = null
    ) {
        // İnternet yoksa: bilgi göster ve fallback'i çağır.
        if (!NetworkUtils.isOnline(activity)) {
            toast(activity, R.string.no_internet_msg)
            onAdNotAvailable?.invoke()
            return
        }
        val p = provider
        if (p == null || !_adsReady.value || isFullscreenBusy() || !p.isRewardedReady()) {
            toast(activity, R.string.ad_not_ready_msg)
            onAdNotAvailable?.invoke()
            if (p != null && !isFullscreenBusy()) p.loadRewarded()
            return
        }

        val shown = AtomicBoolean(false)
        val rewarded = AtomicBoolean(false)
        val finished = AtomicBoolean(false)
        val failToShow: () -> Unit = {
            if (finished.compareAndSet(false, true)) runOnMain {
                fullscreenInProgress = false
                toast(activity, R.string.ad_not_ready_msg)
                if (!rewarded.get()) onAdNotAvailable?.invoke()
                p.loadRewarded()
            }
        }
        markFullscreenStarted()
        try {
            p.showRewarded(
                activity,
                FullscreenCallbacks(
                    onShown = { shown.set(true) },
                    onRewarded = {
                        if (rewarded.compareAndSet(false, true)) runOnMain { onRewardEarned() }
                    },
                    onClosed = {
                        if (finished.compareAndSet(false, true)) runOnMain {
                            fullscreenInProgress = false
                            p.loadRewarded()
                        }
                    },
                    onFailedToShow = failToShow
                )
            )
            mainHandler.postDelayed({ if (!shown.get()) failToShow() }, SHOW_WATCHDOG_MS)
        } catch (e: Throwable) {
            Log.e(TAG, "showRewarded error", e)
            failToShow()
        }
    }

    internal fun createBannerView(context: Context, adWidthDp: Int): View? = try {
        provider?.createBannerView(context, adWidthDp)
    } catch (e: Throwable) {
        Log.e(TAG, "createBannerView error", e)
        null
    }

    internal fun destroyBannerView(view: View) {
        try {
            provider?.destroyBannerView(view)
        } catch (e: Throwable) {
            Log.w(TAG, "destroyBannerView error", e)
        }
    }

    /** AdMob (UMP) gizlilik seçenekleri formu gerekli mi? — Ayarlar ekranında buton gösterilir. */
    val isPrivacyOptionsRequired: Boolean
        get() = (provider as? AdMobAdProvider)?.isPrivacyOptionsRequired() == true

    fun showPrivacyOptions(activity: Activity) {
        (provider as? AdMobAdProvider)?.showPrivacyOptionsForm(activity)
    }

    internal fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun toast(context: Context, resId: Int) = runOnMain {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
    }
}

/**
 * Google AdMob büyük banner. Yükseklik 100dp: standart 50dp şeridin
 * bir standart banner kadar yukarı uzatılmış hali.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val ready by AdManager.adsReady.collectAsState()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(AdConfig.LARGE_BANNER_HEIGHT_DP.dp),
        contentAlignment = Alignment.Center
    ) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        if (ready) {
            key(widthDp) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    factory = { ctx -> AdManager.createBannerView(ctx, widthDp) ?: View(ctx) },
                    onRelease = { view -> AdManager.destroyBannerView(view) }
                )
            }
        }
    }
}
