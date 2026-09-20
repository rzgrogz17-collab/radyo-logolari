package ogzapp.wordgame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;

import ogzapp.wordgame.WordConnectGame;
import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;

import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.graphics.AtlasRegions;


import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.model.Constants;
import ogzapp.wordgame.model.GameData;

import ogzapp.wordgame.ui.dialogs.ConfirmDialog;
import ogzapp.wordgame.ui.tutorial.Tutorial;

import ogzapp.wordgame.util.UiUtil;


public class IntroScreen extends BaseScreen{

    // Referans görseldeki gibi: koyu lacivert yazılı, beyaz/aydınlık pill
    // buton. LevelEndView.java'daki "Sonraki Seviye" butonuyla BİREBİR AYNI
    // renkler - UIConfig.INTRO_PLAY_BUTTON_* renkleri artık kullanılmıyor,
    // ikisi de tamamen aynı görünsün diye burada sabit tanımlandı.
    private static final Color PLAY_BUTTON_TEXT_COLOR    = new Color(0x16324FFF);
    private static final Color PLAY_BUTTON_BG_COLOR      = new Color(0xF7F9FCFF);
    private static final Color PLAY_BUTTON_BG_DOWN_COLOR = new Color(0xE2E7EEFF);

    // --- Tam "hap" (pill/stadium) şekilli buton dokusu üretimi ---
    // NinePatches.rrect atlas dokusuna bağlı kalmıyoruz (o dokunun köşe
    // yarıçapı referans görseldeki kadar yuvarlak olmayabilir); bunun
    // yerine köşeleri MATEMATİKSEL olarak tam yarım daire olan bir doku
    // koddan üretiliyor - LevelEndView.java'daki "Sonraki Seviye" butonuyla
    // BİREBİR AYNI yöntem, ikisi kesinlikle aynı görünsün diye.
    private static final float PILL_HEIGHT_TO_TEXT_RATIO = 2.2f; // yazı, toplam yüksekliğin ~%45'ini kaplar
    private static final int PILL_TEXTURE_SUPERSAMPLE = 4;       // kenarları yumuşatmak için yüksek çözünürlükte çizip küçültüyoruz
    private static final int PILL_STRETCH_PX = 4;                // ortada genişleyebilen ince düz şerit

    private static float computePillButtonHeight(BitmapFont font, float fontScale) {
        return font.getLineHeight() * fontScale * PILL_HEIGHT_TO_TEXT_RATIO;
    }

    public static NinePatch createPillNinePatch(float height, Color color) {
        int h = Math.max(8, Math.round(height));
        int hi = h * PILL_TEXTURE_SUPERSAMPLE;
        if (hi % 2 != 0) hi++;
        int radiusHi = hi / 2;
        int stretchHi = PILL_STRETCH_PX * PILL_TEXTURE_SUPERSAMPLE;
        int wi = radiusHi * 2 + stretchHi;

        Pixmap big = new Pixmap(wi, hi, Pixmap.Format.RGBA8888);
        big.setColor(color);
        big.fillCircle(radiusHi, radiusHi, radiusHi);
        big.fillCircle(wi - radiusHi, radiusHi, radiusHi);
        big.fillRectangle(radiusHi, 0, stretchHi, hi);

        int w = Math.max(1, wi / PILL_TEXTURE_SUPERSAMPLE);
        Pixmap small = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        big.setFilter(Pixmap.Filter.BiLinear);
        small.setFilter(Pixmap.Filter.BiLinear);
        small.drawPixmap(big, 0, 0, wi, hi, 0, 0, w, h);
        big.dispose();

        // KÖK NEDEN DÜZELTMESİ (context-loss dayanıklılığı): Bu metot
        // LevelEndView.java'daki "Sonraki Seviye" butonuyla BİREBİR AYNI
        // (bkz. yukarıdaki dosya yorumu) ve aynı gizli hatayı taşıyordu:
        // düz "new Texture(small); small.dispose();" ile üretilen doku
        // "unmanaged" oluyor, Android'de GL context kaybından (uygulama
        // arka plana alınıp geri dönme) sonra libGDX tarafından otomatik
        // olarak yeniden yüklenmiyor ve geçersiz bir GL ID ile "hayalet"
        // halde kalıyor. O ID daha sonra BAŞKA bir dokuya yeniden atanınca
        // bu buton o dokunun içeriğini (ya da boş/soluk bir görüntü)
        // gösterebiliyordu. LevelEndView.java'daki ile TAMAMEN AYNI çözüm
        // burada da uygulanıyor: pixmap ASLA dispose edilmiyor, managed=true
        // ile bir PixmapTextureData'ya sarılıp Texture'a öyle veriliyor -
        // böylece bu doku da context kaybından sonra kendini GEÇERLİ, yeni
        // bir GL ID ile doğru içerikle yeniden oluşturuyor.
        PixmapTextureData texData = new PixmapTextureData(small, small.getFormat(), false, false, true);
        Texture tex = new Texture(texData);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        int radius = h / 2;
        return new NinePatch(tex, radius, radius, 0, 0);
    }

