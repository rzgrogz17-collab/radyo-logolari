package ogzapp.wordgame.config;

import com.badlogic.gdx.graphics.Color;

import ogzapp.wordgame.WordConnectGame;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.ui.calendar.Date;

public class UIConfig {

    /**
     * UI AYARLARI
     */

/**************************************************************************************************************************************************************************/

    /**
     * Preloader (Yükleme Çubuğu)
     */
    public static final Color LOADING_BAR_COLOR = new Color(0xDFF2FFff);

/**************************************************************************************************************************************************************************/

    /**
     * Arkaplan Renkleri
     */
    public static final Color INTRO_SCREEN_BACKGROUND_COLOR = new Color(0xFEFCFFFF);
    public static final Color GAME_SCREEN_BACKGROUND_COLOR = new Color(0x000000FF);


    // Giriş Ekranı Resmi Seçici
    public static String getIntroScreenBackgroundImage(WordConnectGame wordConnectGame){
        Date date = wordConnectGame.dateUtil.newDate();
        String langCode = LanguageManager.getSelectedLocaleCode();

        if(langCode == null || langCode.equals("en")){
            if(ConfigProcessor.isHallowenDay(date)){
                return "background/intro/halloween.jpg";
            }else if(ConfigProcessor.isThanksGivingDay(wordConnectGame, date)){
                return "background/intro/thanksgiving.jpg";
            }
        }

        if(ConfigProcessor.isChristmasHoliday(wordConnectGame, date)){
            return "background/intro/newyear.jpg";
        }

        return "background/intro/intro.jpg";
    }


    // Oyun İçi Arkaplan Resmi
    public static String getGameScreenBackgroundImage(int levelIndex){
        int total = 70;
        int num = levelIndex / 5;
        num %= total;

        return "background/game/game_" + num + ".jpg";
    }


    /**************************************************************************************************************************************************************************/
    // Öğretici (Tutorial)
    public static final boolean INTERACTIVE_TUTORIAL_ENABLED                    = false;
    public static final Color INTERACTIVE_TUTORIAL_TEXT_BG_COLOR                = new Color(0x008080ff);
    public static final String INTERACTIVE_TUTORIAL_TEXT_COLOR                  = "[#ffffff]";
    public static final boolean INTERACTIVE_TUTORIAL_TEXT_USE_SHADOW_FONT       = true;
    public static final String INTERACTIVE_TUTORIAL_DASHED_DIAL_WORD_COLOR      = "[#ffff00]";
    public static final Color INTERACTIVE_TUTORIAL_GOT_IT_BACKGROUND_COLOR      = new Color(0x008080ff);
    public static final Color INTERACTIVE_TUTORIAL_GOT_IT_TEXT_COLOR            = Color.WHITE;
    public static final boolean INTERACTIVE_TUTORIAL_GOT_IT_USE_SHADOW_FONT     = false;

    /**************************************************************************************************************************************************************************/
    // Üst Panel (Altın, Seviye vb.)
    public static final Color REMAINING_COINS_TEXT_COLOR            = Color.WHITE;
    public static final boolean REMAINING_COINS_USE_SHADOW_FONT     = false;
    public static final Color LEVEL_NUMBER_TEXT_COLOR               = Color.WHITE;
    public static final boolean LEVEL_NUMBER_TEXT_USE_SHADOW_FONT   = true;
    public static final Color COMBO_REWARD_TEXT_COLOR               = Color.WHITE;
    public static final boolean COMBO_REWARD_TEXT_USE_SHADOW_FONT   = true;


    /**************************************************************************************************************************************************************************/
    // OYNA Butonu (Giriş Ekranı)
    public static final Color INTRO_PLAY_BUTTON_TEXT_COLOR          = new Color(0x16324FFF);
    public static final Color INTRO_PLAY_BUTTON_BG_COLOR             = new Color(0xF7F9FCFF);
    public static final Color INTRO_PLAY_BUTTON_BG_DOWN_COLOR        = new Color(0xE2E7EEFF);
    public static final boolean INTRO_PLAY_BUTTON_USE_SHADOW_FONT   = true;

