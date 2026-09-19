package ogzapp.wordgame.ui.dialogs.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import ogzapp.wordgame.WordConnectGame;
import ogzapp.wordgame.config.ConfigProcessor;
import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.model.GameData;
import ogzapp.wordgame.screens.BaseScreen;
import ogzapp.wordgame.ui.Toggle;
import ogzapp.wordgame.ui.dialogs.BaseDialog;

public class Menu extends BaseDialog {

    private Label.LabelStyle menuItemLabelStyle;

    private Runnable langSelectionCallback;
    private LanguageDialog languageDialog;
    private float rowHeight;
    private float hmargin = 0.05f;
    private float y;
    private float vSpace;
    private Label version;
    // NOT: Kullanıcı isteğiyle panel biraz daha genişletildi (0.8 -> 0.9).
    private float widthCoef = 0.9f;
    private float innerMarginCoef = 0.8f;
    private TextureAtlas atlas4;
    private float pressDarkenCoef = 0.92f;
    private final String LANGUAGE = "1";
    private final String SOUND = "2";
    private final String RATE_US = "3";
    private final String GDPR = "4";
    private final String CONTACT_US = "5";
    private final String MUSIC = "6";
    private Array<Actor> rows = new Array<>();

    // Müzik ikonu artık ResourceManager (AssetManager) üzerinden yükleniyor,
    // bkz. setMusic(). Elle oluşturulan bir Texture, uygulama arka plana
    // alınıp GL bağlamı kaybolduğunda (Android context loss) otomatik olarak
    // yeniden yüklenmediği için ikon bazen siyah bir kutu gibi görünüyordu.
    private static final String MUSIC_ICON_TEXTURE = "textures/music.png";

    public Menu(float width, float height, BaseScreen screen, Runnable langSelectionCallback) {
        super(width, height, screen);
        this.langSelectionCallback = langSelectionCallback;
        String font = UIConfig.MENU_ITEM_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        menuItemLabelStyle = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(font, BitmapFont.class), UIConfig.MENU_ITEM_TEXT_COLOR);

        float contentHeight = calculateContentHeight();
        rowHeight = calculateRowHeight();
        vSpace = calculateVSpace();
        boolean inEu = screen.wordConnectGame.adManager != null && screen.wordConnectGame.adManager.isUserInEU();
        contentHeight += (ConfigProcessor.findTotalEnabledMenuRows(inEu, GameConfig.availableLanguages.size() > 1)) * vSpace;

        version = createVersionText(screen.wordConnectGame.version);
        version.setY(version.getPrefHeight() * version.getFontScaleY() + 10f);

