package ogzapp.wordgame.activity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.badlogic.gdx.backends.android.AndroidApplication;

import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

import ogzapp.wordgame.R;
import ogzapp.wordgame.managers.AdManager;
import ogzapp.wordgame.util.RewardedVideoCloseCallback;

// KULLANICI İSTEĞİYLE: Banner reklam TAMAMEN kaldırıldı (kod, alan, import
// ve kaynak (unit id) dahil). Geçiş (interstitial) ve ödüllü (rewarded)
// reklamlar AYNEN korunuyor - sadece banner'a dair hiçbir şey kalmadı.
public class AdActivity extends AndroidApplication implements AdManager {

    private static final String INTERSTITIAL_UNIT_ID = "R-M-19388209-2";
    private static final String REWARDED_UNIT_ID     = "R-M-19388209-3";

    private InterstitialAd yandexInterstitialAd;
    private RewardedAd     yandexRewardedAd;

    private RewardedVideoCloseCallback rewardedAdFinishedCallback;
    private Runnable                   interstitialClosedCallback;
    private boolean                    rewardEarned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MobileAds.initialize(this, () ->
                System.out.println("Yandex MobileAds initialized"));
    }

    // ── REWARDED ──────────────────────────────────────────────────────────────
    public void initRewardedAds() {
        runOnUiThread(() -> {
            RewardedAdLoader loader = new RewardedAdLoader(this);
            loader.setAdLoadListener(new RewardedAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd ad) {
                    yandexRewardedAd = ad;
                    yandexRewardedAd.setAdEventListener(new RewardedAdEventListener() {
                        @Override public void onAdShown() {}
                        @Override public void onAdClicked() {}
                        @Override public void onAdImpression(@Nullable ImpressionData data) {}

                        @Override
                        public void onAdFailedToShow(@NonNull AdError error) {
                            yandexRewardedAd = null;
                            initRewardedAds();
                            if (rewardedAdFinishedCallback != null)
                                rewardedAdFinishedCallback.closed(false);
                        }

                        @Override
                        public void onAdDismissed() {
                            yandexRewardedAd = null;
                            initRewardedAds();
                            if (rewardedAdFinishedCallback != null)
                                rewardedAdFinishedCallback.closed(rewardEarned);
                        }

                        @Override
                        public void onRewarded(@NonNull Reward reward) {
                            rewardEarned = true;
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError error) {
                    yandexRewardedAd = null;
                }
            });
            loader.loadAd(new AdRequestConfiguration.Builder(REWARDED_UNIT_ID).build());
        });
    }

    // ── INTERSTITIAL ──────────────────────────────────────────────────────────
    public void loadInterstitialAds() {
        runOnUiThread(() -> {
            InterstitialAdLoader loader = new InterstitialAdLoader(this);
            loader.setAdLoadListener(new InterstitialAdLoadListener() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    yandexInterstitialAd = ad;
                    yandexInterstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                        @Override public void onAdShown() {}
                        @Override public void onAdClicked() {}
                        @Override public void onAdImpression(@Nullable ImpressionData data) {}

                        @Override
                        public void onAdFailedToShow(@NonNull AdError error) {
                            yandexInterstitialAd = null;
                            loadInterstitialAds();
                            if (interstitialClosedCallback != null)
                                interstitialClosedCallback.run();
                        }

                        @Override
                        public void onAdDismissed() {
                            yandexInterstitialAd = null;
                            loadInterstitialAds();
                            if (interstitialClosedCallback != null)
                                interstitialClosedCallback.run();
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull AdRequestError error) {
                    yandexInterstitialAd = null;
                }
            });
            loader.loadAd(new AdRequestConfiguration.Builder(INTERSTITIAL_UNIT_ID).build());
        });
    }

    // ── AdManager ─────────────────────────────────────────────────────────────
    @Override public boolean isInterstitialAdEnabled()        { return true; }
    @Override public boolean isRewardedAdEnabledToEarnCoins() { return true; }
    @Override public boolean isRewardedAdEnabledToEarnMoves() { return true; }
    @Override public boolean isRewardedAdEnabledToSpinWheel() { return true; }
    @Override public boolean isRewardedAdLoaded()             { return yandexRewardedAd != null; }
    @Override public boolean isInterstitialAdLoaded()         { return yandexInterstitialAd != null; }

    @Override
    public void showInterstitialAd(final Runnable closedCallback) {
        try {
            runOnUiThread(() -> {
                interstitialClosedCallback = closedCallback;
                if (yandexInterstitialAd != null) {
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
                if (yandexRewardedAd != null) {
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

    @Override
    public int getIntervalBetweenRewardedAds() {
        return getResources().getInteger(R.integer.ADMOB_INTERVAL_BETWEEN_REWARDED_ADS_IN_SECONDS);
    }

    @Override public void openGDPRForm() {}
    @Override public boolean isUserInEU() { return false; }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (yandexRewardedAd != null) {
            yandexRewardedAd.setAdEventListener(null);
            yandexRewardedAd = null;
        }
        if (yandexInterstitialAd != null) {
            yandexInterstitialAd.setAdEventListener(null);
            yandexInterstitialAd = null;
        }
    }
}