    public static final float INTRO_PLAY_BUTTON_WIDTH_COEF          = 2.7f; // Daraltılmış hali
    public static final float INTRO_PLAY_BUTTON_FONT_SCALE          = 1.0f; // Küçültülmüş hali

    public static final boolean INTRO_PLAY_BUTTON_PULSATE           = true;

    /**************************************************************************************************************************************************************************/

    // Genel Pencere Ayarları (Dialogs)
    public static final float DIALOG_OPEN_DURATION                      = 0.4f;
    public static final float DIALOG_CLOSE_DURATION                     = 0.6f;
    public static final Color DIALOG_MODAL_BACKGROUND_COLOR             = new Color(0, 0, 0, 0.6f);
    public static final Color DIALOG_TITLE_TEXT_COLOR                   = Color.WHITE;
    public static final boolean DIALOG_TITLE_TEXT_USE_SHADOW_FONT       = true;
    public static final float DIALOG_TITLE_FONT_SCALE                   = 1.0f;
    public static final Color DIALOG_TITLE_BACKROUND_COLOR              = new Color(0x008080ff); // Başlık gri kalabilir veya bunu da bordo yapabilirsin
    public static final Color DIALOG_BACKGROUND_COLOR                   = new Color(0xEEEEEEFF);
    public static final Color DIALOG_BODY_TEXT_COLOR                    = new Color(0xFFFAFAff);
    // NOT: Kullanıcı isteğiyle etkinleştirildi - buzlu cam panel şeffaflığı
    // arttıkça yazı okunurluğu düştüğü için, yazının hemen altında hafif bir
    // gölge (bu, Onayla/Jeton/Bonus Kelimeler gibi TÜM dialogların gövde
    // metnini kapsar) okunurluğu artırıyor.
    public static final boolean DIALOG_BODY_TEXT_USE_SHADOW_FONT        = true;
    public static final float DIALOG_BODY_TEXT_FONT_SCALE               = 0.8f;

    public static final float CONFIRM_DIALOG_BUTTON_WIDTH_COEF          = 0.38f; // Daraltılmış
    public static final float CONFIRM_DIALOG_BUTTON_FONT_SCALE          = 1.0f;
    public static final float CONFIRM_DIALOG_BUTTON_SCALE               = 0.80f; // Küçültülmüş

    public static final boolean CONFIRM_DIALOG_BUTTON_USE_SHADOW_FONT   = true;
    public static final float ALERT_DIALOG_BUTTON_WIDTH_COEF            = 0.7f;
    public static final float ALERT_DIALOG_BUTTON_FONT_SCALE            = 1f;
    public static final boolean ALERT_DIALOG_BUTTON_USE_SHADOW_FONT     = true;

/**************************************************************************************************************************************************************************/

    /**
     * MENÜ BUTONLARI (Ses, Dil, İletişim...)
     * Burası istediğin değişikliğin yapıldığı yer!
     */
    public static final boolean MENU_ITEM_GDPR_ENABLED                  = true;
    public static final boolean MENU_ITEM_RATE_US_ENABLED               = true;
    public static final boolean MENU_ITEM_CONTACT_US_ENABLED            = true;
    public static final boolean MENU_ITEM_LANGUAGE_ENABLED              = true;
    public static final boolean MENU_ITEM_SOUND_ENABLED                 = true;


    // --> RENK DEĞİŞTİ: Opak griden, referans görseldeki gibi yarı saydam
    // "buzlu cam" (frosted glass) beyaza çevrildi.
    public static final Color MENU_ITEM_BG_COLOR                        = new Color(0xFFFFFF40);