    private Image logo;
    public TextButton playButton;
    private Image playButtonShadow;


    public IntroScreen(WordConnectGame wordConnectGame) {
        super(wordConnectGame);
    }


    @Override
    public void show() {

        LanguageManager.updateSelectedLanguage();

        super.show();


        if(GameConfig.DEBUG_LEVEL_INDEX > -1) {
            GameData.updateFirstIncompleteLevelIndex(GameConfig.DEBUG_LEVEL_INDEX);
            GameData.clearTileStates();
            GameData.clearSavedSolvedWordsJson();
            GameData.clearWordsWithRocket();
            GameData.clearExtraWords();
            GameData.saveComboCount(0);
            GameData.saveComboReward(0);
        }



        int firstIncompleteLevel = GameData.findFirstIncompleteLevel();

        if(GameConfig.SKIP_INTRO){
            GameData.saveTutorialStep(Constants.TUTORIAL_PLAY_BUTTON);
            wordConnectGame.setScreen(new GameScreen(wordConnectGame));
            return;
        }




        setBackground(UIConfig.INTRO_SCREEN_BACKGROUND_COLOR, UIConfig.getIntroScreenBackgroundImage(wordConnectGame));

        setTopPanel();
        topPanel.setY(stage.getHeight());

        logo = new Image(AtlasRegions.splash_logo);
        logo.setOrigin(Align.center);
        logo.setScale(0);
        final float heightScaleFactor = 1.5f;
        final float widthScaleFactor = 1.5f;

        logo.setHeight(logo.getHeight() * heightScaleFactor);
        logo.setWidth(logo.getWidth() * widthScaleFactor);

        logo.setX((stage.getWidth() - logo.getWidth()) * 0.5f);
        logo.setY(stage.getHeight() * 0.6f);
        stage.addActor(logo);


        if(LanguageManager.locale.LevelCount > 0) {
            createPlayButton(firstIncompleteLevel);
        }else {
            Gdx.app.log("game.log", "You haven't generated any levels!");
            return;
        }
        animateIn();
    }



