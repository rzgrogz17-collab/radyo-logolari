package ogzapp.wordgame.activity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import ogzapp.wordgame.config.GameConfig;

/**
 * Birim kimlikleri burada. Aç/kapa anahtarları GameConfig içindedir.
 */
public final class AdsConfig {

    public static final boolean ADMOB_ENABLED = GameConfig.ADMOB_ENABLED;
    public static final boolean YANDEX_ADS_ENABLED = GameConfig.YANDEX_ADS_ENABLED;

    public static final String[] YANDEX_COUNTRIES = GameConfig.YANDEX_AD_COUNTRIES;

    public static final String YANDEX_INTERSTITIAL_ID = "R-M-19388209-2";
    public static final String YANDEX_REWARDED_ID = "R-M-19388209-3";
    /** Gerçek Yandex banner birim kimliği. */
    public static final String YANDEX_BANNER_ID = "R-M-19388209-1";

    /** Manifest'teki com.google.android.gms.ads.APPLICATION_ID ile aynı olmalı. */
    public static final String ADMOB_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";
    public static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String ADMOB_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";

    /** Huawei test kimlikleri. Yayında kendi Petal birim kimliklerinle değiştir. */
    public static final String HUAWEI_REWARDED_ID = "testx9dtjwj8hp";
    public static final String HUAWEI_INTERSTITIAL_ID = "testb4znbuh3n2";
    public static final String HUAWEI_BANNER_ID = "testw6vs28auh3";

    public enum Network {
        ADMOB, YANDEX, HUAWEI
    }

    private static final Set<String> YANDEX_SET = new HashSet<String>(Arrays.asList(YANDEX_COUNTRIES));

    private AdsConfig() {
    }

    public static Network resolve(String countryCode) {
        if (ADMOB_ENABLED) return Network.ADMOB;
        String code = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.US);
        if (YANDEX_ADS_ENABLED && YANDEX_SET.contains(code)) return Network.YANDEX;
        return Network.HUAWEI;
    }

    public static boolean isBannerEnabled(Network network) {
        if (network == Network.ADMOB) return GameConfig.ADMOB_BANNER_ENABLED;
        if (network == Network.HUAWEI) return GameConfig.HUAWEI_BANNER_ENABLED;
        return GameConfig.YANDEX_BANNER_ENABLED;
    }

    public static boolean isInterstitialEnabled(Network network) {
        if (network == Network.ADMOB) return GameConfig.ADMOB_INTERSTITIAL_ENABLED;
        if (network == Network.HUAWEI) return GameConfig.HUAWEI_INTERSTITIAL_ENABLED;
        return GameConfig.YANDEX_INTERSTITIAL_ENABLED;
    }
}