    // Referans görseldeki buzlu cam panel için: Menu ekranının kendi panel
    // arka planı, SADECE Menu'ye özel yarı saydam beyaz yapıldı. Diğer TÜM
    // dialoglar (satın alma, çark, sözlük, vb.) hâlâ DIALOG_BACKGROUND_COLOR
    // kullanmaya devam ediyor ve bu değişiklikten ETKİLENMİYOR.
    // --> GÜNCELLEME: Kullanıcı geri bildirimiyle şeffaflık ÖNEMLİ ÖLÇÜDE
    // azaltıldı (0x4D ≈ %30 opaklıktan 0xE8 ≈ %91 opaklığa) - panel artık
    // arkasındaki fotoğraf/harfler bariz şekilde görünmeyecek kadar belirgin,
    // yine de tamamen düz (opak) değil, hafif "buzlu cam" hissi koruyor.
    // NOT: Kullanıcı, mevcut halin HÂLÂ düz/opak (buzlu cam değil) göründüğünü
    // referans görselle karşılaştırarak gösterdi. 0xE8 (~%91 opaklık) gerçek
    // bir "buzlu cam" hissi için fazla yoğundu - arkadaki oyun ekranı neredeyse
    // hiç görünmüyordu. Alfa belirgin şekilde düşürüldü (0x80 ≈ %50) ki
    // arkadaki renkler/şekiller net şekilde süzülerek görünsün.
    public static final Color MENU_DIALOG_BACKGROUND_COLOR             = new Color(0xFFFFFF80);

    // Referans görselde "MENÜ" başlığının arkasında ayrı renkli bir kutu
    // YOK - yazı doğrudan camın üzerinde duruyor. Bu yüzden SADECE Menu'nün
    // başlık arka planı tamamen SAYDAM yapıldı. Diğer dialogların (teal
    // renkli) başlık kutusu bu değişiklikten ETKİLENMİYOR.
    public static final Color MENU_DIALOG_TITLE_BACKGROUND_COLOR       = new Color(1f, 1f, 1f, 0f);

    public static final Color MENU_ITEM_TEXT_COLOR                      = Color.WHITE;
    public static final boolean MENU_ITEM_USE_SHADOW_FONT               = true;

    // --> "Başarısız!" (bomba patladı) ve "Bomba patlamak üzere!" dialogları
    // için: Menü'deki AYNI buzlu cam (yarı saydam panel + görünmez başlık
    // kutusu) görünümü, ancak yazı rengi bu ikisinde KOYU (siyaha yakın,
    // "1. Seviye" pill butonuyla aynı lacivert) - çünkü açık/buzlu zemin
    // üzerinde beyaza yakın yazı (eski DIALOG_BODY_TEXT_COLOR) okunmuyordu.
    // Bu üç sabit SADECE bu iki dialogda kullanılıyor; diğer TÜM
    // AlertDialog çağrıları (mağaza, GDPR/genel uyarılar vb.) eskisi gibi
    // opak DIALOG_BACKGROUND_COLOR + beyaz yazıyla devam ediyor, ETKİLENMİYOR.
    public static final Color FROSTED_ALERT_DIALOG_BACKGROUND_COLOR       = MENU_DIALOG_BACKGROUND_COLOR;
    public static final Color FROSTED_ALERT_DIALOG_TITLE_BACKGROUND_COLOR = MENU_DIALOG_TITLE_BACKGROUND_COLOR;
    // Kullanıcı isteğiyle: "Başarısız!" ve "Bomba patlamak üzere!"
    // pencerelerindeki TÜM yazılar (başlık + gövde metni) artık
    // "Günlük Ödül" başlığıyla AYNI renk - DIALOG_TITLE_TEXT_COLOR (beyaz).
    // NOT: Bu iki pencerenin gövde metni, başlık çubuğunun daha koyu
    // zemini yerine AÇIK/buzlu-cam içerik panelinin (FROSTED_ALERT_DIALOG_
    // BACKGROUND_COLOR, ~%50 saydam beyaz) ÜZERİNDE duruyor - beyaz yazı
    // orada daha DÜŞÜK KONTRASTLA görünebilir. İstenirse panel zemini de
    // koyulaştırılabilir.
    public static final Color FROSTED_ALERT_DIALOG_TEXT_COLOR             = DIALOG_TITLE_TEXT_COLOR;
    // Kullanıcı isteğiyle: açık/buzlu-cam panel üzerindeki beyaz gövde
    // metninin okunabilirliği için, yazının arkasına hafif (yarı saydam)
    // siyah bir gölge kopyası eklendi (bkz. AlertDialog/BombDialog).
    public static final Color FROSTED_ALERT_DIALOG_TEXT_SHADOW_COLOR      = new Color(0x00000066);
    // --> RENK DEĞİŞTİ: Koyu griden, cam panelin üzerinde daha iyi okunan
    // yarı saydam beyaza çevrildi.
    public static final Color MENU_DIALOG_VERSION_TEXT_COLOR            = new Color(0xFFFFFFB0);
    public static final boolean MENU_DIALOG_VERSION_USE_SHADOW_FONT     = false;

