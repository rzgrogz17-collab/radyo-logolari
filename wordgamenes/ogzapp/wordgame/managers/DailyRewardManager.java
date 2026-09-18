package ogzapp.wordgame.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.TimeUtils;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.model.Constants;

// Günlük giriş ödülü (7 günlük seri) için basit bir yönetici sınıf.
// HintManager'daki Preferences deseniyle aynı mantığı kullanır.
public class DailyRewardManager {

    private static final long MILLIS_IN_A_DAY = 86400000L;

    // Son ödül talebinden bu yana 24 saatten fazla geçtiyse yeni ödül alınabilir.
    public static boolean isAvailable() {
        if (!GameConfig.DAILY_REWARD_ENABLED) return false;

        long lastClaimTime = getLastClaimTime();
        if (lastClaimTime == 0) return true;

        return TimeUtils.timeSinceMillis(lastClaimTime) > MILLIS_IN_A_DAY;
    }

    public static long getLastClaimTime() {
        Preferences preferences = Gdx.app.getPreferences(Constants.PREFS_NAME);
        return preferences.getLong(Constants.KEY_DAILY_REWARD_LAST_CLAIM_TIME, 0);
    }

    // 1..DAILY_REWARD_COINS.length arasında, oyuncunun bugün talep edeceği
    // gün numarasını döndürür (henüz talep edilmedi). Son talepten bu yana
    // 2 günden fazla geçtiyse seri sıfırlanır ve 1. güne döner.
    public static int getUpcomingStreakDay() {
        Preferences preferences = Gdx.app.getPreferences(Constants.PREFS_NAME);
        long lastClaimTime = preferences.getLong(Constants.KEY_DAILY_REWARD_LAST_CLAIM_TIME, 0);
        int lastDay = preferences.getInteger(Constants.KEY_DAILY_REWARD_STREAK_DAY, 0);

        if (lastClaimTime == 0) return 1;

        long elapsed = TimeUtils.timeSinceMillis(lastClaimTime);
        boolean streakBroken = elapsed > MILLIS_IN_A_DAY * 2;

        if (streakBroken) return 1;

        int totalDays = GameConfig.DAILY_REWARD_COINS.length;
        return (lastDay % totalDays) + 1;
    }

    public static int getCoinsForDay(int day) {
        int index = Math.max(1, Math.min(day, GameConfig.DAILY_REWARD_COINS.length)) - 1;
        return GameConfig.DAILY_REWARD_COINS[index];
    }

    // Bugünün ödülünü talep eder: coin bakiyesini günceller ve seriyi ilerletir.
    public static int claim() {
        int day = getUpcomingStreakDay();
        int coins = getCoinsForDay(day);

        HintManager.setCoinCount(HintManager.getRemainingCoins() + coins);

        Preferences preferences = Gdx.app.getPreferences(Constants.PREFS_NAME);
        preferences.putLong(Constants.KEY_DAILY_REWARD_LAST_CLAIM_TIME, TimeUtils.millis());
        preferences.putInteger(Constants.KEY_DAILY_REWARD_STREAK_DAY, day);
        preferences.flush();

        return coins;
    }
}
