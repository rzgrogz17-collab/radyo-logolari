package com.wello.wallpaperose.advertise;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.wello.wallpaperose.AppConfig;


/**
 * Ekranların tek reklam girişi. Banner, geçiş, ödüllü ve açılış
 * reklamı yalnızca AdMob üzerinden gider.
 */
public class AdNetworkHelper {

    private static final String TAG = "AdNetworkHelper";

    private final Activity activity;
    @Nullable
    private final AdMobAdManager admobAdManager;

    public AdNetworkHelper(@NonNull Activity activity) {
        this.activity = activity;
        if (!AppConfig.Ads.USE_ADMOB) {
            admobAdManager = null;
            Log.d(TAG, "AdMob kapali");
            return;
        }
        admobAdManager = new AdMobAdManager(activity);
        if (activity instanceof LifecycleOwner) {
            ((LifecycleOwner) activity).getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onResume(@NonNull LifecycleOwner owner) {
                    if (admobAdManager != null) admobAdManager.resumeBanner();
                }

                @Override
                public void onPause(@NonNull LifecycleOwner owner) {
                    if (admobAdManager != null) admobAdManager.pauseBanner();
                }

                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    if (admobAdManager != null) admobAdManager.release();
                }
            });
        }
        Log.d(TAG, "Bu ekran icin aktif reklam agi: ADMOB");
    }

    public void updateConsentStatus() {
        if (!AppConfig.ads.ad_enable || !AppConfig.ads.ad_enable_gdpr) return;
        ConsentInformation info = UserMessagingPlatform.getConsentInformation(activity);
        Log.d(TAG, "AdMob consent durumu, reklam istenebilir: " + info.canRequestAds());
    }

    public void init() {
        if (admobAdManager != null) AdMobAdManager.initialize(activity);
    }

    public void loadBannerAd(boolean enable) {
        if (admobAdManager != null) admobAdManager.loadBannerAd(enable);
    }

    public void destroyAndDetachBanner() {
        if (admobAdManager != null) admobAdManager.destroyAndDetachBanner();
    }

    public void loadInterstitialAd(boolean enable) {
        if (admobAdManager != null) admobAdManager.loadInterstitialAd(enable);
    }

    public boolean showInterstitialAd(boolean enable) {
        return admobAdManager != null && admobAdManager.showInterstitialAd(enable);
    }

    public void loadRewardedAd(boolean enable, @NonNull AdRewardedListener listener) {
        if (admobAdManager == null) {
            listener.onError();
            return;
        }
        admobAdManager.loadRewardedAd(enable, listener);
    }

    public boolean showRewardedAd(boolean enable, @NonNull AdRewardedListener listener) {
        return admobAdManager != null && admobAdManager.showRewardedAd(enable, listener);
    }

    /**
     * Splash açılış reklamı. Birim kimliği {@code ad_admob_open_app_unit_id}.
     * Reklam kapanınca, yüklenemezse veya süre dolunca {@code onFinished} bir kez çalışır.
     */
    public void loadAndShowOpenAppAd(Activity host, boolean enable, @Nullable Runnable onFinished) {
        Runnable done = onFinished == null ? () -> { } : onFinished;
        if (admobAdManager == null) {
            done.run();
            return;
        }
        admobAdManager.loadAndShowAppOpen(enable, done);
    }

    public void loadShowUMPConsentForm() {
        loadShowUMPConsentForm(null);
    }

    /**
     * GDPR formu gerekiyorsa gösterilir. Form kapanınca, gerekmiyorsa hemen,
     * istek hata verirse de {@code onDone} bir kez çalışır. Splash bu yüzden takılmaz.
     */
    public void loadShowUMPConsentForm(@Nullable Runnable onDone) {
        if (!AppConfig.ads.ad_enable || !AppConfig.ads.ad_enable_gdpr) {
            if (onDone != null) onDone.run();
            return;
        }

        final boolean[] finished = {false};
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable finish = () -> {
            if (finished[0]) return;
            finished[0] = true;
            handler.removeCallbacksAndMessages(null);
            if (onDone != null) onDone.run();
        };
        if (onDone != null) handler.postDelayed(finish, 20000L);

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();
        ConsentInformation consentInfo = UserMessagingPlatform.getConsentInformation(activity);
        consentInfo.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    handler.removeCallbacksAndMessages(null);
                    if (finished[0] || activity.isFinishing() || activity.isDestroyed()) {
                        finish.run();
                        return;
                    }
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            activity,
                            formError -> {
                                if (formError != null) {
                                    Log.w(TAG, "UMP form hatasi: " + formError.getMessage());
                                }
                                Log.d(TAG, "AdMob consent durumu, reklam istenebilir: " + consentInfo.canRequestAds());
                                finish.run();
                            });
                },
                requestError -> {
                    Log.w(TAG, "UMP info hatasi: " + requestError.getMessage());
                    finish.run();
                }
        );
    }
}