    // --> BOYUT: Kullanıcı isteğiyle biraz daha uzun (1.4 -> 1.55) - üstten
    // alttan biraz daha ferah.
    public static final float MENU_ITEM_HEIGHT_COEF                     = 1.55f;

    // --> BOŞLUK AZALDI: Daha kompakt görünüm için (0.1 -> 0.05)
    public static final float MENU_ITEM_SPACING_COEF                    = 0.1f;


    /**************************************************************************************************************************************************************************/
    // Dil Seçimi
    public static final Color LANGUAGE_DIALOG_SELECTION_GLOW_COLOR      = new Color(0xD1D0CEff);

    /**************************************************************************************************************************************************************************/

    // Çarkıfelek (Lucky Wheel)
    public static final Color WHEEL_DIALOG_SPIN_BUTTON_TEXT_COLOR               = Color.WHITE;
    public static final float WHEEL_DIAL_SPIN_BUTTON_WIDTH_COEF                 = 0.9f;
    public static final boolean WHEEL_DIALOG_SPIN_BUTTON_USE_SHADOW_FONT        = true;
    public static final float WHEEL_DIALOG_SPIN_BUTTON_FONT_SCALE               = 0.9f;

    public static final Color WHEEL_DIALOG_BACKGROUND_COLOR                     = new Color(0xFEFCFFff);
    public static final Color WHEEL_DIALOG_RAYS_COLOR                           = new Color(0xE5E4E2ff);

    public static final Color WHEEL_DIALOG_ITEM_QUANTITY_TEXT_COLOR_DARK        = Color.WHITE;
    public static final Color WHEEL_DIALOG_ITEM_QUANTITY_TEXT_COLOR_LIGHT       = Color.WHITE;

    public static final Color WHEEL_DIALOG_LIGHT_BULB_COLOR                     = Color.GREEN;

    public static final Color WHEEL_DIALOG_TAP_TO_COLLECT_TEXT_COLOR            = Color.WHITE;
    public static final boolean WHEEL_DIALOG_TAP_TO_COLLECT_USE_SHADOW_FONT     = false;

    public static final Color WHEEL_DIALOG_REWARD_COUNT_BACKGROUND_COLOR        = Color.WHITE;
    public static final Color WHEEL_DIALOG_REWARD_COUNT_TEXT_COLOR              = Color.BLACK;
    public static final boolean WHEEL_DIALOG_REWARD_COUNT_TEXT_USE_SHADOW_FONT  = false;

    public static final Color WHEEL_DIALOG_COME_BACK_TEXT_COLOR                 = Color.WHITE;
    public static final boolean WHEEL_DIALOG_COME_BACK_USE_SHADOW_FONT          = true;

    public static final float WHEEL_DIALOG_CONFETTI_VELOCITY                    = 8f;

    /**************************************************************************************************************************************************************************/

    // Mağaza (IAP)
    public static final Color IAP_DIALOG_TITLE_TEXT_COLOR               = Color.WHITE;
    public static final Color IAP_DIALOG_TITLE_BG_COLOR                 = new Color(0xcead92FF);

    public static final Color IAP_DIALOG_LOADING_CIRCLE_COLOR           = new Color(0x7FFF00FF);

    public static final Color IAP_ONE_TIME_OFFER_RIBBON_COLOR           = new Color(0x008080ff);
    public static final Color IAP_COINS_RIBBON_COLOR                    = new Color(0x008080ff);
    public static final Color IAP_COMBO_PACK_RIBBON_COLOR               = new Color(0x728FCEff);
    public static final Color IAP_RIBBON_TEXT_COLOR                     = Color.WHITE;
    public static final boolean IAP_RIBBON_USE_SHADOW_FONT              = true;

