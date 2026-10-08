package com.wello.wallpaperose.advertise;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdapterResponseInfo;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.wello.wallpaperose.AppConfig;
import com.wello.wallpaperose.R;


/**
 * Banner, geçiş, ödüllü ve açılış reklamı yalnızca AdMob'dan gelir.
 * Kazanan dolum Yandex, Huawei veya Petal ise reklam gösterilmez.
 */
public class AdMobAdManager {

    private static final String TAG = "AdMobAdManager";
    private static final int MAX_BLOCKED_RETRY = 2;
    private static boolean initialized = false;

    private final Activity activity;

    private AdView bannerView;
    private int bannerBlocked;
    private int bannerAttempt;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private InterstitialAd interstitialAd;
    private boolean interstitialLoading = false;
    private int interstitialBlocked;
    private RewardedAd rewardedAd;
    private boolean rewardedLoading = false;
    private int rewardedBlocked;

    public AdMobAdManager(@NonNull Activity activity) {
        this.activity = activity;
    }

    public static void initialize(@NonNull Activity activity) {
        if (initialized) return;
        initialized = true;
        MobileAds.initialize(activity.getApplicationContext(), status ->
                Log.d(TAG, "AdMob hazir."));
    }

    public void loadBannerAd(boolean enable) {
        bannerHandler.post(() -> loadBannerAd(enable, true));
    }

