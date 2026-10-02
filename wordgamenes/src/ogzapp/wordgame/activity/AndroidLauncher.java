package ogzapp.wordgame.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ogzapp.wordgame.DateUtilImpl;
import ogzapp.wordgame.NetworkAndroid;
import ogzapp.wordgame.R;
import ogzapp.wordgame.WordConnectGame;
import ogzapp.wordgame.WordMeaningProviderAndroid;
import ogzapp.wordgame.net.WordMeaningProvider;
import ogzapp.wordgame.util.AppExit;
import ogzapp.wordgame.util.RateUsLauncher;
import ogzapp.wordgame.util.SupportRequest;

public class AndroidLauncher extends AdActivity implements AppExit, RateUsLauncher, SupportRequest {

    private WordConnectGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ❌ KALDIRILDI - Bu satırı SİLİN (AdActivity'de zaten var)
        // com.google.android.gms.ads.MobileAds.initialize(this, initializationStatus -> {});

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = getResources().getBoolean(R.bool.IMMERSIVE_MODE);

        Map<String, WordMeaningProvider> provider = new HashMap<>();
        provider.put("en", new WordMeaningProviderAndroid());

        DateUtilImpl dateUtil = new DateUtilImpl();
        dateUtil.context = this;

        game = new WordConnectGame(new NetworkAndroid(this), provider);
        game.dateUtil = dateUtil;
        game.adManager = this;
        game.appExit = this;
        game.rateUsLauncher = this;
        game.supportRequest = this;

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            game.version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // ✅ SADECE BU SATIR DEĞİŞTİ - DİĞER HER ŞEY AYNI
        View gdxView = initializeForView(game, config);

        // ✅ Layout oluştur
        RelativeLayout layout = new RelativeLayout(this);
        layout.addView(gdxView);
        setContentView(layout);
        attachAdLayout(layout, gdxView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // Rewarded'ı hemen başlat - en yavaş yüklenen reklam türü
        initRewardedAds();
        loadInterstitialAds();

        // KULLANICI İSTEĞİYLE: Banner reklam tamamen kaldırıldı
        // (initBannerAds() artık yok/çağrılmıyor). Geçiş ve ödüllü reklamlar
        // için yükleme 500ms gecikmeyle tekrar deneniyor (ilk deneme çok
        // erken başarısız olabildiği için).
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                initRewardedAds();
                loadInterstitialAds();
            }
        }, 500);
    }

    @Override
    public void exitApp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onBackPressed();
            }
        });
    }

    @Override
    public void launch() {
        Uri uri = Uri.parse(getString(R.string.uygulamaLinki));
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.uygulamaLinki))));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.uygulamaLinki))));
        }
    }

    @Override
    public void sendSupportEmail() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.email)});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request for " + getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT, getDeviceInfo());
        startActivity(Intent.createChooser(intent, "Send e-mail..."));
    }

    public String getDeviceInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("Please type your request above");
        sb.append("\n");
        sb.append("Brand: ");
        sb.append(Build.BRAND);
        sb.append("\n");
        sb.append("Model: ");
        sb.append(Build.MODEL);
        sb.append("\n");
        sb.append("SDK: ");
        sb.append(Build.VERSION.SDK_INT);
        sb.append("\n");

        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = Resources.getSystem().getConfiguration().getLocales().get(0);
        } else {
            locale = Resources.getSystem().getConfiguration().locale;
        }

        sb.append("Locale: ");
        sb.append(locale.getLanguage() + "-" + locale.getCountry());
        sb.append("\n");
        sb.append("App version: ");

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            sb.append(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
