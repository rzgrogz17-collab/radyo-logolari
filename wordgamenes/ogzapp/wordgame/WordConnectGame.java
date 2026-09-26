package ogzapp.wordgame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.Preferences;

import java.util.Map;

import ogzapp.wordgame.managers.AdManager;
import ogzapp.wordgame.managers.AdManagerImpl;
import ogzapp.wordgame.managers.ConnectionManager;
import ogzapp.wordgame.managers.HintManager;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.net.Network;
import ogzapp.wordgame.net.WordMeaningProvider;
import ogzapp.wordgame.screens.BaseScreen;
import ogzapp.wordgame.screens.SplashScreen;
import ogzapp.wordgame.ui.calendar.DateUtil;
import ogzapp.wordgame.ui.dialogs.iap.ShoppingProcessor;
import ogzapp.wordgame.util.AppExit;
import ogzapp.wordgame.util.RateUsLauncher;
import ogzapp.wordgame.util.SupportRequest;

public class WordConnectGame extends Game {

    public ShoppingProcessor shoppingProcessor;
    private BaseScreen currentScreen;
    public ResourceManager resourceManager = new ResourceManager();
    public DateUtil dateUtil;
    public String version;
    public AdManager adManager;
    public AppExit appExit;
    public RateUsLauncher rateUsLauncher;
    public SupportRequest supportRequest;
    public Music fonMuzigi;

    public WordConnectGame(Network network, Map<String, WordMeaningProvider> providerMap) {
        ConnectionManager.network = network;
        LanguageManager.wordMeaningProviderMap = providerMap;
    }

    public void notificationReceived(int coins, String title, String text) {
        int current = HintManager.getRemainingCoins();
        int newAmount = current + coins;
        HintManager.setCoinCount(newAmount);
        if (currentScreen != null) currentScreen.notificationReceived(newAmount, title, text);
    }

    @Override
    public void create() {
        fonMuzigi = Gdx.audio.newMusic(Gdx.files.internal("sfx/background.mp3"));
        fonMuzigi.setLooping(true);
        Preferences prefs = Gdx.app.getPreferences("MyPreferences");
        boolean musicOn = prefs.getBoolean("music_on", true);
        if (musicOn) fonMuzigi.play();
        fonMuzigi.setVolume(0.5f);

        if (adManager == null) {
            adManager = new AdManagerImpl(this, false);
        }

        setScreen(new SplashScreen(this));
    }

    public void updateMusicState() {
        Preferences prefs = Gdx.app.getPreferences("MyPreferences");
        boolean musicOn = prefs.getBoolean("music_on", true);
        if (fonMuzigi != null) {
            if (musicOn && !fonMuzigi.isPlaying()) fonMuzigi.play();
            else if (!musicOn && fonMuzigi.isPlaying()) fonMuzigi.pause();
        }
    }

    @Override
    public void setScreen(Screen screen) {
        if (currentScreen != null) currentScreen.dispose();
        currentScreen = (BaseScreen) screen;
        super.setScreen(screen);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (currentScreen != null) currentScreen.dispose();
        resourceManager.clear();
        resourceManager.dispose();
        if (fonMuzigi != null) fonMuzigi.dispose();
    }

    int yukseklik;

    public int getYukseklik() {
        return yukseklik;
    }

    public void setYukseklik(int yukseklik) {
        this.yukseklik = yukseklik;
    }
}