        y = version.getHeight() * 2f;
        content.setSize(width * widthCoef, contentHeight + y);
        setContentBackground();
        // Referans görseldeki buzlu cam panel görünümü: Menu'ye özel yarı
        // saydam beyaz renk (DIALOG_BACKGROUND_COLOR global/opak varsayılanı
        // ETKİLENMİYOR, diğer dialoglar aynı kalıyor).
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);

        atlas4 = screen.wordConnectGame.resourceManager.get(ResourceManager.ATLAS_4, TextureAtlas.class);
        setTitleLabel(LanguageManager.get("menu"));
        // Referans görselde başlığın arkasında kutu yok, yazı camın üzerinde
        // duruyor - bu yüzden SADECE Menu'nün başlık arka planını saydam
        // yapıyoruz (diğer dialogların teal başlık kutusu ETKİLENMİYOR).
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        // "MENÜ" yazısı, arkasındaki kutu kaldırılınca biraz fazla yukarıda/
        // panelin dışında kalıyordu - sadece Menu'ye özel olarak biraz
        // aşağıya indirildi ve çok az büyütüldü (diğer dialogların başlık
        // konumu/boyutu ETKİLENMİYOR).
        titleLabel.setFontScale(titleLabel.getFontScaleX() * 1.10f);
        // BaseDialog.setTitleLabel() ile AYNI merkezleme mantığı - font
        // ölçeği burada büyütüldüğü için getPrefHeight() yeniden alınıyor.
        titleLabel.setY(titleContainer.getHeight() * 0.30f - titleLabel.getPrefHeight() * 0.5f);
        setCloseButton();
        // Referans görseldeki gibi, kapatma butonu panelin sağ üst
        // köşesinin biraz DIŞINA taşsın diye konumu ayarlanıyor (sadece
        // Menu'ye özel - diğer dialogların kapatma butonu konumu
        // ETKİLENMİYOR).
        closeButton.setX(content.getWidth() - closeButton.getWidth() * 0.55f);
        closeButton.setY(content.getHeight() - closeButton.getHeight() * 0.55f);

        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });

        hmargin *= content.getWidth();
        init();
    }

    private float calculateContentHeight() {
        boolean inEu = screen.wordConnectGame.adManager != null && screen.wordConnectGame.adManager.isUserInEU();
        boolean hasManyLocale = GameConfig.availableLanguages.size() > 1;
        return (ConfigProcessor.findTotalEnabledMenuRows(inEu, hasManyLocale) + 2) * calculateRowHeight() * 1.05f;
    }

    private float calculateRowHeight() {
        return AtlasRegions.ic_rate_us.getRegionHeight() * UIConfig.MENU_ITEM_HEIGHT_COEF;
    }

    private float calculateVSpace() {
        return rowHeight * UIConfig.MENU_ITEM_SPACING_COEF;
    }

    private void init() {
        rows.add(version);
        version.setX((content.getWidth() - version.getWidth()) * 0.5f);

        boolean inEu = screen.wordConnectGame.adManager != null && screen.wordConnectGame.adManager.isUserInEU();
        if (UIConfig.MENU_ITEM_GDPR_ENABLED && inEu) setGDPR();
        if (UIConfig.MENU_ITEM_RATE_US_ENABLED) setRateUs();
        if (UIConfig.MENU_ITEM_CONTACT_US_ENABLED) setContactUs();
        boolean hasManyLocale = GameConfig.availableLanguages.size() > 1;
        if (UIConfig.MENU_ITEM_LANGUAGE_ENABLED && hasManyLocale) setLanguage();
        if (UIConfig.MENU_ITEM_SOUND_ENABLED) setSound();
        setMusic();

        content.addActor(version);
    }

    private void setGDPR() {
        Group group1 = getRow(AtlasRegions.gdpr, LanguageManager.get("gdpr"));
        group1.setName(GDPR);
        content.addActor(group1);
        group1.setY(y);
        y += group1.getHeight() + vSpace;
    }

    private void setRateUs() {
        Group group2 = getRow(AtlasRegions.ic_rate_us, LanguageManager.get("rate_us"));
        group2.setName(RATE_US);
        group2.setY(y);
        content.addActor(group2);
        y += group2.getHeight() + vSpace;
    }

    private void setContactUs() {
        Group group3 = getRow(AtlasRegions.ic_email, LanguageManager.get("contact_us"));
        group3.setName(CONTACT_US);
        group3.setY(y);
        content.addActor(group3);
        y += group3.getHeight() + vSpace;
    }

    private void setLanguage() {
        Group group4 = getRow(AtlasRegions.ic_language, LanguageManager.get("language"));
        group4.setName(LANGUAGE);
        group4.setY(y);
        content.addActor(group4);
        y += group4.getHeight() + vSpace;

        TextureAtlas.AtlasRegion langIcon = atlas4.findRegion(LanguageManager.locale.code);
        if (langIcon != null) {
            group4.setOrigin(Align.center);
            Image image = new Image(langIcon);
            float size = group4.getHeight() * 0.7f;
            image.setSize(size, size);
            image.setX((group4.getWidth() - image.getWidth()) - hmargin * innerMarginCoef);
            image.setY((group4.getHeight() - image.getHeight()) * 0.5f);
            group4.addActor(image);
            image.setOrigin(Align.center);
        }
    }

    private void setSound() {
        Group group5 = getRow(AtlasRegions.ic_sound, LanguageManager.get("sounds"));
        group5.setName(SOUND);
        group5.setY(y);
        content.addActor(group5);

        final Toggle toggle = new Toggle();
        toggle.setEnabled(!ConfigProcessor.muted);
        toggle.setX(group5.getWidth() - toggle.getWidth() - hmargin * innerMarginCoef);
        toggle.setY((group5.getHeight() - toggle.getHeight()) * 0.5f);
        group5.addActor(toggle);
        toggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameData.setGameMute(!toggle.isEnabled());
            }
        });

        toggle.setOrigin(Align.center);
        y += group5.getHeight() + vSpace;
    }

    // Müzik butonu - music.png ikonu kullanılır
    private void setMusic() {
        Group group6 = new Group();
        group6.setName(MUSIC);
        group6.setSize(content.getWidth() - hmargin * 2, rowHeight);

        // Kullanıcı isteğiyle kenarlık ÇİZGİSİ kaldırıldı - sadece buzlu
        // cam zemin kaldı.
        Image bg = new Image(NinePatches.round_rect_shadow);
        bg.setName("bg");
        bg.setSize(group6.getWidth(), group6.getHeight());
        bg.setColor(UIConfig.MENU_ITEM_BG_COLOR);
        group6.addActor(bg);

        // Müzik ikonu (music.png) - ResourceManager (AssetManager) üzerinden
        // yükleniyor; böylece diğer ikonlar gibi context loss sonrası
        // otomatik olarak yeniden yükleniyor ve siyah kutu olarak görünmüyor.
        if (!screen.wordConnectGame.resourceManager.contains(MUSIC_ICON_TEXTURE)) {
            screen.wordConnectGame.resourceManager.load(MUSIC_ICON_TEXTURE, Texture.class);
            screen.wordConnectGame.resourceManager.finishLoading();
        }
        Texture musicTexture = screen.wordConnectGame.resourceManager.get(MUSIC_ICON_TEXTURE, Texture.class);
        musicTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image musicIcon = new Image(musicTexture);
        musicIcon.setOrigin(Align.center);

        // 1. Boyutu ayarla
        float iconSize = rowHeight * 0.7f;
        musicIcon.setSize(iconSize, iconSize);

        // 2. Konumu ayarla (örneğin 10 piksel yukarı)
        musicIcon.setX(hmargin * innerMarginCoef);
        musicIcon.setY((group6.getHeight() - musicIcon.getHeight()) * 0.5f + 10f);

        group6.addActor(musicIcon);
        addIconShadow(group6, musicIcon, iconSize);

        // Metin
        Label label = new Label(LanguageManager.get("music"), menuItemLabelStyle);
        label.setX(musicIcon.getX() + musicIcon.getWidth() + hmargin * innerMarginCoef);
        label.setY((group6.getHeight() - label.getHeight()) * 0.5f);
        group6.addActor(label);

        // Toggle
        final Preferences prefs = Gdx.app.getPreferences("MyPreferences");
        final Toggle toggle = new Toggle();
        toggle.setEnabled(prefs.getBoolean("music_on", true));
        toggle.setX(group6.getWidth() - toggle.getWidth() - hmargin * innerMarginCoef);
        toggle.setY((group6.getHeight() - toggle.getHeight()) * 0.5f);
        group6.addActor(toggle);
        toggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean on = toggle.isEnabled();
                prefs.putBoolean("music_on", on).flush();
                WordConnectGame game = screen.wordConnectGame;
                if (game.fonMuzigi != null) {
                    if (on) game.fonMuzigi.play();
                    else game.fonMuzigi.pause();
                }
            }
        });
        toggle.setOrigin(Align.center);

        group6.addListener(clickListener);
        group6.setX(hmargin);
        group6.setY(y);
        content.addActor(group6);
        y += group6.getHeight() + vSpace;
        rows.add(group6);
    }

    private Group getRow(TextureAtlas.AtlasRegion icon, String title) {
        Group container = new Group();
        container.setName(title);
        container.setSize(content.getWidth() - hmargin * 2, rowHeight);

        // Kullanıcı isteğiyle kenarlık ÇİZGİSİ kaldırıldı - sadece buzlu
        // cam zemin kaldı.
        Image bg = new Image(NinePatches.round_rect_shadow);
        bg.setName("bg");
        bg.setSize(container.getWidth(), container.getHeight());
        bg.setColor(UIConfig.MENU_ITEM_BG_COLOR);
        container.addActor(bg);

        Image ic = new Image(icon);
        ic.setOrigin(Align.center);
        ic.setX(hmargin * innerMarginCoef);
        ic.setY((container.getHeight() - ic.getHeight()) * 0.5f);
        container.addActor(ic);
        addIconShadow(container, ic, ic.getWidth());

        Label label = new Label(title, menuItemLabelStyle);
        label.setX(ic.getX() + ic.getWidth() + hmargin * innerMarginCoef);
        label.setY((container.getHeight() - label.getHeight()) * 0.5f);
        container.addActor(label);
        container.addListener(clickListener);
        container.setX(hmargin);

        rows.add(container);
        return container;
    }

    // İkonların arka plan üzerinde daha net görünmesi için altlarına hafif bir
    // gölge ekler. Yeni bir görsel gerektirmez: ikonun kendi dokusunu koyu ve
    // saydam olarak biraz büyütüp aşağı kaydırarak çizer.
    private void addIconShadow(Group container, Image icon, float iconSize) {
        float scale = 1.12f;
        float w = icon.getWidth() * scale;
        float h = icon.getHeight() * scale;

        Image shadow = new Image(icon.getDrawable());
        shadow.setSize(w, h);
        shadow.setColor(0f, 0f, 0f, 0.4f);
        shadow.setPosition(
                icon.getX() + (icon.getWidth() - w) * 0.5f,
                icon.getY() + (icon.getHeight() - h) * 0.5f - iconSize * 0.05f
        );
        container.addActorBefore(icon, shadow);
    }

    private Label createVersionText(String v) {
        if (v == null) v = "";
        String font = UIConfig.MENU_DIALOG_VERSION_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        Label.LabelStyle style = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(font, BitmapFont.class), UIConfig.MENU_DIALOG_VERSION_TEXT_COLOR);
        Label label = new Label(LanguageManager.format("app_version", v), style);
        label.setAlignment(Align.center);
        label.setFontScale(0.7f);
        return label;
    }

    private InputListener clickListener = new InputListener() {
        private Group getRowFromTarget(Actor target) {
            while (target != null) {
                if (target instanceof Group && rows.contains((Group) target, true)) {
                    return (Group) target;
                }
                target = target.getParent();
            }
            return null;
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            Group row = getRowFromTarget(event.getTarget());
            if (row == null) return false;
            if (row.getName() != null) {
                if (row.getName().equals(SOUND) || row.getName().equals(MUSIC)) {
                    Actor toggle = row.findActor("toggle");
                    if (toggle != null && (event.getTarget() == toggle || event.getTarget().isDescendantOf(toggle))) {
                        return false;
                    }
                }
            }
            Image bg = (Image) row.findActor("bg");
            if (bg != null) {
                Color c = bg.getColor();
                c.r *= pressDarkenCoef;
                c.g *= pressDarkenCoef;
                c.b *= pressDarkenCoef;
            }
            row.setScale(0.97f);
            return true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            Group row = getRowFromTarget(event.getTarget());
            if (row == null) return;
            Image bg = (Image) row.findActor("bg");
            if (bg != null) {
                Color c = bg.getColor();
                c.r *= 1 / pressDarkenCoef;
                c.g *= 1 / pressDarkenCoef;
                c.b *= 1 / pressDarkenCoef;
            }
            row.setScale(1f);
            onMenuRowClick(row);
            super.touchUp(event, x, y, pointer, button);
        }
    };

    private void onMenuRowClick(Actor target) {
        String name = target.getName();
        if (name == null) return;
        switch (name) {
            case LANGUAGE:
                // Menü'yü kapatıyoruz ki Dil ekranı tek başına kalsın; aksi
                // halde Menü arkada açık kalıyor ve oyuna dönmek için geri
                // tuşuna iki kez basmak gerekiyordu. Dil dialogunu Menü'nün
                // ÇOCUĞU olarak değil doğrudan stage'e ekliyoruz - yoksa
                // Menü'nün kapanma animasyonu bitince (bu, tamamen
                // görünmez hale gelen bir üst grup demek) Dil ekranı da
                // onunla birlikte görünmez olurdu.
                hide();
                if (languageDialog == null)
                    languageDialog = new LanguageDialog(getWidth(), getHeight(), screen, langSelectionCallback);
                screen.stage.addActor(languageDialog);
                languageDialog.show();
                break;
            case GDPR:
                screen.wordConnectGame.adManager.openGDPRForm();
                break;
            case RATE_US:
                if (screen.wordConnectGame.rateUsLauncher != null)
                    screen.wordConnectGame.rateUsLauncher.launch();
                break;
            case CONTACT_US:
                if (screen.wordConnectGame.supportRequest != null)
                    screen.wordConnectGame.supportRequest.sendSupportEmail();
                break;
        }
    }

    @Override
    protected void openAnimFinished() {
        super.openAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);
    }

    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);
        remove();
    }

    @Override
    public void show() {
        super.show();
        content.setColor(1, 1, 1, 0);
        content.setScale(0.8f);
        content.addAction(Actions.parallel(
                Actions.fadeIn(0.3f),
                Actions.scaleTo(1f, 1f, 0.3f)
        ));
    }

    @Override
    public void hide() {
        // KÖK NEDEN DÜZELTMESİ: Bu metod önceden backNavQueue.pop()
        // işlemini Menu.super.hide() üzerinden, yani 0.2 saniyelik
        // kapanma animasyonu BİTTİKTEN SONRA yapıyordu. Ama "Dil"
        // dialogu hemen ardından, animasyon beklenmeden AYNI ANDA
        // (senkron) backNavQueue'ya EKLENİYORDU. Sonuç: kuyruk kısa
        // süreliğine [Menu, Dil] oluyor, 0.2sn sonra çalışan pop() ise
        // Menü'yü değil o sırada üstte olan Dil'i düşürüyordu. Bu da
        // "oyuna dönmek için geri tuşuna iki kez basma" hatasının asıl
        // sebebiydi. Artık kuyruktan çıkarma, diğer TÜM dialoglarla
        // (BaseDialog.hide()) AYNI ANDA - animasyon başlamadan HEMEN
        // ÖNCE - yapılıyor. Animasyonun geri kalanı (fade+scale) için
        // closeDialog() doğrudan çağrılıyor, Menu.super.hide() ARTIK
        // ÇAĞRILMIYOR (o, kuyruktan bir daha - yanlışlıkla ikinci kez -
        // çıkarma yapardı).
        if (screen.backNavQueue != null && screen.backNavQueue.size() > 0) screen.backNavQueue.pop();

        content.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.fadeOut(0.2f),
                        Actions.scaleTo(0.8f, 0.8f, 0.2f)
                ),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        closeDialog();
                    }
                })
        ));
    }
}