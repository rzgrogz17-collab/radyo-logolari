package com.company.game;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.company.R;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.InitializationListener;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

/**
 * Same placements as AdMob: banner, interstitial and rewarded.
 * Used only when use_admob is false.
 */
public class UtilsYandex {
    private static final String TAG = "YandexAds";

    private final MainActivity activity;
    private boolean enableBanner;
    private boolean enableInter;
    private boolean enableReward;
    private boolean bannerAtBottom = true;
    private boolean bannerNotOverlap = true;

    private BannerAdView bannerAd;
    private InterstitialAdLoader interstitialLoader;
    private InterstitialAd interstitialAd;
    private RewardedAdLoader rewardedLoader;
    private RewardedAd rewardedAd;
    private boolean rewardGranted = false;

    public UtilsYandex(MainActivity activity) {
        this.activity = activity;
    }

    public void start(boolean enableBanner, boolean enableInter, boolean enableReward,
                      boolean bannerAtBottom, boolean bannerNotOverlap, boolean testing) {
        this.enableBanner = enableBanner;
        this.enableInter = enableInter;
        this.enableReward = enableReward;
        this.bannerAtBottom = bannerAtBottom;
        this.bannerNotOverlap = bannerNotOverlap;

        if (!enableBanner) hideSlot();
        if (!enableBanner && !enableInter && !enableReward) return;

        boolean consent = true;
        if (activity.getResources().getBoolean(R.bool.enable_gdpr)) {
            String vendor = PreferenceManager.getDefaultSharedPreferences(activity)
                    .getString("IABTCF_VendorConsents", "");
            consent = vendor != null && vendor.indexOf('1') >= 0;
        }
        MobileAds.setUserConsent(consent);
        MobileAds.setAgeRestrictedUser(activity.getResources().getBoolean(R.bool.under_age));
        if (testing) MobileAds.enableLogging(true);

        MobileAds.initialize(activity, new InitializationListener() {
            @Override
            public void onInitializationCompleted() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (UtilsYandex.this.enableBanner) prepareBanner();
                        if (UtilsYandex.this.enableInter) prepareInter();
                        if (UtilsYandex.this.enableReward) prepareReward();
                    }
                });
            }
        });
    }

    public void show_banner(Boolean visible) {
        final int visibility = Boolean.TRUE.equals(visible) ? View.VISIBLE : View.GONE;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FrameLayout slot = activity.findViewById(R.id.ad_container);
                if (slot != null) slot.setVisibility(visibility);
            }
        });
    }

    public void show_inter() {
        if (!enableInter || interstitialAd == null) {
            Log.d(TAG, "Interstitial not ready");
            return;
        }
        interstitialAd.setAdEventListener(new InterstitialAdEventListener() {
            @Override
            public void onAdShown() { }

            @Override
            public void onAdFailedToShow(@NonNull AdError adError) {
                Log.d(TAG, "Interstitial failed to show: " + adError.getDescription());
                clearInterstitial();
                prepareInter();
            }

            @Override
            public void onAdDismissed() {
                clearInterstitial();
                prepareInter();
            }

            @Override
            public void onAdClicked() { }

            @Override
            public void onAdImpression(@Nullable ImpressionData impressionData) { }
        });
        interstitialAd.show(activity);
    }

    public void show_reward() {
        if (!enableReward || rewardedAd == null) {
            Log.d(TAG, "Rewarded not ready");
            activity.reward("no");
            return;
        }
        rewardGranted = false;
        rewardedAd.setAdEventListener(new RewardedAdEventListener() {
            @Override
            public void onAdShown() { }

            @Override
            public void onAdFailedToShow(@NonNull AdError adError) {
                Log.d(TAG, "Rewarded failed to show: " + adError.getDescription());
                rewardGranted = false;
                clearRewarded();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.reward("no");
                    }
                });
                prepareReward();
            }

            @Override
            public void onAdDismissed() {
                boolean granted = rewardGranted;
                rewardGranted = false;
                clearRewarded();
                if (!granted) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.reward("no");
                        }
                    });
                }
                prepareReward();
            }

            @Override
            public void onAdClicked() { }

            @Override
            public void onAdImpression(@Nullable ImpressionData impressionData) { }

            @Override
            public void onRewarded(@NonNull com.yandex.mobile.ads.rewarded.Reward reward) {
                rewardGranted = true;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.reward("yes");
                    }
                });
            }
        });
        rewardedAd.show(activity);
    }

    public void destroy() {
        if (bannerAd != null) {
            bannerAd.setBannerAdEventListener(null);
            bannerAd.destroy();
            bannerAd = null;
        }
        if (interstitialLoader != null) {
            interstitialLoader.setAdLoadListener(null);
            interstitialLoader = null;
        }
        clearInterstitial();
        if (rewardedLoader != null) {
            rewardedLoader.setAdLoadListener(null);
            rewardedLoader = null;
        }
        clearRewarded();
    }

    private void hideSlot() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FrameLayout slot = activity.findViewById(R.id.ad_container);
                if (slot != null) slot.setVisibility(View.GONE);
            }
        });
    }

    private void prepareBanner() {
        final FrameLayout slot = activity.findViewById(R.id.ad_container);
        if (slot == null) return;

        if (!bannerAtBottom) {
            LinearLayout main = activity.findViewById(R.id.main);
            if (main != null) {
                main.removeView(slot);
                main.addView(slot, 0);
            }
        }
        if (!bannerNotOverlap) {
            ViewGroup.LayoutParams raw = slot.getLayoutParams();
            if (raw instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) raw).setMargins(0, -140, 0, 0);
            }
        }

        if (bannerAd != null) {
            bannerAd.destroy();
            bannerAd = null;
        }
        bannerAd = new BannerAdView(activity);
        slot.removeAllViews();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL);
        slot.addView(bannerAd, lp);
        slot.setVisibility(View.GONE);

        slot.post(new Runnable() {
            @Override
            public void run() {
                if (bannerAd == null) return;
                DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                int widthPx = slot.getWidth() > 0 ? slot.getWidth() : metrics.widthPixels;
                int widthDp = Math.max(1, Math.round(widthPx / metrics.density));
                bannerAd.setAdSize(BannerAdSize.stickySize(activity, widthDp));
                bannerAd.setAdUnitId(activity.getString(R.string.yandex_banner_id));
                bannerAd.setBannerAdEventListener(new BannerAdEventListener() {
                    @Override
                    public void onAdLoaded() {
                        Log.d(TAG, "Banner loaded");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                slot.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                        Log.d(TAG, "Banner failed: " + adRequestError.getDescription());
                    }

                    @Override
                    public void onAdClicked() { }

                    @Override
                    public void onLeftApplication() { }

                    @Override
                    public void onReturnedToApplication() { }

                    @Override
                    public void onImpression(@Nullable ImpressionData impressionData) { }
                });
                bannerAd.loadAd(new AdRequest.Builder().build());
            }
        });
    }

    private void prepareInter() {
        if (!enableInter) return;
        if (interstitialLoader == null) {
            interstitialLoader = new InterstitialAdLoader(activity);
            interstitialLoader.setAdLoadListener(new InterstitialAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    interstitialAd = ad;
                    Log.d(TAG, "Interstitial loaded");
                }

                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                    interstitialAd = null;
                    Log.d(TAG, "Interstitial failed: " + adRequestError.getDescription());
                }
            });
        }
        interstitialLoader.loadAd(new AdRequestConfiguration.Builder(
                activity.getString(R.string.yandex_inter_id)).build());
    }

    private void prepareReward() {
        if (!enableReward) return;
        if (rewardedLoader == null) {
            rewardedLoader = new RewardedAdLoader(activity);
            rewardedLoader.setAdLoadListener(new RewardedAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    rewardedAd = ad;
                    Log.d(TAG, "Rewarded loaded");
                }

                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                    rewardedAd = null;
                    Log.d(TAG, "Rewarded failed: " + adRequestError.getDescription());
                }
            });
        }
        rewardedLoader.loadAd(new AdRequestConfiguration.Builder(
                activity.getString(R.string.yandex_reward_id)).build());
    }

    private void clearInterstitial() {
        if (interstitialAd != null) {
            interstitialAd.setAdEventListener(null);
            interstitialAd = null;
        }
    }

    private void clearRewarded() {
        if (rewardedAd != null) {
            rewardedAd.setAdEventListener(null);
            rewardedAd = null;
        }
    }
}
