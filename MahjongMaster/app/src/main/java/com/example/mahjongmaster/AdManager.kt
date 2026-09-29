package com.example.mahjongmaster

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean

enum class AdNetwork { YANDEX, HUAWEI, ADMOB }

/** Tam ekran (geçiş / ödüllü) reklam olayları — her ağ bunları kendi SDK olaylarından çağırır. */
internal class FullscreenCallbacks(
    val onShown: () -> Unit,
    val onRewarded: () -> Unit,
    val onClosed: () -> Unit,
    val onFailedToShow: () -> Unit
)

/**
 * Her reklam ağı (Yandex / Huawei / AdMob) bu arayüzü uygular.
 * Ortak davranış (internet kontrolü, Toast mesajları, yeniden yükleme,
 * yedek/acil hak verme) AdManager içinde TEK yerde yönetilir; böylece
 * üç ağda da oyunun reklam mantığı birebir aynı çalışır.
 */
internal interface AdProvider {
    val network: AdNetwork

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
// ÜLKE TESPİTİ — Yandex mi Huawei mi?
// Sıra: 1) Mobil şebeke ülkesi  2) SIM kart ülkesi
//       3) Daha önce tespit edilmiş şebeke/SIM ülkesi (Wi-Fi tabletler için)
//       4) Saat dilimi  5) Cihaz dili/bölgesi
// ============================================================
object CountryDetector {
    private const val PREFS = "MahjongAdPrefs"
    private const val KEY_LAST_COUNTRY = "last_telephony_country"

    data class Result(val countryCode: String, val source: String)

    fun detect(context: Context): Result {
        val forced = AdConfig.DEBUG_FORCE_COUNTRY
        if (!forced.isNullOrBlank() && isDebuggable(context)) {
            return Result(forced.uppercase(Locale.ROOT), "debug-force")
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val network = normalize(runCatching { tm?.networkCountryIso }.getOrNull())
        val sim = normalize(runCatching { tm?.simCountryIso }.getOrNull())
        val telephony = network ?: sim
        if (telephony != null) {
            prefs.edit().putString(KEY_LAST_COUNTRY, telephony).apply()
            return Result(telephony, if (network != null) "network" else "sim")
        }

        normalize(prefs.getString(KEY_LAST_COUNTRY, null))?.let { return Result(it, "cached") }

        val zoneId = TimeZone.getDefault().id
        yandexCountryForTimeZone(zoneId)?.let { return Result(it, "timezone") }

        val localeCountry = normalize(Locale.getDefault().country)
        // Coğrafi bir saat dilimi (ör. Europe/Berlin) listede yoksa cihaz
        // Yandex bölgesinde değildir; dil ayarı tek başına belirleyici olmaz.
        if (isGeographicZone(zoneId)) {
            val code = localeCountry?.takeUnless { it in AdConfig.YANDEX_COUNTRIES } ?: "ZZ"
            return Result(code, "timezone")
        }
        return Result(localeCountry ?: "ZZ", "locale")
    }

    private fun normalize(code: String?): String? =
        code?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.length == 2 && it.all(Char::isLetter) }

    private fun isDebuggable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun isGeographicZone(id: String): Boolean =
        id.contains('/') && !id.startsWith("Etc/") && !id.startsWith("SystemV/")

    private fun yandexCountryForTimeZone(id: String): String? = when (id) {
        "Europe/Istanbul", "Asia/Istanbul", "Turkey" -> "TR"
        "Europe/Minsk" -> "BY"
        "Europe/Chisinau", "Europe/Tiraspol" -> "MD"
        "Europe/Belgrade" -> "RS"
        "Asia/Tbilisi" -> "GE"
        "Asia/Yerevan" -> "AM"
        "Asia/Baku" -> "AZ"
        "Asia/Bishkek" -> "KG"
        "Asia/Dushanbe" -> "TJ"
        "Asia/Tashkent", "Asia/Samarkand" -> "UZ"
        "Asia/Almaty", "Asia/Qostanay", "Asia/Qyzylorda", "Asia/Aqtobe",
        "Asia/Aqtau", "Asia/Atyrau", "Asia/Oral" -> "KZ"

        "Europe/Moscow", "W-SU", "Europe/Kaliningrad", "Europe/Kirov", "Europe/Volgograd",
        "Europe/Astrakhan", "Europe/Saratov", "Europe/Ulyanovsk", "Europe/Samara",
        "Asia/Yekaterinburg", "Asia/Omsk", "Asia/Novosibirsk", "Asia/Barnaul", "Asia/Tomsk",
        "Asia/Novokuznetsk", "Asia/Krasnoyarsk", "Asia/Irkutsk", "Asia/Chita", "Asia/Yakutsk",
        "Asia/Khandyga", "Asia/Vladivostok", "Asia/Ust-Nera", "Asia/Magadan", "Asia/Sakhalin",
        "Asia/Srednekolymsk", "Asia/Kamchatka", "Asia/Anadyr" -> "RU"

        else -> null
    }
}

// ============================================================
// 1. REKLAM YÖNETİCİSİ — Yandex / Huawei Petal / Google AdMob
// Oyunun geri kalanı yalnızca bu nesnenin initialize / showInterstitial /
// showRewarded fonksiyonlarını ve BannerAdView() composable'ını kullanır.
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

    private val _activeNetwork = MutableStateFlow<AdNetwork?>(null)
    val activeNetwork: StateFlow<AdNetwork?> = _activeNetwork.asStateFlow()

    /** SDK hazır (ve AdMob için kullanıcı onayı alınmış) olduğunda true olur. */
    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    var detectedCountry: String = ""
        private set

    /** Hangi ağın kullanılacağını belirler — tek karar noktası. */
    fun resolveNetwork(context: Context): AdNetwork {
        val result = CountryDetector.detect(context)
        detectedCountry = result.countryCode
        val network = when {
            !AdConfig.USE_YANDEX_AND_HUAWEI -> AdNetwork.ADMOB
            result.countryCode in AdConfig.YANDEX_COUNTRIES -> AdNetwork.YANDEX
            else -> AdNetwork.HUAWEI
        }
        Log.i(TAG, "Country=${result.countryCode} (source=${result.source}) -> $network")
        return network
    }

    fun initialize(activity: Activity) {
        if (provider != null) return
        try {
            val appContext = activity.applicationContext
            val network = resolveNetwork(appContext)
            val created: AdProvider = when (network) {
                AdNetwork.YANDEX -> YandexAdProvider(appContext)
                AdNetwork.HUAWEI -> HuaweiAdProvider(appContext)
                AdNetwork.ADMOB -> AdMobAdProvider(appContext)
            }
            provider = created
            _activeNetwork.value = network
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
 * Aktif reklam ağına göre (Yandex / Huawei / AdMob) alt banner reklamı.
 * SDK hazır olana kadar 50dp'lik alan ayrılır ki oyun alanı zıplamasın.
 */
@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val network by AdManager.activeNetwork.collectAsState()
    val ready by AdManager.adsReady.collectAsState()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        if (network != null && ready) {
            key(network, widthDp) {
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
