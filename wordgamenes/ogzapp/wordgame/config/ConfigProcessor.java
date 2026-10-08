package ogzapp.wordgame.config;

import com.badlogic.gdx.graphics.Color;

import ogzapp.wordgame.WordConnectGame;
import ogzapp.wordgame.ui.calendar.Date;

public class ConfigProcessor {


    /**
     * There is no configuration option in this file. It serves to make calculations for GameConfig.java and UIConfig.java.
     */


    public static boolean muted;



    public static Color getLevelColor(int levelIndex){
        if (UIConfig.levelColors == null || UIConfig.levelColors.length == 0) {
            return new Color(Color.WHITE);
        }

        // Dial, önizleme ve bulunan kelime kutuları 10 seviye boyunca aynı
        // rengi kullanır. 10. seviye ödülünden sonra (index 10, 20, 30…)
        // bir sonraki levelColors değerine geçilir.
        int cycle = Math.max(0, levelIndex) / 10;
        int index = cycle % UIConfig.levelColors.length;
        Color src = UIConfig.levelColors[index];
        Color copy = new Color(src);
        copy.a = 1f;
        return copy;
    }





    public static boolean isMenuEnabled(boolean gdpr, boolean hasManyLocale){
        return findTotalEnabledMenuRows(gdpr, hasManyLocale) > 0;
    }



    public static int findTotalEnabledMenuRows(boolean gdpr, boolean hasManyLocale){
        int enabledItems = 0;

        if(UIConfig.MENU_ITEM_GDPR_ENABLED && gdpr) enabledItems++;
        if(UIConfig.MENU_ITEM_RATE_US_ENABLED) enabledItems++;
        if(UIConfig.MENU_ITEM_CONTACT_US_ENABLED) enabledItems++;
        if(UIConfig.MENU_ITEM_LANGUAGE_ENABLED && hasManyLocale) enabledItems++;
        if(UIConfig.MENU_ITEM_SOUND_ENABLED) enabledItems++;

        return enabledItems;
    }



    public static boolean isHallowenDay(Date date){
        return date.getMonth() == 9 && date.getDate() == 31;
    }



    public static boolean isThanksGivingDay(WordConnectGame wordConnectGame, Date date){
        Date thanksGiving = wordConnectGame.dateUtil.newDate(date.getYear(), 10, 1);
        int dayOfWeek = thanksGiving.getDay() - 1;//java.util.Calendar is 1-based
        int thanksGivingDate = 22 + (11 - dayOfWeek) % 7;
        return date.getMonth() == 10 && date.getDate() == thanksGivingDate;
    }




    public static boolean isChristmasHoliday(WordConnectGame wordConnectGame, Date date){
        Date start = null;
        Date end = null;

        if(date.getMonth() == 11){
            start = wordConnectGame.dateUtil.newDate(date.getYear(), 11, 24);
            end   = wordConnectGame.dateUtil.newDate(date.getYear() + 1, 0, 6);
        }else if(date.getMonth() == 0){
            start = wordConnectGame.dateUtil.newDate(date.getYear() - 1, 11, 24);
            end   = wordConnectGame.dateUtil.newDate(date.getYear(), 0, 5);
        }

        if(start == null || end == null)return false;

        return date.after(start) && date.before(end);
    }

}