    private void loadBannerAd(boolean enable, boolean fresh) {
        if (!adsAllowed(enable)) {
            hideBannerSlot();
            return;
        }
        String unitId = unitId(AppConfig.ads.ad_admob_banner_unit_id);
        ViewGroup container = activity.findViewById(R.id.ad_container);
        if (unitId == null || container == null) {
            hideBannerSlot();
            return;
        }
        if (fresh) {
            bannerBlocked = 0;
            bannerAttempt = 0;
        }

        hideBannerSlot();
        AdSize size = bannerAttempt == 1 ? AdSize.BANNER : adaptiveSize();
        bannerView = new AdView(activity);
        bannerView.setAdUnitId(unitId);
        bannerView.setAdSize(size);
        bannerView.setBackgroundColor(Color.TRANSPARENT);
        bannerView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (bannerView == null) return;
                if (blockedNetwork(bannerView.getResponseInfo())) {
                    Log.w(TAG, "Yandex/Huawei banner gosterilmedi.");
                    hideBannerSlot();
                    bannerBlocked++;
                    if (bannerBlocked <= MAX_BLOCKED_RETRY) loadBannerAd(enable, false);
                    return;
                }
                bannerBlocked = 0;
                bannerAttempt = 0;
                container.setBackgroundColor(Color.TRANSPARENT);
                container.setVisibility(View.VISIBLE);
                container.post(() -> {
                    if (bannerView == null || container.getVisibility() != View.VISIBLE) {
                        setPagerBottomMargin(0);
                        return;
                    }
                    setPagerBottomMargin(bannerBarHeight());
                });
                Log.d(TAG, "AdMob banner yuklendi.");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                Log.w(TAG, "Banner hatasi: " + error.getCode() + " " + error.getMessage());
                hideBannerSlot();
                bannerAttempt++;
                if (bannerAttempt < 3 && !activity.isFinishing() && !activity.isDestroyed()) {
                    bannerHandler.postDelayed(() -> loadBannerAd(enable, false), bannerAttempt == 1 ? 2000L : 5000L);
                }
            }
        });
        container.setBackgroundColor(Color.TRANSPARENT);
        container.setMinimumHeight(0);
        container.setVisibility(View.GONE);
        container.removeAllViews();
        container.addView(bannerView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        bannerView.loadAd(new AdRequest.Builder().build());
    }

    private void hideBannerSlot() {
        destroyBanner();
        ViewGroup container = activity.findViewById(R.id.ad_container);
        if (container != null) {
            container.setBackgroundColor(Color.TRANSPARENT);
            container.setMinimumHeight(0);
            container.removeAllViews();
            container.setVisibility(View.GONE);
        }
        setPagerBottomMargin(0);
    }

    private int bannerBarHeight() {
        View bar = activity.findViewById(R.id.lyt_bar);
        if (bar != null && bar.getHeight() > 0) return bar.getHeight();
        return bannerView == null ? 0 : bannerView.getHeight();
    }

    private void setPagerBottomMargin(int heightPx) {
        View pager = activity.findViewById(R.id.view_pager_main);
        if (!(pager != null && pager.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) return;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) pager.getLayoutParams();
        if (lp.bottomMargin == heightPx) return;
        lp.bottomMargin = heightPx;
        pager.setLayoutParams(lp);
    }

    public void pauseBanner() {
        if (bannerView != null) bannerView.pause();
    }

    public void resumeBanner() {
        if (bannerView != null) bannerView.resume();
    }

    public void destroyAndDetachBanner() {
        destroyBanner();
    }

    public void release() {
        bannerHandler.removeCallbacksAndMessages(null);
        hideBannerSlot();
        interstitialAd = null;
        interstitialLoading = false;
        rewardedAd = null;
        rewardedLoading = false;
    }

    private void destroyBanner() {
        AdView view = bannerView;
        if (view == null) return;
        bannerView = null;
        view.setAdListener(null);
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) parent.removeView(view);
        view.destroy();
    }

    private AdSize adaptiveSize() {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        float widthPixels = metrics.widthPixels;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
            widthPixels = windowMetrics.getBounds().width();
        }
        float density = metrics.density <= 0f ? 1f : metrics.density;
        int adWidth = (int) (widthPixels / density);
        if (adWidth <= 0) adWidth = 320;
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth);
    }

    public void loadInterstitialAd(boolean enable) {
        if (!adsAllowed(enable) || interstitialLoading || interstitialAd != null) return;
        String unitId = unitId(AppConfig.ads.ad_admob_interstitial_unit_id);
        if (unitId == null) return;
        interstitialLoading = true;
        InterstitialAd.load(activity, unitId, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialLoading = false;
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (blockedNetwork(ad.getResponseInfo())) {
                            interstitialAd = null;
                            interstitialBlocked++;
                            Log.w(TAG, "Yandex/Huawei gecis reklami gosterilmedi.");
                            if (interstitialBlocked <= MAX_BLOCKED_RETRY) loadInterstitialAd(enable);
                            return;
                        }
                        interstitialBlocked = 0;
                        interstitialAd = ad;
                        Log.d(TAG, "AdMob interstitial yuklendi.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        interstitialAd = null;
                        interstitialLoading = false;
                        Log.w(TAG, "Interstitial hatasi: " + error.getCode() + " " + error.getMessage());
                    }
                });
    }

    public boolean showInterstitialAd(boolean enable) {
        if (!adsAllowed(enable)) return false;
        if (interstitialAd == null || blockedNetwork(interstitialAd.getResponseInfo())) {
            interstitialAd = null;
            loadInterstitialAd(enable);
            return false;
        }
        if (activity.isFinishing() || activity.isDestroyed() || !activity.hasWindowFocus()) {
            Log.w(TAG, "Interstitial ertelendi: ekran henuz odakli degil");
            return false;
        }
        InterstitialAd showing = interstitialAd;
        interstitialAd = null;
        showing.setImmersiveMode(false);
        showing.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "AdMob interstitial gosterildi.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                loadInterstitialAd(enable);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.w(TAG, "Interstitial gosterilemedi: " + adError.getMessage());
                loadInterstitialAd(enable);
            }
        });
        try {
            showing.show(activity);
            return true;
        } catch (RuntimeException ex) {
            Log.w(TAG, "Interstitial gosterilemedi: " + ex.getMessage());
            loadInterstitialAd(enable);
            return false;
        }
    }

    public void loadRewardedAd(boolean enable, @NonNull AdRewardedListener listener) {
        if (!adsAllowed(enable) || rewardedLoading || rewardedAd != null) return;
        String unitId = unitId(AppConfig.ads.ad_admob_rewarded_unit_id);
        if (unitId == null) {
            listener.onError();
            return;
        }
        rewardedLoading = true;
        RewardedAd.load(activity, unitId, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedLoading = false;
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (blockedNetwork(ad.getResponseInfo())) {
                            rewardedAd = null;
                            rewardedBlocked++;
                            Log.w(TAG, "Yandex/Huawei odullu reklam gosterilmedi.");
                            if (rewardedBlocked <= MAX_BLOCKED_RETRY) {
                                loadRewardedAd(enable, listener);
                            } else {
                                listener.onError();
                            }
                            return;
                        }
                        rewardedBlocked = 0;
                        rewardedAd = ad;
                        Log.d(TAG, "AdMob rewarded yuklendi.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedAd = null;
                        rewardedLoading = false;
                        Log.w(TAG, "Rewarded hatasi: " + error.getCode() + " " + error.getMessage());
                        listener.onError();
                    }
                });
    }

    /**
     * Reklam hazır değilse false döner ve işlemi çağıran taraf sürdürür.
     * Ödül, reklam kapandıktan sonra bir kez bildirilir. Erken kapatmada ödül yoktur.
     */
    public boolean showRewardedAd(boolean enable, @NonNull AdRewardedListener listener) {
        if (!adsAllowed(enable)) return false;
        if (activity.isFinishing() || activity.isDestroyed()) return false;
        if (rewardedAd == null || blockedNetwork(rewardedAd.getResponseInfo())) {
            rewardedAd = null;
            loadRewardedAd(enable, emptyRewardListener());
            return false;
        }
        final boolean[] earned = {false};
        final boolean[] settled = {false};
        RewardedAd showing = rewardedAd;
        rewardedAd = null;
        showing.setImmersiveMode(false);
        showing.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "AdMob rewarded gosterildi.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                if (settled[0]) return;
                settled[0] = true;
                if (earned[0]) listener.onComplete();
                else listener.onDismissed();
                loadRewardedAd(enable, emptyRewardListener());
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                if (settled[0]) return;
                settled[0] = true;
                Log.w(TAG, "Rewarded gosterilemedi: " + adError.getMessage());
                listener.onError();
                loadRewardedAd(enable, emptyRewardListener());
            }
        });
        try {
            showing.show(activity, rewardItem -> earned[0] = true);
            return true;
        } catch (RuntimeException ex) {
            Log.w(TAG, "Rewarded gosterilemedi: " + ex.getMessage());
            loadRewardedAd(enable, emptyRewardListener());
            return false;
        }
    }

    /**
     * Açılış reklamı. {@code onFinished} kapanınca, gösterilemeyince,
     * yüklenemeyince veya süre dolunca tam bir kez çalışır.
     */
    public void loadAndShowAppOpen(boolean enable, @NonNull Runnable onFinished) {
        if (!adsAllowed(enable)) {
            onFinished.run();
            return;
        }
        String unitId = unitId(AppConfig.ads.ad_admob_open_app_unit_id);
        if (unitId == null) {
            onFinished.run();
            return;
        }
        final boolean[] done = {false};
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable finish = () -> {
            if (done[0]) return;
            done[0] = true;
            handler.removeCallbacksAndMessages(null);
            onFinished.run();
        };
        int seconds = 10;
        if (AppConfig.ads.limit_time_open_app_loading != null && AppConfig.ads.limit_time_open_app_loading > 0) {
            seconds = AppConfig.ads.limit_time_open_app_loading;
        }
        handler.postDelayed(finish, seconds * 1000L);
        AppOpenAd.load(activity.getApplicationContext(), unitId, new AdRequest.Builder().build(),
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        if (done[0]) return;
                        if (blockedNetwork(ad.getResponseInfo())) {
                            Log.w(TAG, "Yandex/Huawei acilis reklami gosterilmedi.");
                            finish.run();
                            return;
                        }
                        presentOpen(ad, finish, done, handler, 0);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        Log.w(TAG, "Acilis reklami yuklenemedi: " + error.getCode() + " " + error.getMessage());
                        finish.run();
                    }
                });
    }

    private void presentOpen(AppOpenAd ad, Runnable finish, boolean[] done, Handler handler, int attempt) {
        handler.postDelayed(() -> {
            if (done[0]) return;
            if (activity.isFinishing() || activity.isDestroyed()) {
                finish.run();
                return;
            }
            if (!activity.hasWindowFocus() && attempt < 8) {
                presentOpen(ad, finish, done, handler, attempt + 1);
                return;
            }
            if (!activity.hasWindowFocus()) {
                Log.w(TAG, "Acilis reklami ertelendi: ekran odakta degil");
                finish.run();
                return;
            }
            ad.setImmersiveMode(false);
            ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "AdMob acilis reklami gosterildi.");
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    finish.run();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.w(TAG, "Acilis reklami gosterilemedi: " + adError.getMessage());
                    finish.run();
                }
            });
            handler.removeCallbacks(finish);
            handler.postDelayed(finish, 45000L);
            try {
                ad.show(activity);
            } catch (RuntimeException ex) {
                Log.w(TAG, "Acilis reklami gosterilemedi: " + ex.getMessage());
                finish.run();
            }
        }, attempt == 0 ? 250 : 200);
    }

    private boolean adsAllowed(boolean enable) {
        return enable && AppConfig.ads != null && AppConfig.ads.ad_enable && AppConfig.Ads.USE_ADMOB;
    }

    private static String unitId(String id) {
        if (id == null) return null;
        String trimmed = id.trim();
        if (!trimmed.startsWith("ca-app-pub-") || !trimmed.contains("/")) return null;
        return trimmed;
    }

    /** Yalnızca kazanan doluma bakılır. AdMob dolumu kalır. */
    private static boolean blockedNetwork(ResponseInfo info) {
        if (info == null) return false;
        AdapterResponseInfo loaded = info.getLoadedAdapterResponseInfo();
        String source = loaded == null ? "" : loaded.getAdSourceName();
        String instance = loaded == null ? "" : loaded.getAdSourceInstanceName();
        String adapter = loaded == null ? info.getMediationAdapterClassName() : loaded.getAdapterClassName();
        String blob = ((source == null ? "" : source) + " "
                + (instance == null ? "" : instance) + " "
                + (adapter == null ? "" : adapter)).toLowerCase(java.util.Locale.US);
        if (blob.trim().isEmpty()) return false;
        return blob.contains("yandex")
                || blob.contains("petal")
                || blob.contains("huawei")
                || blob.contains("hms.ads")
                || blob.contains("openalliance");
    }

    private static AdRewardedListener emptyRewardListener() {
        return new AdRewardedListener() {
            @Override public void onComplete() {}
            @Override public void onDismissed() {}
            @Override public void onError() {}
        };
    }
}