    public static final Color IAP_CARD_TITLE_TEXT_COLOR                 = new Color(0x0C090AFF);
    public static final boolean IAP_CARD_TITLE_TEXT_USE_SHADOW_FONT     = false;
    public static final Color IAP_CARD_BG_COLOR                         = new Color(0xffeaccFF);
    public static final Color IAP_CARD_CENTER_BG_COLOR                  = new Color(0xdfbb9eFF);
    public static final Color IAP_CARD_QUANTITY_TEXT_COLOR              = Color.WHITE;
    public static final boolean IAP_CARD_QUANTITY_TEXT_USE_SHADOW_FONT  = true;

    public static final Color IAP_BUY_BUTTON_TEXT_COLOR                 = Color.WHITE;
    public static final boolean IAP_BUY_BUTTON_TEXT_USE_SHADOW_FONT     = true;

    public static final float IAP_COINS_BUY_BUTTON_TEXT_SCALE           = 1.0f;
    public static final float IAP_BUNDLE_BUY_BUTTON_TEXT_SCALE          = 1.2f;

    /**************************************************************************************************************************************************************************/
    // Bonus Kelimeler
    // --> RENK DEĞİŞTİ: Kemik beyazı (ivory) yapıldı.
    public static final Color BWD_WORDS_TEXT_COLOR                      = new Color(0xF5F0DCFF);
    // --> RENK DEĞİŞTİ: Kemik beyazı (ivory) yapıldı.
    public static final Color BWD_WORDS_TITLE_COLOR                     = new Color(0xF5F0DCFF);
    // --> RENK DEĞİŞTİ: Opak yeşilden, buzlu cam temasına uyan yarı saydama
    // çevrildi.
    public static final Color BWD_WORDS_BG_COLOR                        = new Color(0xFFFFFF33);
    public static final Color BWD_BADGE_TEXT_COLOR                      = new Color(0xeaeaeaFF);

    public static final Color BWD_DIALOG_REWARD_MODE_BG_COLOR           = new Color(0xb7aea1ff);
    public static final Color BWD_DIALOG_REWARD_MODE_BODY_TEXT_COLOR    = Color.WHITE;

    /**************************************************************************************************************************************************************************/
    // Reklamları Kaldır
    public static final Color REMOVE_ADS_DIALOG_COST_TEXT_COLOR                     = Color.WHITE;
    public static final boolean REMOVE_ADS_DIALOG_COST_TEXT_USE_SHADOW_FONT         = false;
    public static final Color REMOVE_ADS_DIALOG_BUY_BUTTON_TEXT_COLOR               = Color.WHITE;
    public static final boolean REMOVE_ADS_DIALOG_BUY_BUTTON_TEXT_USE_SHODOW_FONT   = true;
    public static final float REMOVE_ADS_DIALOG_BUY_BUTTON_WIDTH_COEF               = 0.7f;
    public static final float REMOVE_ADS_DIALOG_BUY_BUTTON_FONT_SCALE               = 1f;

    /************************************************************************************************************************************************************************/
    // PENCEREYİ DARALTAN AYAR:
    // Eski Değer: 0.8f idi.
    // Yeni Değer: 0.6f yapalım (Pencere belirgin şekilde daralır)
    public static final float WATCH_AND_EARN_DIALOG_BUTTON_WIDTH_COEF   = 0.6f;

    // BUTON YAZISINI KÜÇÜLTEN AYAR:
    // Eski Değer: 1f idi.
    // Yeni Değer: 0.85f yapalım ("İzle" yazısı kibarlaşır)
    public static final float WATCH_AND_EARN_DIALOG_BUTTON_FONT_SCALE   = 0.85f;

    /**************************************************************************************************************************************************************************/
    // Sözlük
    public static final Color DICTIONARY_DIALOG_CENTER_BG_COLOR                 = new Color(0xb7aea1ff);
    public static final Color DICTIONARY_DIALOG_LOADING_CIRCLE_COLOR            = new Color(0x008080ff);
    public static final Color DICTIONARY_DIALOG_NAVIGATION_ARROW_COLOR          = new Color(0xC83F49ff);
    public static final Color DICTIONARY_DIALOG_WORD_TEXT_COLOR                 = new Color(0xFFFFFFff);
    public static final float DICTIONARY_DIALOG_WORD_TEXT_SCALE                 = 1.2f;
    public static final boolean DICTIONARY_DIALOG_WORD_TEXT_USE_SHADOW_FONT     = false;
    public static final Color DICTIONARY_DIALOG_MEANING_TEXT_COLOR              = Color.WHITE;
    public static final boolean DICTIONARY_DIALOG_MEANING_TEXT_USE_SHADOW_FONT  = false;

