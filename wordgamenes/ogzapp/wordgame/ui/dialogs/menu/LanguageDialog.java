package ogzapp.wordgame.ui.dialogs.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;


import java.util.HashMap;
import java.util.Map;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.i18n.Locale;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;

import ogzapp.wordgame.ui.dialogs.BaseDialog;

public class LanguageDialog extends BaseDialog {

    // Native display name shown next to each flag, matching the reference
    // "Choisir la langue" layout (flag + language name inside a box).
    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<String, String>() {
        {
            put("en", "English");
            put("af", "Afrikaans");
            put("cs", "Čeština");
            put("de", "Deutsch");
            put("es", "Español");
            put("fr", "Français");
            put("id", "Indonesia");
            put("it", "Italiano");
            put("nl", "Nederlands");
            put("pl", "Polski");
            put("ro", "Română");
            put("ru", "Русский");
            put("tr", "Türkçe");
            put("uk", "Українська");
            put("uz", "Oʻzbekcha");
        }
    };

    // Seçili satırın içi diğerleriyle aynı buzlu cam. Seçim: sağdaki boş
    // radyo halkası + satırın etrafında hafif krem ışıltı (yeşil dolgu yok).
    private static final Color ITEM_BG_COLOR = UIConfig.MENU_ITEM_BG_COLOR;
    private static final Color ITEM_TEXT_COLOR = UIConfig.MENU_ITEM_TEXT_COLOR;
    private static final Color ITEM_TEXT_SELECTED_COLOR = UIConfig.MENU_ITEM_TEXT_COLOR;
    private static final Color RADIO_RING_COLOR = new Color(0xE8E8E8FF);

    private Runnable callback;
    private Table table = new Table();
    private String currentLanguage;
    private TextButton confirmButton;

    private final Map<String, Image> bgByCode = new HashMap<>();
    private final Map<String, Image> glowByCode = new HashMap<>();
    private final Map<String, Image> radioByCode = new HashMap<>();
    private final Map<String, Label> labelByCode = new HashMap<>();
    private String selectedCode;


