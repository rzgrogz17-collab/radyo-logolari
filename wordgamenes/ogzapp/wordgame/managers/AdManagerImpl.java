package ogzapp.wordgame.managers;

import com.badlogic.gdx.Gdx;
import ogzapp.wordgame.WordConnectGame;
import ogzapp.wordgame.util.RewardedVideoCloseCallback;

public class AdManagerImpl implements AdManager {

    private WordConnectGame game;
    private boolean isInEU;

    public AdManagerImpl(WordConnectGame game, boolean isInEU) {
        this.game = game;
        this.isInEU = isInEU;
    }

    @Override
    public boolean isInterstitialAdEnabled() {
        return true;
    }

    @Override
    public boolean isRewardedAdEnabledToEarnCoins() {
        return true;
    }

    @Override
    public boolean isRewardedAdEnabledToEarnMoves() {
        return true;
    }

    @Override
    public boolean isRewardedAdEnabledToSpinWheel() {
        return true;
    }

    @Override
    public boolean isRewardedAdLoaded() {
        // Gerçek reklam kontrolü yapılmalı, örnek olarak true döndürüyoruz
        return true;
    }

    @Override
    public boolean isInterstitialAdLoaded() {
        return true;
    }

    @Override
    public void showInterstitialAd(Runnable closedCallback) {
        // Müziği durdur
        if (game.fonMuzigi != null && game.fonMuzigi.isPlaying()) {
            game.fonMuzigi.pause();
        }

        // Reklam gösterimi (platforma özel kod buraya gelecek)

        // Reklam kapandığında:
        if (closedCallback != null) {
            closedCallback.run();
        }
        // Müziği geri başlat
        game.updateMusicState();
    }

    @Override
    public void showRewardedAd(RewardedVideoCloseCallback finishedCallback) {
        // Müziği durdur
        if (game.fonMuzigi != null && game.fonMuzigi.isPlaying()) {
            game.fonMuzigi.pause();
        }

        // Reklam kapandığında:
        // Reklam gösterimi

        // Reklam kapandığında:
        if (finishedCallback != null) {
            finishedCallback.closed(true); // veya false, duruma göre
        }
        game.updateMusicState();
    }

    @Override
    public int getIntervalBetweenRewardedAds() {
        return 60;
    }

    @Override
    public void openGDPRForm() {
        Gdx.net.openURI("https://wordconnectbrainfungame.blogspot.com/");
    }

    @Override
    public boolean isUserInEU() {
        return isInEU;
    }
}