    /**************************************************************************************************************************************************************************/
    // Bölüm Sonu
    public static final int LEVEL_FINISHED_VIEW_PARTICLE_COUNT                          = 7;
    public static final Color LEVEL_FINISHED_VIEW_BG_COLOR                              = new Color(0x00000056);

    public static final Color GAME_COMPLETELY_FINISHED_TEXT_COLOR                       = Color.WHITE;
    public static final boolean GAME_COMPLETELY_FINISHED_TEXT_USE_SHADOW_FONT           = true;

    public static final Color LEVEL_FINISHED_VIEW_COINS_EARNED_TEXT_COLOR               = Color.WHITE;
    public static final boolean LEVEL_FINISHED_VIEW_COINS_EARNED_TEXT_USE_SHADOW_FONT   = true;
    public static final Color LEVEL_FINISHED_VIEW_STAR_PARTICLES_COLOR                  = new Color(0xfff772ff);

    public static final float LEVEL_END_VIEW_NEXT_LEVEL_BUTTON_WIDTH_COEF               = 1.0f; // Daraltılmış
    public static final float LEVEL_END_VIEW_NEXT_LEVEL_BUTTON_FONT_SCALE               = 0.9f; // Küçültülmüş

    public static final boolean LEVEL_END_VIEW_NEXT_LEVEL_BUTTON_USE_SHADOW_FONT        = true;

    /**************************************************************************************************************************************************************************/
    // Altın Animasyonu
    public static final Color COIN_ANIM_TEXT_COLOR  = new Color(0x7fd645ff);
    public static final float COIN_ANIM_DURATION    = 0.8f;

    /**************************************************************************************************************************************************************************/
    // Önizleme Yazısı
    public static final Color PREVIEW_TEXT_COLOR        = Color.WHITE;
    public static final float PREVIEW_FONT_SCALE        = 0.7f;

    /**************************************************************************************************************************************************************************/
    // Oyun Kutuları
    public static final Color GAME_GRID_LETTER_COLOR_SOLVED                 = Color.WHITE;
    public static final Color GAME_GRID_LETTER_COLOR_REVEALED               = Color.GRAY;

    public static final int TILE_SUCCESS_SPARKLE_PARTICLE_COUNT             = 8;
    public static final Color TILE_SUCCESS_SPARKLE_PARTICLE_COLOR           = new Color(0xe2d143ff);


    public static Color getTileBackgroundUnsolvedColorByLevelIndex(int levelIndex){
        return new Color(0xffffffb3);
    }

    public static Color getDialBackgroundColorByLevelIndex(int levelIndex){
        return new Color(0xffffffb3);
    }

    public static Color getDialButtonTextColorUpStateByLevelIndex(int levelIndex){
        return Color.BLACK;
    }

    public static Color getDialButtonTextColorDownStateByLevelIndex(int levelIndex){
        return Color.WHITE;
    }

    public static final float TILE_LETTER_FONT_SCALE                        = 0.55f;


    // Tekerlek Harf Boyutları (3-4-5 harfliler büyük)
    public static float getDialButtonLetterFontScale(int numLetters){
        switch (numLetters){
            case 3:
                return 0.7f;
            case 4:
                return 0.8f;
            case 5:
                return 0.82f;
            case 6:
                return 0.75f;
            case 7:
                return 0.8f;
            case 8:
                return 0.85f;
            default:
                return 1;
        }
    }


    public static final float DIAL_BUTTON_MARGIN                            = 0.02f;


    public static final float getLengthOfLinesBetweenDialLetters(int buttonCount){
        if(buttonCount <= 5) return 10.0f;
        else return 8.0f;
    }


