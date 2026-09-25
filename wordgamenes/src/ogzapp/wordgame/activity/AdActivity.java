package ogzapp.wordgame.activity;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.InterstitialAd;
import com.huawei.hms.ads.banner.BannerView;
import com.huawei.hms.ads.reward.Reward;
import com.huawei.hms.ads.reward.RewardAd;
import com.huawei.hms.ads.reward.RewardAdLoadListener;
import com.huawei.hms.ads.reward.RewardAdStatusListener;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

import ogzapp.wordgame.R;
import ogzapp.wordgame.managers.AdManager;
import ogzapp.wordgame.util.RewardedVideoCloseCallback;

/**
 * Splash açılışında ülke bulunur, yalnızca seçilen SDK başlatılır.
 * AdMob açıksa her yerde AdMob. Kapalıysa listedeki ülkelerde Yandex,
 * diğerlerinde Huawei Petal. Ödüllü ve geçiş reklamı üç ağda da aynı
 * kapanış geri çağrısıyla çalışır. Banner ayrı anahtarla açılır.
 */
public class AdActivity extends AndroidApplication implements AdManager {

    private AdsConfig.Network network = AdsConfig.Network.YANDEX;
    private boolean sdkReady;
    private boolean rewardedQueued;
    private boolean interstitialQueued;
    private boolean rewardedLoading;
    private boolean interstitialLoading;

    private RewardedAd yandexRewardedAd;
    private com.yandex.mobile.ads.interstitial.InterstitialAd yandexInterstitialAd;
    private BannerAdView yandexBanner;

    private com.google.android.gms.ads.rewarded.RewardedAd admobRewardedAd;
    private com.google.android.gms.ads.interstitial.InterstitialAd admobInterstitialAd;
    private AdView admobBanner;

    private RewardAd huaweiRewardedAd;
    private InterstitialAd huaweiInterstitialAd;
    private BannerView huaweiBanner;

    private RewardedVideoCloseCallback rewardedAdFinishedCallback;
    private Runnable interstitialClosedCallback;
    private boolean rewardEarned;