    private void createPlayButton(int firstIncompleteLevel){
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        String font = UIConfig.INTRO_PLAY_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        buttonStyle.font = wordConnectGame.resourceManager.get(font, BitmapFont.class);
        buttonStyle.fontColor = PLAY_BUTTON_TEXT_COLOR;

        NinePatch rUp = createPillNinePatch(computePillButtonHeight(buttonStyle.font, UIConfig.INTRO_PLAY_BUTTON_FONT_SCALE), PLAY_BUTTON_BG_COLOR);
        NinePatch rDown = createPillNinePatch(computePillButtonHeight(buttonStyle.font, UIConfig.INTRO_PLAY_BUTTON_FONT_SCALE), PLAY_BUTTON_BG_DOWN_COLOR);
        buttonStyle.up = new NinePatchDrawable(rUp);
        buttonStyle.down = new NinePatchDrawable(rDown);

        String label = null;

        if(firstIncompleteLevel == LanguageManager.locale.LevelCount){
            label = LanguageManager.get("to_be_continued");
        } else {
            label = LanguageManager.format("play_label", firstIncompleteLevel + 1);
        }

        playButton = new TextButton(label, buttonStyle);
        playButton.getLabel().setFontScale(UIConfig.INTRO_PLAY_BUTTON_FONT_SCALE);
        playButton.setWidth(playButton.getLabel().getWidth() + rUp.getLeftWidth() * UIConfig.INTRO_PLAY_BUTTON_WIDTH_COEF);
        // Yüksekliği, üretilen "hap" dokusunun gerçek piksel yüksekliğiyle
        // BİREBİR eşleştiriyoruz - aksi halde nine-patch dikeyde esneyip
        // uçlar tam yarım daire olmaktan çıkar (oval/ezik görünür).
        playButton.setHeight(rUp.getTotalHeight());
        playButton.setTransform(true);

        /*  BUTON KONUM */

        playButton.setOrigin(Align.center);
        playButton.setScale(0);
        playButton.setX((stage.getWidth() - playButton.getWidth()) * 0.5f);
        playButton.setY(stage.getHeight() * 0.2f);
        if(label.equals(LanguageManager.get("to_be_continued"))) playButton.setDisabled(true);

        // Buton dokusuyla BİREBİR AYNI üretim yöntemiyle (createPillNinePatch)
        // bir "arkalık" oluşturuyoruz - böylece köşeleri her zaman butonla
        // TAM AYNI oranda yuvarlak olur. Eski "NinePatches.round_rect_shadow"
        // denemesinde, büyüttüğümüz buton boyutuyla orantısız kalıp neredeyse
        // kare/kutu gibi görünüyordu; artık bu mümkün değil. Tamamen opak
        // (şeffaf DEĞİL), düz beyaz.
        float shadowMargin = playButton.getHeight() * 0.18f;
        float shadowHeight = playButton.getHeight() + shadowMargin;
        NinePatch shadowPatch = createPillNinePatch(shadowHeight, Color.WHITE);
        playButtonShadow = new Image(new NinePatchDrawable(shadowPatch));
        playButtonShadow.setWidth(playButton.getWidth() + shadowMargin);
        playButtonShadow.setHeight(shadowHeight);
        playButtonShadow.setPosition(playButton.getX() - shadowMargin * 0.5f, playButton.getY() - shadowMargin * 0.5f);
        playButtonShadow.setOrigin(Align.center);
        playButtonShadow.setScale(0);
        stage.addActor(playButtonShadow);

        stage.addActor(playButton);


        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stage.getRoot().setTouchable(Touchable.disabled);
                playButton.clearActions();
                playButton.setScale(1f);

                if(tutorial != null) {
                    GameData.saveTutorialStep(Constants.TUTORIAL_PLAY_BUTTON);
                    tutorial.fadeOut(null, true);
                }

                animateOut(new Runnable() {
                    @Override
                    public void run() {
                        wordConnectGame.setScreen(new GameScreen(wordConnectGame));
                    }
                });

            }
        });
    }







    private void animateIn(){
        topPanel.addAction(Actions.moveBy(0, -topPanel.getHeight(), 0.2f, ogzapp.wordgame.actions.Interpolation.backOut));

        UiUtil.actorAnimIn(logo, 0.2f, null);
        if (playButtonShadow != null) UiUtil.actorAnimIn(playButtonShadow, 0.3f, null);
        UiUtil.actorAnimIn(playButton, 0.3f, animateInFinished);
        // Yazı, butonun kendisinden önce belirmesin diye ölçek animasyonuna
        // paralel bir alfa (şeffaflık) geçişi ekleniyor.
        playButton.getColor().a = 0f;
        playButton.addAction(Actions.sequence(Actions.delay(0.3f), Actions.fadeIn(0.3f)));
    }




    private Runnable animateInFinished = new Runnable() {
        @Override
        public void run() {

            if(UIConfig.INTERACTIVE_TUTORIAL_ENABLED && GameData.getTutorialStep() == 0){
                tutorialStep_1();
            }else{
                if(checkDailyRewardTiming()){
                    setPlayButtonVisibleForDailyWheel(false);
                } else if(!checkWheelDialogTiming()){
                    checkRateStatus();
                }
            }
            // Kalp atışı / nabız (pulse) efekti kasıtlı olarak KALDIRILDI -
            // "Sonraki Seviye" butonuyla aynı, sade/hareketsiz duruyor.
            // (Eskiden UIConfig.INTRO_PLAY_BUTTON_PULSATE burada
            // UiUtil.pulsate(playButton) çağırıyordu.)
        }
    };





    private void tutorialStep_1(){
        tutorial = new Tutorial(this);
        tutorial.paddingX = 1.0f;
        tutorial.paddingY = 1.2f;
        tutorial.step = Constants.TUTORIAL_PLAY_BUTTON;
        stage.addActor(tutorial);
        tutorial.getColor().a = 0f;
        tutorial.highlightActor(playButton, Tutorial.Shape.RECT);
        tutorial.addAction(Actions.fadeIn(.3f));
        tutorial.indicateActor(90);
    }




    private void animateOut(Runnable callback){
        topPanel.addAction(Actions.moveBy(0, topPanel.getHeight(), 0.2f, ogzapp.wordgame.actions.Interpolation.backIn));
        UiUtil.actorAnimOut(logo, 0.1f, null);
        if (playButtonShadow != null) UiUtil.actorAnimOut(playButtonShadow, 0.2f, null);
        UiUtil.actorAnimOut(playButton, 0.2f, 0.08f, callback);
        // Yazı, butonun kendisinden önce kaybolmasın diye ölçek animasyonuna
        // paralel bir alfa (şeffaflık) geçişi ekleniyor.
        playButton.addAction(Actions.sequence(Actions.delay(0.2f), Actions.fadeOut(0.3f)));
    }





    public void setPlayButtonVisibleForDailyWheel(boolean visible) {
        if (playButton == null) return;
        playButton.clearActions();
        playButton.setScale(1f);
        playButton.getColor().a = 1f;
        playButton.setVisible(visible);
        playButton.setTouchable(visible ? Touchable.enabled : Touchable.disabled);
        if (playButtonShadow != null) {
            playButtonShadow.clearActions();
            playButtonShadow.setScale(1f);
            playButtonShadow.getColor().a = 1f;
            playButtonShadow.setVisible(visible);
        }
    }


    private void checkRateStatus(){
        if(GameConfig.SHOW_RATE_DIALOG && wordConnectGame.rateUsLauncher != null){

            if(GameConfig.DEBUG_RATE_US){
                showRateDialog();
                return;
            }

            Preferences prefs = Gdx.app.getPreferences(Constants.PREFS_NAME);
            if(prefs.getBoolean(Constants.KEY_DONT_SHOW_AGAIN, false)){
                return;
            }

            int launchCount = prefs.getInteger(Constants.KEY_APP_LAUNCH_COUNT, 0);
            launchCount++;
            prefs.putInteger(Constants.KEY_APP_LAUNCH_COUNT, launchCount);

            long firstLaunchDate = prefs.getLong(Constants.KEY_APP_FIRST_LAUNCH_DATE, 0);
            if(firstLaunchDate == 0){
                prefs.putLong(Constants.KEY_APP_FIRST_LAUNCH_DATE, TimeUtils.millis());
            }

            if(launchCount >= GameConfig.APP_LAUNCHES_BEFORE_RATE){
                if(TimeUtils.millis() >= firstLaunchDate + GameConfig.DAYS_TO_ELAPSE_BEFORE_RATE  * 24 * 60 * 60 * 1000){
                    showRateDialog();
                }
            }

            prefs.flush();
        }
    }





    private void showRateDialog(){
        ConfirmDialog confirmDialog = new ConfirmDialog(
                stage.getWidth(),
                stage.getHeight(),
                this,
                LanguageManager.get("rate_us_title"),
                LanguageManager.get("rate_us_text"),
                LanguageManager.get("rate_button_label"),
                LanguageManager.get("later_button_label")
        );

        confirmDialog.setDialogId(Constants.CONFIRM_DIALOG_RATE_US);
        stage.addActor(confirmDialog);
        confirmDialog.show();

        confirmDialog.setConfirmCallback(new ConfirmDialog.ConfirmCallback() {
            @Override
            public void confirmClicked(String buttonLabel) {
                if(buttonLabel != null) {
                    if (buttonLabel.equals(LanguageManager.get("rate_button_label"))){
                        wordConnectGame.rateUsLauncher.launch();
                    }

                    Preferences prefs = Gdx.app.getPreferences(Constants.PREFS_NAME);
                    prefs.putBoolean(Constants.KEY_DONT_SHOW_AGAIN, true);
                    prefs.flush();
                }
            }
        });
    }




    @Override
    protected boolean onBackPress() {

        boolean hasDialog = super.onBackPress();



        if(!hasDialog) wordConnectGame.appExit.exitApp();
        return false;
    }
}
