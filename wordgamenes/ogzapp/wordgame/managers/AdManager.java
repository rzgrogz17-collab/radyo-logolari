package ogzapp.wordgame.managers;

import ogzapp.wordgame.util.RewardedVideoCloseCallback;

public interface AdManager {
    boolean isInterstitialAdEnabled();
    boolean isRewardedAdEnabledToEarnCoins();
    boolean isRewardedAdEnabledToEarnMoves();
    boolean isRewardedAdEnabledToSpinWheel();
    boolean isRewardedAdLoaded();
    boolean isInterstitialAdLoaded();
    void showInterstitialAd(Runnable closedCallback);
    void showRewardedAd(RewardedVideoCloseCallback finishedCallback);
    int getIntervalBetweenRewardedAds();
    /** Alttaki banner'ın piksel yüksekliği. Banner yoksa 0. */
    int getBannerHeightPixels();
    void openGDPRForm();
    boolean isUserInEU();
}