    public LanguageDialog(float width, float height, BaseScreen screen, Runnable callback) {
        super(width, height, screen);
        this.callback = callback;

        // NOT: Kullanıcının verdiği referans görsele göre liste artık
        // ESKİ 2 SÜTUNLU IZGARA yerine TEK SÜTUN (üst üste) diziliyor.
        // 15 dil tek sütunda ekrana sığmayacağı kadar uzun olduğundan,
        // panel artık (referans görseldeki gibi) SABİT bir yükseklikte
        // tutuluyor ve liste bir ScrollPane içinde yukarı/aşağı kaydırılıyor.
        // NOT: Kullanıcı isteğiyle panel biraz daha genişletildi (0.88 -> 0.94).
        // Boy, üstten ve alttan yaklaşık bir ülke satırı kadar kısaltıldı;
        // altta aynı yükseklikte yeşil (sarı kenarlı) kapatma butonu var.
        float contentW = width * 0.94f;
        float tableWidth = contentW * 0.9f;
        float cellHeight = tableWidth * 0.175f;
        content.setSize(contentW, height * 0.82f - cellHeight);

        TextureAtlas atlas4 = screen.wordConnectGame.resourceManager.get(ResourceManager.ATLAS_4, TextureAtlas.class);

        table.setWidth(tableWidth);

        if (LanguageManager.locale != null)
            selectedCode = LanguageManager.locale.code;

        Label.LabelStyle nameStyle = new Label.LabelStyle(
                screen.wordConnectGame.resourceManager.get(ResourceManager.fontSemiBold, BitmapFont.class),
                ITEM_TEXT_COLOR);

        float pad = tableWidth * 0.025f;
        // Kullanıcı isteğiyle: satırlar artık tam genişlik DEĞİL - dış
        // kenarlardan (sağ/sol) biraz İÇERİ daraltıldı (0.90) ve buna
        // karşılık biraz daha UZUN/yüksek yapıldı (0.155 -> 0.175). Table
        // varsayılan olarak her satırı KENDİ sütununda ORTALADIĞI için
        // (satır tableWidth'ten dar olunca) narrower satırlar otomatik
        // olarak ortalanıyor - ekstra bir X kayması gerekmiyor.
        float cellWidth = tableWidth * 0.90f;
        float radioSize = cellHeight * 0.30f;
        float radioPad = cellHeight * 0.28f;
        Texture radioTex = createRadioRingTexture(
                Math.max(16, Math.round(radioSize)),
                Math.max(2, Math.round(radioSize * 0.10f)),
                RADIO_RING_COLOR);

        for (Map.Entry<String, Locale> entry : GameConfig.availableLanguages.entrySet()) {
            String code = entry.getKey();
            boolean isSelected = code.equals(selectedCode);

            Group cell = new Group();
            cell.setName(code);
            cell.setSize(cellWidth, cellHeight);
            cell.setTransform(true);
            cell.setOrigin(Align.center);

            Image glow = new Image(AtlasRegions.glow);
            float glowW = cellWidth * 1.18f;
            float glowH = cellHeight * 1.55f;
            glow.setSize(glowW, glowH);
            glow.setPosition((cellWidth - glowW) * 0.5f, (cellHeight - glowH) * 0.5f);
            glow.setColor(UIConfig.LANGUAGE_DIALOG_SELECTION_GLOW_COLOR);
            glow.getColor().a = isSelected ? 0.55f : 0f;
            glow.setVisible(isSelected);
            cell.addActor(glow);
            glowByCode.put(code, glow);

            Image bg = new Image(NinePatches.rrect);
            bg.setSize(cellWidth, cellHeight);
            bg.setColor(ITEM_BG_COLOR);
            cell.addActor(bg);
            bgByCode.put(code, bg);

            float iconSize = cellHeight * 0.62f;
            // KÖK NEDEN DÜZELTMESİ: atlas4.findRegion(code) sonucu burada
            // ÖNCEDEN hiç null kontrolü yapılmadan doğrudan new Image(...)'e
            // veriliyordu. Menu.java'daki AYNI bayrak mantığı (setLanguage())
            // bu ihtimale karşı zaten "if (langIcon != null)" ile korunuyordu
            // - LanguageDialog'da bu koruma UNUTULMUŞTU. Bölge her zaman
            // bulunduğunda görünürde bir fark yaratmaz, ama bölge herhangi
            // bir sebeple (ör. bir cihazda atlas4 içeriği eksik/bozuk) null
            // dönerse, önceki kod ilk satırda NullPointerException ile
            // ÇÖKÜYORDU; artık o satır sadece bayrağı atlıyor, geri kalan
            // dil listesi (isim, seçim, kaydırma) çalışmaya devam ediyor.
            TextureAtlas.AtlasRegion flagRegion = atlas4.findRegion(code);
            if (flagRegion != null) {
                Image icon = new Image(flagRegion);
                icon.setScaling(Scaling.fit);
                icon.setSize(iconSize, iconSize);
                icon.setX(cellHeight * 0.3f);
                icon.setY((cellHeight - iconSize) * 0.5f);
                cell.addActor(icon);
            }

            String name = LANGUAGE_NAMES.containsKey(code) ? LANGUAGE_NAMES.get(code) : code.toUpperCase();
            Label label = new Label(name, new Label.LabelStyle(nameStyle));
            label.setColor(isSelected ? ITEM_TEXT_SELECTED_COLOR : ITEM_TEXT_COLOR);

            float labelX = cellHeight * 0.3f + iconSize + cellHeight * 0.35f;
            float maxLabelWidth = cellWidth - labelX - radioSize - radioPad;
            if (label.getWidth() > maxLabelWidth)
                label.setFontScale(maxLabelWidth / label.getWidth());

            label.setX(labelX);
            label.setY((cellHeight - label.getHeight() * label.getFontScaleY()) * 0.5f);
            cell.addActor(label);
            labelByCode.put(code, label);

            Image radio = new Image(radioTex);
            radio.setSize(radioSize, radioSize);
            radio.setX(cellWidth - radioSize - radioPad);
            radio.setY((cellHeight - radioSize) * 0.5f);
            radio.setVisible(isSelected);
            cell.addActor(radio);
            radioByCode.put(code, radio);

            cell.addListener(clickListener);

            table.add(cell).size(cellWidth, cellHeight)
                    .padLeft(pad).padRight(pad)
                    .padTop(pad).padBottom(pad);
            table.row();
        }
        table.pack();

        setContentBackground();
        contentBackground.setSize(content.getWidth(), content.getHeight());
        // Diğer buzlu cam dialoglarla AYNI paylaşılan renk sabitleri
        // kullanılıyor (UIConfig.java'ya yeni bir şey EKLENMEDİ).
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);

