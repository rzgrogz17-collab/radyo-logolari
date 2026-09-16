package ogzapp.wordgame.ui.dialogs.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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

    // NOT: Kullanıcının verdiği referans görsele göre eski koyu lacivert
    // "gece modu" pilleri, açık/buzlu cam kartlara çevrildi. Zemin açıldığı
    // için yazı rengi de KOYUYA çevrildi (beyaz yazı artık okunmazdı).
    // Kenarlık ÇİZGİSİ kullanıcı isteğiyle kaldırıldı - sadece zemin rengi.
    //
    // GÜNCELLEME (kullanıcı isteğiyle, "siyah tablo" referansına göre):
    // Seçili satır artık DOLU YEŞİL zemin DEĞİL - diğerleriyle AYNI buzlu
    // cam zemini kullanıyor (hatta biraz daha "buzlu"/yoğun), etrafında
    // ise siyah referans tablodaki gibi YEŞİL bir ÇERÇEVE (kenarlık) var.
    // Çerçeve, gerçek bir "stroke" dokusu olmadığı için, aynı rrect
    // dokusundan biraz DAHA BÜYÜK bir yeşil kopya + üstüne normal boyutta
    // buzlu-cam kopya bindirerek (aradaki fark kadar) çizgi görünümü elde
    // ediliyor (bkz. aşağıdaki "border" Image'ı).
    private static final Color ITEM_BG_COLOR             = new Color(0xFFFFFF66);
    private static final Color ITEM_BG_SELECTED_COLOR    = new Color(0xFFFFFF8A);
    private static final Color ITEM_BORDER_SELECTED_COLOR = new Color(0x3FAE53FF);
    private static final Color ITEM_TEXT_COLOR           = new Color(0x2C3540FF);
    private static final Color ITEM_TEXT_SELECTED_COLOR  = new Color(0x2C3540FF);

    private Runnable callback;
    private Table table = new Table();
    private String currentLanguage;

    private final Map<String, Image> bgByCode = new HashMap<>();
    private final Map<String, Image> borderByCode = new HashMap<>();
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
        content.setSize(width * 0.94f, height * 0.82f);

        TextureAtlas atlas4 = screen.wordConnectGame.resourceManager.get(ResourceManager.ATLAS_4, TextureAtlas.class);

        float tableWidth = content.getWidth() * 0.9f;
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
        float cellHeight = tableWidth * 0.175f;

        for (Map.Entry<String, Locale> entry : GameConfig.availableLanguages.entrySet()) {
            String code = entry.getKey();
            boolean isSelected = code.equals(selectedCode);

            Group cell = new Group();
            cell.setName(code);
            cell.setSize(cellWidth, cellHeight);
            cell.setTransform(true);
            cell.setOrigin(Align.center);

            // Kullanıcı isteğiyle: seçili satırın etrafında (siyah referans
            // tablodaki gibi) YEŞİL bir ÇERÇEVE var. Gerçek bir "sadece
            // çizgi" dokusu projede olmadığından, AYNI rrect dokusundan
            // hücreden biraz DAHA BÜYÜK bir yeşil kopya buraya (bg'den
            // ÖNCE, yani ARKAYA) ekleniyor; üzerine tam boy buzlu-cam bg
            // bindiğinde sadece kenarlardaki ince pay yeşil çerçeve gibi
            // görünüyor. Sadece seçili hücrede görünür.
            float borderThickness = cellHeight * 0.07f;
            Image border = new Image(NinePatches.rrect);
            border.setSize(cellWidth + borderThickness * 2f, cellHeight + borderThickness * 2f);
            border.setPosition(-borderThickness, -borderThickness);
            border.setColor(ITEM_BORDER_SELECTED_COLOR);
            border.setVisible(isSelected);
            cell.addActor(border);
            borderByCode.put(code, border);

            // Kullanıcı isteğiyle kenarlık ÇİZGİSİ kaldırıldı - sadece
            // açık/hafif saydam "buzlu cam" zemin kaldı, seçili/seçili
            // olmayan ayrımı yalnızca zemin rengiyle yapılıyor.
            Image bg = new Image(NinePatches.rrect);
            bg.setSize(cellWidth, cellHeight);
            bg.setColor(isSelected ? ITEM_BG_SELECTED_COLOR : ITEM_BG_COLOR);
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
            float maxLabelWidth = cellWidth - labelX - cellHeight * 0.2f;
            if (label.getWidth() > maxLabelWidth)
                label.setFontScale(maxLabelWidth / label.getWidth());

            label.setX(labelX);
            label.setY((cellHeight - label.getHeight() * label.getFontScaleY()) * 0.5f);
            cell.addActor(label);
            labelByCode.put(code, label);

            cell.addListener(clickListener);

            table.add(cell).size(cellWidth, cellHeight).padBottom(pad);
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
        float bottomGap = titleGap;

        // Liste alanı: başlığın altından panelin alt kenarına kadar kalan
        // TÜM boşluk - içerik bundan uzun olursa ScrollPane kaydırıyor.
        float scrollAreaTop = content.getHeight() - titleHeight * 0.83f - titleGap;
        float scrollAreaHeight = scrollAreaTop - bottomGap;

        // BonusWordsIncompleteDialog'daki İLE AYNI, zaten kanıtlanmış
        // ScrollPane deseni: varsayılan stil, görünmez kaydırma çubuğu -
        // parmakla sürükleyerek kaydırılıyor.
        ScrollPane scrollPane = new ScrollPane(table);
        scrollPane.setScrollbarsVisible(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setSize(tableWidth, scrollAreaHeight);
        scrollPane.setPosition((content.getWidth() - tableWidth) * 0.5f, bottomGap);
        content.addActor(scrollPane);


        setTitleLabel(LanguageManager.getSelectedLocaleCode() == null ? "Please Select a Language" : LanguageManager.get("language"));
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        // Başlık yazısı üst kenara çok yakın duruyordu - biraz aşağı
        // indirildi ve çok az büyütüldü (diğer 4 dialogla aynı oranlarda).
        titleLabel.setFontScale(titleLabel.getFontScaleX() * 1.10f);
        // BaseDialog.setTitleLabel() ile AYNI merkezleme mantığı - font
        // ölçeği burada büyütüldüğü için getPrefHeight() yeniden alınıyor.
        titleLabel.setY(titleContainer.getHeight() * 0.30f - titleLabel.getPrefHeight() * 0.5f);

        setCloseButton();

        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });

        if(LanguageManager.locale == null)
            closeButton.setVisible(false);
    }

    @Override
    public void show() {
        super.show();
        if(LanguageManager.locale != null && LanguageManager.locale.code != null) currentLanguage = LanguageManager.locale.code;
    }


    private void markSelected(String code) {
        if (selectedCode != null) {
            Image prevBg = bgByCode.get(selectedCode);
            Label prevLabel = labelByCode.get(selectedCode);
            Image prevBorder = borderByCode.get(selectedCode);
            if (prevBg != null) prevBg.setColor(ITEM_BG_COLOR);
            if (prevLabel != null) prevLabel.setColor(ITEM_TEXT_COLOR);
            if (prevBorder != null) prevBorder.setVisible(false);
        }

        Image bg = bgByCode.get(code);
        Label label = labelByCode.get(code);
        Image border = borderByCode.get(code);
        if (bg != null) bg.setColor(ITEM_BG_SELECTED_COLOR);
        if (label != null) label.setColor(ITEM_TEXT_SELECTED_COLOR);
        if (border != null) border.setVisible(true);

        selectedCode = code;
    }


    // KÖK NEDEN DÜZELTMESİ: Önceden ham bir InputListener kullanılıyordu ve
    // touchUp'ta HER ZAMAN seçim yapıp kapatıyordu - parmak listeyi
    // KAYDIRMAK için sürüklense bile. ScrollPane bir sürüklemeyi algılayıp
    // dokunuşu iptal ettiğinde, ClickListener bunu doğru şekilde anlar ve
    // clicked() metodunu ÇAĞIRMAZ; böylece kaydırma sırasında yanlışlıkla
    // dil seçilip dialog kapanmıyor.
    private ClickListener clickListener = new ClickListener(){
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
            if(code != null) {
                markSelected(code);
                newCode = code;
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
                //setNewLanguage(target.getName());
            }
        }
    };

    String newCode;

    private void setNewLanguage(String code){

        if(currentLanguage != null && currentLanguage.equals(code)) return;
        screen.setNewLanguage(code);
        getStage().getRoot().setTouchable(Touchable.disabled);
        hide();
    }

    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);
        remove();

        if(currentLanguage == null || (newCode != null && !currentLanguage.equals(newCode))){
            screen.setNewLanguage(newCode);
            if(callback != null)callback.run();
        }

    }

    @Override
    public boolean navigateBack() {
        if(currentLanguage == null) return false;
        return super.navigateBack();
    }
}