    public static final Color ROCKET_FIRE_COLOR                             = new Color(0xffbf00ff);
    public static final Color FINGER_SELECTED_TILE_COLOR                    = Color.GRAY;

    public static final Color DIAL_ROUND_PARTICLE_COLOR                     = Color.OLIVE;
    public static final Color DIAL_STAR_PARTICLE_COLOR                      = Color.ORANGE;
    public static final Color DIAL_STRIP_PARTICLE_COLOR                     = Color.WHITE;
    public static final Color DIAL_PARTICLE_ALTERNATE_COLOR                 = Color.ORANGE;


    // Tekerlek Buton Boyutları (3-4-5 harfliler büyük)
    public static float calculateDialButtonScale(int total){
        switch (total){
            case 3:
                return 1.1f;
            case 4:
                return 0.9f;
            case 5:
                return 0.82f;
            case 6:
                return 0.7f;
            case 7:
                return 0.65f;
            case 8:
                return 0.6f;
            default:
                return 1.0f;
        }

    }


    public static final Color BOMB_DEFUSED_RIBBON_COLOR                     = new Color(0x008080ff);
    public static final Color FEEDBACK_RIBBON_COLOR                         = new Color(0xDC143Cff);

    public static final Color FEEDBACK_TEXT_COLOR                           = Color.WHITE;
    public static final boolean FEEDBACK_TEXT_USE_SHADOW_FONT               = false;
    public static final float FEEDBACK_SHOW_DURATION                        = 0.5f;

    public static final Color COIN_ANIMATION_SPARKLE_COLOR                  = new Color(0xfbb117ff);


    // Çizgi Rengi (Royal Mavi)
    public static final Color[] levelColors = {
            Color.ROYAL,
    };

    public static final Color SIDE_COMBO_STAR_COLOR                 = new Color(0xfffac2ff);

    public static final boolean ENABLE_CAMERA_SHAKE                 = true;

    public static final float getComboShakeAmount(int comboCount){
        switch (comboCount){
            case 2: return 0.005f;
            case 3: return 0.006f;
            case 4: return 0.0065f;
            case 5: return 0.007f;
            case 6: return 0.0075f;
            default: return 0.008f;

        }
    }


    /**************************************************************************************************************************************************************************/
    // İpucu Butonları
    public static final Color HINT_BUTTON_COST_TEXT_COLOR           = Color.BLACK;
    public static final Color HINT_BUTTON_REMANING_TEXT_COLOR       = Color.WHITE;
    public static final Color HINT_BUTTON_REMANING_BG_COLOR         = new Color(0xDC143Cff);
    public static final boolean HINT_BUTTON_TEXT_USE_SHADOW_FONT    = false;

    /**************************************************************************************************************************************************************************/
    // Uyarılar (Toast)
    public static final Color TOAST_BACKGROUND_COLOR        = new Color(0x00000095);
    public static final Color TOAST_TEXT_COLOR              = new Color(0xfece57ff);
    public static final boolean TOAST_TEXT_USE_SHADOW_FONT  = false;
    public static final float TOAST_RUN_TIME                = 2f;

    /**************************************************************************************************************************************************************************/

    // Yardım Balonları
    public static final float TOOLTIP_WIDTH_COEF                = 0.3f;
    public static final float TOOLTIP_FONT_SCALE                = 0.7f;
    public static final float TOOLTIP_SHOW_DURATION             = 3f;
    public static final Color TOOLTIP_BG_COLOR                  = Color.BLACK;
    public static final Color TOOLTIP_TEXT_COLOR                = Color.WHITE;
    public static final boolean TOOLTIP_TEXT_USE_SHADOW_FONT    = false;

    /**************************************************************************************************************************************************************************/

    // Ekran Kenar Boşlukları
    public static final float MARGIN_TOP_WIDE_SCREEN = 1.8f;
    public static final float MARGIN_TOP_NORMAL_SCREEN = 2.0f;

    public static final float MARGIN_BOTTOM_WIDE_SCREEN = 0.05f;
    public static final float MARGIN_BOTTOM_NORMAL_SCREEN = 0.1f;

    public static final float LEFT_AND_RIGHT_MARGIN = 0.03f;

}