    private RelativeLayout rootLayout;
    private View gameView;
    private RelativeLayout bannerSlot;
    private boolean bannerRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CountryDetector.detect(this, new CountryDetector.Callback() {
            @Override
            public void onCountry(String isoAlpha2) {
                network = AdsConfig.resolve(isoAlpha2);
                System.out.println("Ad route country=" + isoAlpha2 + " network=" + network);
                initChosenSdk();
            }
        });
    }

    void attachAdLayout(RelativeLayout layout, View game) {
        rootLayout = layout;
        gameView = game;
        tryLoadBanner();
    }

    private void initChosenSdk() {
        Runnable ready = new Runnable() {
            @Override
            public void run() {
                sdkReady = true;
                initRewardedAds();
                loadInterstitialAds();
                tryLoadBanner();
            }
        };
        if (network == AdsConfig.Network.ADMOB) {
            MobileAds.initialize(this, initializationStatus -> runOnUiThread(ready));
        } else if (network == AdsConfig.Network.HUAWEI) {
            HwAds.init(this);
            ready.run();
        } else {
            com.yandex.mobile.ads.common.MobileAds.initialize(this, () -> runOnUiThread(ready));
        }
    }

    // ── REWARDED ──────────────────────────────────────────────────────────────
    public void initRewardedAds() {
        if (!sdkReady) {
            rewardedQueued = true;
            return;
        }
        if (rewardedLoading || isRewardedAdLoaded()) return;
        rewardedLoading = true;
        rewardedQueued = false;
        if (network == AdsConfig.Network.ADMOB) loadAdmobRewarded();
        else if (network == AdsConfig.Network.HUAWEI) loadHuaweiRewarded();
        else loadYandexRewarded();
    }

    private void loadYandexRewarded() {
        runOnUiThread(() -> {
            RewardedAdLoader loader = new RewardedAdLoader(this);
            loader.setAdLoadListener(new RewardedAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    rewardedLoading = false;
                    yandexRewardedAd = ad;
                    yandexRewardedAd.setAdEventListener(new RewardedAdEventListener() {
                        @Override public void onAdShown() {}
                        @Override public void onAdClicked() {}
                        @Override public void onAdImpression(@Nullable ImpressionData data) {}

                        @Override
                        public void onAdFailedToShow(@NonNull AdError error) {
                            yandexRewardedAd = null;
                            finishRewarded(false);
                        }

                        @Override
                        public void onAdDismissed() {
                            yandexRewardedAd = null;
                            finishRewarded(rewardEarned);
                        }

                        @Override
                        public void onRewarded(@NonNull com.yandex.mobile.ads.rewarded.Reward reward) {
                            rewardEarned = true;
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError error) {
                    rewardedLoading = false;
                    yandexRewardedAd = null;
                }
            });
            loader.loadAd(new AdRequestConfiguration.Builder(AdsConfig.YANDEX_REWARDED_ID).build());
        });
    }

    private void loadAdmobRewarded() {
        runOnUiThread(() -> com.google.android.gms.ads.rewarded.RewardedAd.load(
                this,
                AdsConfig.ADMOB_REWARDED_ID,
                new AdRequest.Builder().build(),
                new com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull com.google.android.gms.ads.rewarded.RewardedAd ad) {
                        rewardedLoading = false;
                        admobRewardedAd = ad;
                        admobRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                admobRewardedAd = null;
                                finishRewarded(rewardEarned);
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                                admobRewardedAd = null;
                                finishRewarded(false);
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedLoading = false;
                        admobRewardedAd = null;
                    }
                }));
    }

    private void loadHuaweiRewarded() {
        runOnUiThread(() -> {
            huaweiRewardedAd = new RewardAd(this, AdsConfig.HUAWEI_REWARDED_ID);
            huaweiRewardedAd.loadAd(new AdParam.Builder().build(), new RewardAdLoadListener() {
                @Override
                public void onRewardedLoaded() {
                    rewardedLoading = false;
                }

                @Override
                public void onRewardAdFailedToLoad(int errorCode) {
                    rewardedLoading = false;
                    huaweiRewardedAd = null;
                }
            });
        });
    }

    // ── INTERSTITIAL ──────────────────────────────────────────────────────────
    public void loadInterstitialAds() {
        if (!sdkReady) {
            interstitialQueued = true;
            return;
        }
        if (interstitialLoading || isInterstitialAdLoaded()) return;
        interstitialLoading = true;
        interstitialQueued = false;
        if (network == AdsConfig.Network.ADMOB) loadAdmobInterstitial();
        else if (network == AdsConfig.Network.HUAWEI) loadHuaweiInterstitial();
        else loadYandexInterstitial();
    }

    private void loadYandexInterstitial() {
        runOnUiThread(() -> {
            InterstitialAdLoader loader = new InterstitialAdLoader(this);
            loader.setAdLoadListener(new InterstitialAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull com.yandex.mobile.ads.interstitial.InterstitialAd ad) {
                    interstitialLoading = false;
                    yandexInterstitialAd = ad;
                    yandexInterstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                        @Override public void onAdShown() {}
                        @Override public void onAdClicked() {}
                        @Override public void onAdImpression(@Nullable ImpressionData data) {}

                        @Override
                        public void onAdFailedToShow(@NonNull AdError error) {
                            yandexInterstitialAd = null;
                            finishInterstitial();
                        }

                        @Override
                        public void onAdDismissed() {
                            yandexInterstitialAd = null;
                            finishInterstitial();
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError error) {
                    interstitialLoading = false;
                    yandexInterstitialAd = null;
                }
            });
            loader.loadAd(new AdRequestConfiguration.Builder(AdsConfig.YANDEX_INTERSTITIAL_ID).build());
        });
    }

    private void loadAdmobInterstitial() {
        runOnUiThread(() -> com.google.android.gms.ads.interstitial.InterstitialAd.load(
                this,
                AdsConfig.ADMOB_INTERSTITIAL_ID,
                new AdRequest.Builder().build(),
                new com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd ad) {
                        interstitialLoading = false;
                        admobInterstitialAd = ad;
                        admobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                admobInterstitialAd = null;
                                finishInterstitial();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                                admobInterstitialAd = null;
                                finishInterstitial();
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialLoading = false;
                        admobInterstitialAd = null;
                    }
                }));
    }

    private void loadHuaweiInterstitial() {
        runOnUiThread(() -> {
            huaweiInterstitialAd = new InterstitialAd(this);
            huaweiInterstitialAd.setAdId(AdsConfig.HUAWEI_INTERSTITIAL_ID);
            huaweiInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    interstitialLoading = false;
                }

                @Override
                public void onAdFailed(int errorCode) {
                    interstitialLoading = false;
                    huaweiInterstitialAd = null;
                }

                @Override
                public void onAdClosed() {
                    huaweiInterstitialAd = null;
                    finishInterstitial();
                }
            });
            huaweiInterstitialAd.loadAd(new AdParam.Builder().build());
        });
    }

    // ── BANNER ────────────────────────────────────────────────────────────────
    private void tryLoadBanner() {
        if (!AdsConfig.BANNER_ENABLED || !sdkReady || rootLayout == null || bannerRequested) return;
        bannerRequested = true;
        if (bannerSlot == null) {
            bannerSlot = new RelativeLayout(this);
            bannerSlot.setId(View.generateViewId());
            RelativeLayout.LayoutParams slotLp = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slotLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            rootLayout.addView(bannerSlot, slotLp);
            if (gameView != null) {
                RelativeLayout.LayoutParams gameLp = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                gameLp.addRule(RelativeLayout.ABOVE, bannerSlot.getId());
                gameView.setLayoutParams(gameLp);
            }
        }
        if (network == AdsConfig.Network.ADMOB) loadAdmobBanner();
        else if (network == AdsConfig.Network.HUAWEI) loadHuaweiBanner();
        else loadYandexBanner();
    }

    private void loadYandexBanner() {
        yandexBanner = new BannerAdView(this);
        yandexBanner.setAdUnitId(AdsConfig.YANDEX_BANNER_ID);
        yandexBanner.setAdSize(com.yandex.mobile.ads.common.AdSize.stickySize(this, bannerWidthDp()));
        yandexBanner.setBannerAdEventListener(new BannerAdEventListener() {
            @Override public void onAdLoaded() {}
            @Override public void onAdFailedToLoad(@NonNull AdRequestError error) {}
            @Override public void onAdClicked() {}
            @Override public void onImpression(@Nullable ImpressionData data) {}
            @Override public void onLeftApplication() {}
            @Override public void onReturnedToApplication() {}
        });
        bannerSlot.addView(yandexBanner, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        yandexBanner.loadAd(new com.yandex.mobile.ads.common.AdRequest.Builder().build());
    }

    private void loadAdmobBanner() {
        admobBanner = new AdView(this);
        admobBanner.setAdUnitId(AdsConfig.ADMOB_BANNER_ID);
        admobBanner.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
        bannerSlot.addView(admobBanner, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        admobBanner.loadAd(new AdRequest.Builder().build());
    }

    private void loadHuaweiBanner() {
        huaweiBanner = new BannerView(this);
        huaweiBanner.setAdId(AdsConfig.HUAWEI_BANNER_ID);
        huaweiBanner.setBannerAdSize(BannerAdSize.BANNER_SIZE_320_50);
        bannerSlot.addView(huaweiBanner, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        huaweiBanner.loadAd(new AdParam.Builder().build());
    }

    private int bannerWidthDp() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return Math.max(1, Math.round(metrics.widthPixels / metrics.density));
    }

    // ── AdManager ─────────────────────────────────────────────────────────────
    @Override public boolean isInterstitialAdEnabled()        { return true; }
    @Override public boolean isRewardedAdEnabledToEarnCoins() { return true; }
    @Override public boolean isRewardedAdEnabledToEarnMoves() { return true; }
    @Override public boolean isRewardedAdEnabledToSpinWheel() { return true; }

    @Override
    public boolean isRewardedAdLoaded() {
        if (network == AdsConfig.Network.ADMOB) return admobRewardedAd != null;
        if (network == AdsConfig.Network.HUAWEI) return huaweiRewardedAd != null && huaweiRewardedAd.isLoaded();
        return yandexRewardedAd != null;
    }

    @Override
    public boolean isInterstitialAdLoaded() {
        if (network == AdsConfig.Network.ADMOB) return admobInterstitialAd != null;
        if (network == AdsConfig.Network.HUAWEI) return huaweiInterstitialAd != null && huaweiInterstitialAd.isLoaded();
        return yandexInterstitialAd != null;
    }

    @Override
    public void showInterstitialAd(final Runnable closedCallback) {
        try {
            runOnUiThread(() -> {
                interstitialClosedCallback = closedCallback;
                if (network == AdsConfig.Network.ADMOB && admobInterstitialAd != null) {
                    admobInterstitialAd.show(AdActivity.this);
                } else if (network == AdsConfig.Network.HUAWEI && huaweiInterstitialAd != null && huaweiInterstitialAd.isLoaded()) {
                    huaweiInterstitialAd.show(AdActivity.this);
                } else if (network == AdsConfig.Network.YANDEX && yandexInterstitialAd != null) {
                    yandexInterstitialAd.show(AdActivity.this);
                } else {
                    loadInterstitialAds();
                    if (closedCallback != null) closedCallback.run();
                }
            });
        } catch (Exception e) {
            if (closedCallback != null) closedCallback.run();
        }
    }

    @Override
    public void showRewardedAd(final RewardedVideoCloseCallback finishedCallback) {
        try {
            runOnUiThread(() -> {
                rewardedAdFinishedCallback = finishedCallback;
                rewardEarned = false;
                if (network == AdsConfig.Network.ADMOB && admobRewardedAd != null) {
                    admobRewardedAd.show(AdActivity.this, rewardItem -> rewardEarned = true);
                } else if (network == AdsConfig.Network.HUAWEI && huaweiRewardedAd != null && huaweiRewardedAd.isLoaded()) {
                    huaweiRewardedAd.show(AdActivity.this, new RewardAdStatusListener() {
                        @Override
                        public void onRewarded(Reward reward) {
                            rewardEarned = true;
                        }

                        @Override
                        public void onRewardAdClosed() {
                            huaweiRewardedAd = null;
                            finishRewarded(rewardEarned);
                        }

                        @Override
                        public void onRewardAdFailedToShow(int errorCode) {
                            huaweiRewardedAd = null;
                            finishRewarded(false);
                        }
                    });
                } else if (network == AdsConfig.Network.YANDEX && yandexRewardedAd != null) {
                    yandexRewardedAd.show(AdActivity.this);
                } else {
                    android.widget.Toast.makeText(AdActivity.this, "No Ads Available", android.widget.Toast.LENGTH_SHORT).show();
                    if (finishedCallback != null) finishedCallback.closed(false);
                    initRewardedAds();
                }
            });
        } catch (Exception e) {
            System.out.println("showRewardedAd error: " + e);
        }
    }

    private void finishRewarded(boolean earned) {
        rewardedLoading = false;
        initRewardedAds();
        if (rewardedAdFinishedCallback != null) rewardedAdFinishedCallback.closed(earned);
    }

    private void finishInterstitial() {
        interstitialLoading = false;
        loadInterstitialAds();
        if (interstitialClosedCallback != null) interstitialClosedCallback.run();
    }

    @Override
    public int getIntervalBetweenRewardedAds() {
        return getResources().getInteger(R.integer.ADMOB_INTERVAL_BETWEEN_REWARDED_ADS_IN_SECONDS);
    }

    @Override public void openGDPRForm() {}
    @Override public boolean isUserInEU() { return false; }

    @Override
    protected void onDestroy() {
        if (yandexRewardedAd != null) {
            yandexRewardedAd.setAdEventListener(null);
            yandexRewardedAd = null;
        }
        if (yandexInterstitialAd != null) {
            yandexInterstitialAd.setAdEventListener(null);
            yandexInterstitialAd = null;
        }
        if (yandexBanner != null) {
            yandexBanner.destroy();
            yandexBanner = null;
        }
        if (admobBanner != null) {
            admobBanner.destroy();
            admobBanner = null;
        }
        if (huaweiBanner != null) {
            huaweiBanner.destroy();
            huaweiBanner = null;
        }
        admobRewardedAd = null;
        admobInterstitialAd = null;
        huaweiRewardedAd = null;
        huaweiInterstitialAd = null;
        super.onDestroy();
    }
}