        float titleHeight = AtlasRegions.dialog_title.getRegionHeight();
        float titleGap = titleHeight * 0.15f;
        float bottomPad = Math.max(titleGap, cellHeight * 0.12f);

        TextButton.TextButtonStyle confirmStyle = new TextButton.TextButtonStyle();
        String confirmFont = UIConfig.ALERT_DIALOG_BUTTON_USE_SHADOW_FONT
                ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        confirmStyle.font = screen.wordConnectGame.resourceManager.get(confirmFont, BitmapFont.class);
        confirmStyle.fontColor = Color.WHITE;
        confirmStyle.up = new NinePatchDrawable(NinePatches.play_r_up);
        confirmStyle.down = new NinePatchDrawable(NinePatches.play_r_down);
        confirmStyle.disabled = new NinePatchDrawable(NinePatches.play_r_down);

        String confirmText = LanguageManager.bundle != null ? LanguageManager.get("okay") : "OK";
        confirmButton = new TextButton(confirmText, confirmStyle);
        confirmButton.getLabel().setFontScale(UIConfig.ALERT_DIALOG_BUTTON_FONT_SCALE);
        confirmButton.setSize(Math.min(cellWidth, content.getWidth() * 0.72f), cellHeight);
        confirmButton.setOrigin(Align.center);
        confirmButton.setTransform(true);
        confirmButton.setX((content.getWidth() - confirmButton.getWidth()) * 0.5f);
        confirmButton.setY(bottomPad);
        content.addActor(confirmButton);

        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (LanguageManager.locale == null && newCode == null) return;
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });

        // Liste alanı: başlığın altından kapatma butonunun üstüne kadar.
        float scrollAreaBottom = confirmButton.getY() + confirmButton.getHeight() + bottomPad;
        float scrollAreaTop = content.getHeight() - titleHeight * 0.83f - titleGap;
        float scrollAreaHeight = Math.max(cellHeight, scrollAreaTop - scrollAreaBottom);

        // BonusWordsIncompleteDialog'daki İLE AYNI, zaten kanıtlanmış
        // ScrollPane deseni: varsayılan stil, görünmez kaydırma çubuğu -
        // parmakla sürükleyerek kaydırılıyor.
        ScrollPane scrollPane = new ScrollPane(table);
        scrollPane.setScrollbarsVisible(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setSize(tableWidth, scrollAreaHeight);
        scrollPane.setPosition((content.getWidth() - tableWidth) * 0.5f, scrollAreaBottom);
        content.addActor(scrollPane);


        setTitleLabel(LanguageManager.getSelectedLocaleCode() == null ? "Please Select a Language" : LanguageManager.get("language"));
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        // Başlık yazısı üst kenara çok yakın duruyordu - biraz aşağı
        // indirildi ve çok az büyütüldü (diğer 4 dialogla aynı oranlarda).
        titleLabel.setFontScale(titleLabel.getFontScaleX() * 1.10f);
        // BaseDialog.setTitleLabel() ile AYNI merkezleme mantığı - font
        // ölçeği burada büyütüldüğü için getPrefHeight() yeniden alınıyor.
        titleLabel.setY(titleContainer.getHeight() * 0.30f - titleLabel.getPrefHeight() * 0.5f);
        titleLabel.getStyle().font = screen.wordConnectGame.resourceManager.get(ResourceManager.fontSemiBold, BitmapFont.class);
        titleLabel.getStyle().fontColor = ITEM_TEXT_COLOR;
        titleLabel.setColor(ITEM_TEXT_COLOR);
        confirmButton.toFront();
    }

    @Override
    public void show() {
        super.show();
        if (LanguageManager.locale != null && LanguageManager.locale.code != null)
            currentLanguage = LanguageManager.locale.code;
    }


    private void markSelected(String code) {
        if (selectedCode != null) {
            Image prevBg = bgByCode.get(selectedCode);
            Label prevLabel = labelByCode.get(selectedCode);
            Image prevGlow = glowByCode.get(selectedCode);
            Image prevRadio = radioByCode.get(selectedCode);
            if (prevBg != null) prevBg.setColor(ITEM_BG_COLOR);
            if (prevLabel != null) prevLabel.setColor(ITEM_TEXT_COLOR);
            if (prevGlow != null) {
                prevGlow.setVisible(false);
                prevGlow.getColor().a = 0f;
            }
            if (prevRadio != null) prevRadio.setVisible(false);
        }

        Image bg = bgByCode.get(code);
        Label label = labelByCode.get(code);
        Image glow = glowByCode.get(code);
        Image radio = radioByCode.get(code);
        if (bg != null) bg.setColor(ITEM_BG_COLOR);
        if (label != null) label.setColor(ITEM_TEXT_SELECTED_COLOR);
        if (glow != null) {
            glow.setVisible(true);
            glow.getColor().a = 0.55f;
        }
        if (radio != null) radio.setVisible(true);

        selectedCode = code;
    }

    private static Texture createRadioRingTexture(int size, int stroke, Color color) {
        int s = Math.max(8, size);
        int ring = Math.max(2, stroke);
        Pixmap pixmap = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        int cx = s / 2;
        int cy = s / 2;
        int outer = Math.max(2, s / 2 - 1);
        pixmap.setColor(color);
        pixmap.fillCircle(cx, cy, outer);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fillCircle(cx, cy, Math.max(1, outer - ring));
        PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
        Texture tex = new Texture(texData);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return tex;
    }


    // KÖK NEDEN DÜZELTMESİ: Önceden ham bir InputListener kullanılıyordu ve
    // touchUp'ta HER ZAMAN seçim yapıp kapatıyordu - parmak listeyi
    // KAYDIRMAK için sürüklense bile. ScrollPane bir sürüklemeyi algılayıp
    // dokunuşu iptal ettiğinde, ClickListener bunu doğru şekilde anlar ve
    // clicked() metodunu ÇAĞIRMAZ; böylece kaydırma sırasında yanlışlıkla
    // dil seçilip dialog kapanmıyor.
    private ClickListener clickListener = new ClickListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            boolean result = super.touchDown(event, x, y, pointer, button);
            event.getListenerActor().setScale(0.95f);
            return result;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            super.touchUp(event, x, y, pointer, button);
            event.getListenerActor().setScale(1f);
        }

        @Override
        public void clicked(InputEvent event, float x, float y) {
            Actor cellActor = event.getListenerActor();
            String code = cellActor.getName();
            if (code != null) {
                markSelected(code);
                newCode = code;
            }
        }
    };

    String newCode;

    private void setNewLanguage(String code) {

        if (currentLanguage != null && currentLanguage.equals(code)) return;
        screen.setNewLanguage(code);
        getStage().getRoot().setTouchable(Touchable.disabled);
        hide();
    }

    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);
        remove();

        if (currentLanguage == null || (newCode != null && !currentLanguage.equals(newCode))) {
            final String code = newCode;
            // Ekran değişimini Stage.act() / dialog kapanış animasyonu
            // SIRASINDA yapma - aksi halde Actor: Modal sarmalayıcısıyla
            // çökme oluşuyor. Bir sonraki karede çalıştır.
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    screen.setNewLanguage(code);
                    if (callback != null) callback.run();
                }
            });
        }

    }

    @Override
    public boolean navigateBack() {
        if (currentLanguage == null) return false;
        return super.navigateBack();
    }
}