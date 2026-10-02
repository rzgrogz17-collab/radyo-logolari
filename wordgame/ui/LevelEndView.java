package ogzapp.wordgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pools;

import ogzapp.wordgame.actions.Interpolation;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.events.ShowDictionaryEvent;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.managers.HintManager;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.model.Constants;
import ogzapp.wordgame.model.GameData;
import ogzapp.wordgame.screens.BaseScreen;
import ogzapp.wordgame.ui.dialogs.DictionaryDialog;
import ogzapp.wordgame.ui.dialogs.wheel.WheelDialog;
import ogzapp.wordgame.util.UiUtil;

public class LevelEndView extends Group {

    protected TextButton nextLevel;
    protected Image nextLevelShadow;
    private ShowDictionaryEvent dictionaryEventListener;
    protected BaseScreen screen;
    protected float coinViewX, coinViewY;

    protected ImageButton dictButton;
    protected Label rewardLabel;
    protected Group rewardLabelContainer;
    protected Runnable nextLevelCallback;

    // --- Seviye ödül kutusu (her 10 seviyede bir 20 coin) ---
    private static final int MILESTONE_LEVEL_INTERVAL          = 10;
    private static final int MILESTONE_REWARD_COINS            = 20;
    private static final Color MILESTONE_SEGMENT_FILLED_COLOR  = new Color(0xFFFFFFFF);
    private static final Color MILESTONE_SEGMENT_EMPTY_COLOR   = new Color(0xFFFFFF40);
    private static final Color MILESTONE_TEXT_COLOR            = Color.WHITE;
    private static final Color MILESTONE_MIST_COLOR            = new Color(0xF5F8FCFF);
    private static final Color MILESTONE_REWARD_TEXT_COLOR     = new Color(0xFFC940FF);

    // =====================================================================
    // "10 SEVİYEDE BİR ÖDÜL" SİSTEMİ - ANA AÇMA/KAPAMA ANAHTARLARI
    // Hepsi true/false. Aşağıdaki her anahtar, tek başına, başka hiçbir
    // yeri değiştirmeden açılıp kapatılabilir. ŞU AN HEPSİ AÇIK (true).
    // =====================================================================

    // ANA ANAHTAR: "10 seviye tamamla, ödül al" sisteminin tamamı (halka/daire,
    // içindeki hediye kutusu, ilerleme yazısı, coin ödülü - her şey).
    // false yapıldığında rozet hiç oluşturulmaz/gösterilmez ve ödül verilmez.
    private static final boolean MILESTONE_FEATURE_ENABLED      = true;

    // Halkanın/hediye kutusunun etrafındaki sürekli dönen "sis efekti"
    // (mist ring) - true/false ile açılıp kapatılabilen bir anahtar.
    private static final boolean MILESTONE_MIST_EFFECT_ENABLED = true;

    // Hediye kutusu açıldığında halkadan dışa doğru yayılan "dalga efekti"
    // (wave/ring-pulse patlaması) - true/false ile açılıp kapatılabilen
    // bir anahtar.
    private static final boolean MILESTONE_WAVE_EFFECT_ENABLED = true;

    // Referans görseldeki gibi: koyu/lacivert yazılı, beyaz/aydınlık pill
    // buton (Sonraki Seviye ve X. Seviye butonları için).
    private static final Color LEVEL_BUTTON_TEXT_COLOR    = new Color(0x16324FFF);
    private static final Color LEVEL_BUTTON_BG_COLOR       = new Color(0xF7F9FCFF);
    private static final Color LEVEL_BUTTON_BG_DOWN_COLOR  = new Color(0xE2E7EEFF);

    // --- Tam "hap" (pill/stadium) şekilli buton dokusu üretimi ---
    // NinePatches.rrect atlas dokusuna bağlı kalmıyoruz (o dokunun köşe
    // yarıçapı referans görseldeki kadar yuvarlak olmayabilir); bunun
    // yerine köşeleri MATEMATİKSEL olarak tam yarım daire olan bir doku
    // koddan üretiliyor - böylece görünüm dış bir asset dosyasına değil,
    // doğrudan koda bağlı ve kesin olarak referansla eşleşiyor.
    private static final float PILL_HEIGHT_TO_TEXT_RATIO = 2.2f; // yazı, toplam yüksekliğin ~%45'ini kaplar
    private static final int PILL_TEXTURE_SUPERSAMPLE = 4;       // kenarları yumuşatmak için yüksek çözünürlükte çizip küçültüyoruz
    private static final int PILL_STRETCH_PX = 4;                // ortada genişleyebilen ince düz şerit

    private static float computePillButtonHeight(BitmapFont font, float fontScale) {
        return font.getLineHeight() * fontScale * PILL_HEIGHT_TO_TEXT_RATIO;
    }

    private static NinePatch createPillNinePatch(float height, Color color) {
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

        // KÖK NEDEN DÜZELTMESİ (context-loss dayanıklılığı): Burada ÖNCEDEN
        // düz "new Texture(small); small.dispose();" kullanılıyordu. Bu,
        // milestoneRingTexture için zaten tespit edilip düzeltilmiş olan
        // KLASİK "unmanaged doku" hatasının BİREBİR AYNISIYDI (bkz.
        // setMilestoneBadge() yorumları) - sadece BAŞKA bir dokuda
        // (bu butonda) hâlâ DÜZELTİLMEMİŞ halde duruyordu. Böyle üretilen
        // bir Texture, Android'de uygulama arka plana alınıp GL context
        // kaybolduğunda libGDX'in otomatik doku kurtarma sistemine DAHİL
        // OLMUYOR; doku geçersiz bir GL ID ile "hayalet" halde kalıyor ve
        // o ID daha sonra BAŞKA bir dokuya (ör. atlas4'teki bayrak/ödül
        // dokusuna ya da hiçbir şeye) yeniden atanınca bu buton/gölge o
        // dokunun içeriğini (karışık bayrak+yazı görüntüsü) ya da tamamen
        // boş/soluk bir görüntü gösteriyordu - "Sonraki Seviye" butonunda
        // ve 10 seviyelik ödül halkasının çevresinde bildirilen görsel
        // bozulmanın gerçek kök nedeni tam olarak buydu.
        //
        // Çözüm, ring dokusuyla AYNI: pixmap ASLA dispose edilmiyor ve
        // managed=true ile bir PixmapTextureData'ya sarılıp Texture'a öyle
        // veriliyor - böylece bu doku da libGDX'in kendi otomatik
        // context-kurtarma listesine dahil oluyor ve context kaybından
        // sonra GEÇERLİ, yeni bir GL ID ile kendini doğru içerikle yeniden
        // oluşturuyor. Pixmap, Texture -> PixmapTextureData zinciri
        // üzerinden zaten canlı tutuluyor (NinePatch bu Texture'ı referans
        // olarak tutuyor), bu yüzden ayrı bir alan/liste gerekmiyor.
        PixmapTextureData texData = new PixmapTextureData(small, small.getFormat(), false, false, true);
        Texture tex = new Texture(texData);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        int radius = h / 2;
        return new NinePatch(tex, radius, radius, 0, 0);
    }

    private Group milestoneContainer;
    private Image milestoneGiftIcon;
    private Image milestoneRingImage;      // tek parça, tam dairesel halka (5 bölümlü)
    private Texture milestoneRingTexture;  // halkanın o anki dokusu (dolu/boş durumu değişince yeniden üretilir)
    // Halkanın kalıcı/tekrar-kullanılan piksel verisi. Bu pixmap ASLA
    // dispose edilmiyor ve managed TextureData tarafından TUTULUYOR - bu
    // sayede Android'de uygulama arka plana alınıp GL context kaybolduğunda
    // (ekran kilidi, uygulama değiştirme vb.) libGDX'in kendi otomatik doku
    // kurtarma sistemi bu pixmap'ten dokuyu doğru şekilde yeniden yükleyip
    // FARKLI/GEÇERLİ bir GL ID ile geri getirebiliyor. (bkz. renderRingPixmap
    // ve setMilestoneBadge/updateMilestoneBadge yorumları)
    private Pixmap milestoneRingPixmap;
    private PixmapTextureData milestoneRingTextureData;
    private float milestoneRingRadius;     // halkanın orta yarıçapı (dünya birimi)
    private float milestoneRingThickness;  // halka kalınlığı (dünya birimi)
    private Label milestoneProgressLabel;
    private Image milestoneMist1, milestoneMist2, milestoneMist3;
    private boolean milestoneWheelAvailable;

    public LevelEndView(BaseScreen screen, float width, float height) {
        setSize(width, height);
        this.screen = screen;

        setBackground();
        setDictButton(); // buton ekleme metodu

        String btnFontName = UIConfig.LEVEL_END_VIEW_NEXT_LEVEL_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        BitmapFont btnFont = screen.wordConnectGame.resourceManager.get(btnFontName, BitmapFont.class);
        float pillHeight = computePillButtonHeight(btnFont, UIConfig.LEVEL_END_VIEW_NEXT_LEVEL_BUTTON_FONT_SCALE);

        NinePatch btnUpPatch = createPillNinePatch(pillHeight, LEVEL_BUTTON_BG_COLOR);
        NinePatch btnDownPatch = createPillNinePatch(pillHeight, LEVEL_BUTTON_BG_DOWN_COLOR);
        setButton(LanguageManager.get("next_level"), new NinePatchDrawable(btnUpPatch), new NinePatchDrawable(btnDownPatch));

        nextLevel.addListener(changeListener);
    }

    protected ChangeListener changeListener = new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, Actor actor) {
            screen.topPanel.coinView.cancel(true);
            putBackCoinView();
            getStage().getRoot().setTouchable(Touchable.disabled);
            DictionaryDialog.words = null;
            hide();
        }
    };

    protected void setDictButton() {
        // --- İNGİLİZCE İÇİN BUTON GÖSTERME ---
        if (LanguageManager.locale != null && LanguageManager.locale.code.equals("en")) {
            return; // İngilizce seçiliyse buton eklenmez
        }
        // --- DİĞER DİLLER İÇİN NORMAL İŞLEYİŞ ---
        if (dictButton == null && LanguageManager.wordMeaningProviderMap.containsKey(LanguageManager.locale.code)) {
            dictButton = new ImageButton(new TextureRegionDrawable(AtlasRegions.btn_dictionary_up), new TextureRegionDrawable(AtlasRegions.btn_dictionary_down));
            dictButton.setOrigin(Align.center);
            dictButton.setTransform(true);
            dictButton.setScale(0);
            dictButton.setX((getWidth() - dictButton.getWidth()) * 0.5f);
            dictButton.setY(getHeight() - dictButton.getHeight() - screen.topPanel.coinView.getY());
            addActor(dictButton);

            dictButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (dictionaryEventListener != null) {
                        dictionaryEventListener.showDictionary(null);
                    }
                }
            });
        }
    }

    public void setButton(String text, Drawable up, Drawable down) {
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        String font = UIConfig.LEVEL_END_VIEW_NEXT_LEVEL_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        buttonStyle.font = screen.wordConnectGame.resourceManager.get(font, BitmapFont.class);
        buttonStyle.fontColor = LEVEL_BUTTON_TEXT_COLOR;
        buttonStyle.up = up;
        buttonStyle.down = down;

        nextLevel = new TextButton(text, buttonStyle);
        nextLevel.getLabel().setFontScale(UIConfig.LEVEL_END_VIEW_NEXT_LEVEL_BUTTON_FONT_SCALE);
        nextLevel.setWidth(getWidth() * ((UiUtil.isScreenWide() ? 0.55f : 0.6f)) * UIConfig.LEVEL_END_VIEW_NEXT_LEVEL_BUTTON_WIDTH_COEF);
        // Yüksekliği, üretilen "hap" dokusunun gerçek piksel yüksekliğiyle
        // BİREBİR eşleştiriyoruz - aksi halde nine-patch dikeyde esneyip
        // uçlar tam yarım daire olmaktan çıkar (oval/ezik görünür).
        nextLevel.setHeight(up.getMinHeight());
        nextLevel.setX((getWidth() - nextLevel.getWidth()) * 0.5f);
        nextLevel.setY(getHeight() * 0.1f);
        nextLevel.setTransform(true);
        nextLevel.setOrigin(Align.center);

        // Buton dokusuyla BİREBİR AYNI üretim yöntemiyle (createPillNinePatch)
        // bir "arkalık" oluşturuyoruz - böylece köşeleri her zaman butonla
        // TAM AYNI oranda yuvarlak olur. Eski "NinePatches.round_rect_shadow"
        // denemesinde, büyüttüğümüz buton boyutuyla orantısız kalıp neredeyse
        // kare/kutu gibi görünüyordu; artık bu mümkün değil. Tamamen opak
        // (şeffaf DEĞİL), düz beyaz.
        float shadowMargin = up.getMinHeight() * 0.18f;
        float shadowHeight = up.getMinHeight() + shadowMargin;
        NinePatch shadowPatch = createPillNinePatch(shadowHeight, Color.WHITE);
        nextLevelShadow = new Image(new NinePatchDrawable(shadowPatch));
        nextLevelShadow.setWidth(nextLevel.getWidth() + shadowMargin);
        nextLevelShadow.setHeight(shadowHeight);
        nextLevelShadow.setPosition(nextLevel.getX() - shadowMargin * 0.5f, nextLevel.getY() - shadowMargin * 0.5f);
        nextLevelShadow.setOrigin(Align.center);
        addActor(nextLevelShadow);

        addActor(nextLevel);
    }

    protected void setCoinView() {
        coinViewX = screen.topPanel.coinView.getX();
        coinViewY = screen.topPanel.coinView.getY();

        screen.topPanel.coinView.remove();
        screen.topPanel.coinView.setOrigin(Align.center);

        screen.topPanel.coinView.setPosition(screen.topPanel.getX() + coinViewX, screen.topPanel.getY() + coinViewY - screen.topPanel.getHeight());
        if (screen.topPanel.coinView.plus != null) screen.topPanel.coinView.plus.setDisabled(true);
        addActor(screen.topPanel.coinView);
    }

    public void addNextLevelListener(Runnable listener) {
        nextLevelCallback = listener;
    }

    public void addDictionaryShowListener(ShowDictionaryEvent dictionaryEventListener) {
        this.dictionaryEventListener = dictionaryEventListener;
    }

    protected void setRewardText(String text) {
        if (screen.topPanel.coinView.isCancelled()) return;
        if (rewardLabel == null) {
            String font = UIConfig.LEVEL_FINISHED_VIEW_COINS_EARNED_TEXT_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
            Label.LabelStyle wordTitlelabelStyle = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(font, BitmapFont.class), UIConfig.LEVEL_FINISHED_VIEW_COINS_EARNED_TEXT_COLOR);

            rewardLabel = new Label(text, wordTitlelabelStyle);

            rewardLabelContainer = new Group();
            rewardLabelContainer.addActor(rewardLabel);
            addActor(rewardLabelContainer);
        } else {
            rewardLabel.setText(text);
            rewardLabelContainer.setVisible(true);
            rewardLabelContainer.getColor().a = 1f;
        }

        GlyphLayout glyphLayout = Pools.obtain(GlyphLayout.class);
        glyphLayout.setText(rewardLabel.getStyle().font, text);
        rewardLabelContainer.setSize(glyphLayout.width, glyphLayout.height);
        Pools.free(glyphLayout);
        rewardLabelContainer.setOrigin(Align.center);
        rewardLabelContainer.setScale(0);
        rewardLabelContainer.setX((getWidth() - rewardLabelContainer.getWidth()) * 0.5f);

        float buttonTop = nextLevel.getY() + nextLevel.getHeight();
        rewardLabelContainer.setY(buttonTop + (0 - buttonTop) * 0.5f - rewardLabelContainer.getHeight() * 0.5f); // cupContainer yoksa 0 kullan
    }

    protected void setCoinRewardView(int count) {
        Vector2 pos = rewardLabel.localToActorCoordinates(screen.topPanel.coinView, new Vector2(rewardLabel.getWidth() * 0.5f, rewardLabel.getHeight() * 1.3f));
        screen.topPanel.coinView.createCoinAnimation(count, pos.x, pos.y, coinAnimComplete);
    }

    private Runnable coinAnimComplete = new Runnable() {
        @Override
        public void run() {
            Action fadeOut = Actions.fadeOut(0.3f);
            RunnableAction runnableAction = new RunnableAction();
            runnableAction.setRunnable(new Runnable() {
                @Override
                public void run() {
                    putBackCoinView();
                }
            });
            screen.topPanel.coinView.addAction(new SequenceAction(fadeOut, runnableAction));
            rewardLabelContainer.addAction(Actions.fadeOut(0.3f));
        }
    };

    protected void putBackCoinView() {
        if (screen.topPanel.coinView.getParent() == this) {
            screen.topPanel.coinView.remove();
            screen.topPanel.coinView.setPosition(coinViewX, coinViewY);
            screen.topPanel.addActor(screen.topPanel.coinView);
            screen.topPanel.coinView.getColor().a = 1;
            if (rewardLabelContainer != null) rewardLabelContainer.setVisible(false);
        }
    }

    public void setBackground() {
        Image bg = new Image(NinePatches.rect);
        bg.setSize(getWidth(), getHeight());
        bg.setColor(UIConfig.LEVEL_FINISHED_VIEW_BG_COLOR);
        addActor(bg);
    }

    // "Sonraki seviye" ekranının ortasında duran, her MILESTONE_LEVEL_INTERVAL
    // (10) seviyede bir dolan ve o an MILESTONE_REWARD_COINS (20) coin ödülü
    // veren hediye kutusu rozeti. İlk çağrıda oluşturulur, sonrasında sadece
    // güncellenir (rewardLabel'daki lazy-init deseniyle aynı mantık).
    //
    // Tasarım: koyu arka plan kutusu YOK. Hediye kutusunun etrafında TAM,
    // TEK PARÇA bir halka var (küçük çentik/tire parçalarından ÖRÜLMÜYOR -
    // eski yöntemde 30 küçük düz parça birleştirilmeye çalışılıyordu ve
    // aralarında görünür dikişler/boşluklar oluyordu). Halka piksel piksel,
    // matematiksel çember denklemiyle üretiliyor (createPillNinePatch'teki
    // "yüksek çözünürlükte çiz, küçült" tekniğinin aynısı), bu yüzden hem
    // dış hem iç kenarı pürüzsüz bir çember. Dolgu artık 5 ayrı bölüm/boşluk
    // DEĞİL, tamamen YÜZDEYE göre kesintisiz tek bir yay (normal dairesel
    // ilerleme çubuğu gibi). Dolu kısım düz opak beyaz değil, yarı saydam +
    // ince bir parlaklık şeridi olan "buzlu cam" (glassmorphism) görünümünde.
    private static final int MILESTONE_RING_TEXTURE_SIZE = 220;       // sabit referans çözünürlük - halkanın dünya boyutundan BAĞIMSIZ
    private static final int MILESTONE_RING_TEXTURE_SUPERSAMPLE = 3;  // kenarları yumuşatmak için yüksek çözünürlükte çizip küçültüyoruz

    // Halkayı (tam/dolu görünen, tek parça daire) piksel piksel üretir.
    // Dolgu, "filledSections/MILESTONE_LEVEL_INTERVAL" oranına göre YÜZDE
    // bazlı, kesintisiz bir yay olarak çizilir (buzlu cam görünümünde beyaz);
    // kalanı boş (yarı saydam gri) renkte olur. Bu, sadece durum değiştiğinde
    // (nadiren) çağrılan tek seferlik bir üretim - her karede ÇALIŞMIYOR, bu
    // yüzden piksel bazlı hesaplama performans sorunu yaratmıyor.
    // ÖNEMLİ - context kaybına (context loss) dayanıklılık: Bu metot artık
    // YENİ bir Pixmap OLUŞTURMUYOR, verilen (kalıcı/tekrar kullanılan)
    // "target" Pixmap'ının içeriğini YERİNDE güncelliyor. target, Texture'ın
    // "managed" TextureData'sı tarafından TUTULAN (retained) pixmap ile
    // AYNI nesne olmalı - böylece Android'de uygulama arka plana alınıp
    // geri dönüldüğünde (GL context kaybı/EGL surface yeniden oluşturma)
    // libGDX'in kendi otomatik doku kurtarma mekanizması bu pixmap'ten
    // doğru içeriği tekrar GPU'ya yükleyebiliyor. Aksi halde (elle
    // oluşturulmuş, "unmanaged" bir doku), context kaybından sonra doku
    // GEÇERSİZ bir GL ID'siyle "hayalet" halde kalır ve o ID başka bir
    // dokuya (ör. fontun kendi atlas dokusuna) yeniden atanırsa, halka/buton
    // o dokunun içeriğini (rastgele harfler) gösterir - rapor edilen "her
    // zaman olabilen, bir kere olunca oyun kapanana kadar süren" bozulmanın
    // gerçek kök nedeni tam olarak buydu.
    private void renderRingPixmap(int filledSections, Pixmap target) {
        float outerRadius = milestoneRingRadius + milestoneRingThickness * 0.5f;
        float innerRadius = milestoneRingRadius - milestoneRingThickness * 0.5f;
        int supersample = MILESTONE_RING_TEXTURE_SUPERSAMPLE;
        int size = MILESTONE_RING_TEXTURE_SIZE * supersample;
        float cx = size * 0.5f;
        float cy = size * 0.5f;
        // Halkanın gerçek dünya oranını (iç/dış yarıçap) sabit referans
        // dokuya haritalıyoruz - böylece doku her zaman dış kenara tam
        // oturur, halkanın gerçek dünya boyutu ne olursa olsun.
        float pxPerUnit = (size * 0.5f) / outerRadius;
        float outerPx = outerRadius * pxPerUnit;
        float innerPx = innerRadius * pxPerUnit;
        float ringThicknessPx = outerPx - innerPx;

        // Artık 5 ayrı bölüm + boşluk YOK - tamamen YÜZDEYE göre, kesintisiz
        // tek bir dolgu yayı (normal bir dairesel ilerleme çubuğu gibi).
        float filledAngleDegrees = 360f * filledSections / (float) MILESTONE_LEVEL_INTERVAL;

        int emptyRGBA = Color.rgba8888(MILESTONE_SEGMENT_EMPTY_COLOR.r, MILESTONE_SEGMENT_EMPTY_COLOR.g, MILESTONE_SEGMENT_EMPTY_COLOR.b, MILESTONE_SEGMENT_EMPTY_COLOR.a);

        Pixmap big = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - cx;
                float dyWorld = cy - (y + 0.5f); // dünya koordinatında yukarı = pozitif
                float dist = (float) Math.sqrt(dx * dx + dyWorld * dyWorld);
                if (dist < innerPx || dist > outerPx) continue; // halkanın dışı/içi - saydam kalsın

                // Standart matematik açısı: 0°=sağ, 90°=yukarı, saat yönünün TERSİNE
                // artar - eski çentik koduyla AYNI konvansiyon (90° ofsetle üst
                // noktadan başlayıp sola doğru sarıyor), böylece görsel konum
                // öncekiyle birebir tutarlı kalıyor.
                float angle = (float) Math.toDegrees(Math.atan2(dyWorld, dx));
                if (angle < 0) angle += 360f;
                float angleFromTop = angle - 90f;
                if (angleFromTop < 0) angleFromTop += 360f;

                if (angleFromTop < filledAngleDegrees) {
                    // "Buzlu cam" (glassmorphism) hissi: düz opak beyaz yerine,
                    // yarı saydam + halkanın kalınlığı boyunca yumuşak bir
                    // parlaklık şeridi olan bir beyaz. t: 0=iç kenar, 1=dış
                    // kenar. Şerit dış kenara yakın bir yerde (t≈0.62) tepe
                    // yapıyor - eğri (kıvrımlı) bir cam yüzeyindeki ışık
                    // yansıması gibi.
                    float t = (dist - innerPx) / ringThicknessPx;
                    float highlight = 1f - Math.abs(t - 0.62f) / 0.62f;
                    if (highlight < 0f) highlight = 0f;
                    if (highlight > 1f) highlight = 1f;
                    float alpha = 0.46f + 0.42f * highlight; // 0.46 (kenarlarda) - 0.88 (parlak şeritte)
                    big.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, alpha));
                } else {
                    big.drawPixel(x, y, emptyRGBA);
                }
            }
        }

        int outSize = MILESTONE_RING_TEXTURE_SIZE;
        big.setFilter(Pixmap.Filter.BiLinear);
        target.setFilter(Pixmap.Filter.BiLinear);
        // Önceki karenin içeriğini temizleyip yenisini çiziyoruz (target
        // kalıcı/tekrar kullanılan bir pixmap, her seferinde en baştan
        // oluşturulmuyor).
        target.setColor(0, 0, 0, 0);
        target.fill();
        target.drawPixmap(big, 0, 0, size, size, 0, 0, outSize, outSize);
        big.dispose();
    }

    private void setMilestoneBadge() {
        milestoneContainer = new Group();
        milestoneContainer.setTransform(true);

        float giftSize = getWidth() * 0.20f;
        float ringThickness = giftSize * 0.17f;
        float ringRadius = giftSize * 0.78f;
        float ringOuterRadius = ringRadius + ringThickness * 0.5f;
        float containerWidth = ringOuterRadius * 2f + 6f;

        // Halkanın üretimi (renderRingPixmap) ve güncellemesi
        // (updateMilestoneBadge) için bu geometriyi sınıf alanlarında
        // saklıyoruz.
        milestoneRingRadius = ringRadius;
        milestoneRingThickness = ringThickness;

        String font = ResourceManager.fontSemiBoldShadow;
        Label.LabelStyle progressStyle = new Label.LabelStyle(
                screen.wordConnectGame.resourceManager.get(font, BitmapFont.class),
                MILESTONE_TEXT_COLOR);

        milestoneProgressLabel = new Label("0/" + MILESTONE_LEVEL_INTERVAL, progressStyle);
        milestoneProgressLabel.setFontScale(0.8f);
        milestoneProgressLabel.setAlignment(Align.center);
        milestoneProgressLabel.setWidth(containerWidth);

        // "3/5" yazısının gerçek (esnetilmeden önceki) genişliği - hap
        // (pill) zemini bu ölçüye göre boyutlandırılıyor.
        GlyphLayout counterLayout = Pools.obtain(GlyphLayout.class);
        counterLayout.setText(milestoneProgressLabel.getStyle().font, milestoneProgressLabel.getText().toString());
        float counterTextWidth = counterLayout.width * milestoneProgressLabel.getFontScaleX();
        Pools.free(counterLayout);

        float pillPaddingX = giftSize * 0.22f;
        float pillHeight = milestoneProgressLabel.getHeight() * 1.7f;
        float pillWidth = Math.min(containerWidth, counterTextWidth + pillPaddingX * 2f);

        float labelGap = giftSize * 0.16f;
        float ringCenterX = containerWidth * 0.5f;
        float ringCenterY = pillHeight + labelGap + ringOuterRadius;
        float containerHeight = ringCenterY + ringOuterRadius;

        // Halkadan dışa doğru yayılan "sis dalgası" efekti: dalga halka
        // kenarında en belirgin halinde başlıyor, dışa doğru yayıldıkça
        // (büyüdükçe) görünürlüğü sürekli azalıyor ve tamamen kayboluyor.
        // Önce belirip sonra kaybolan bir "kalp atışı" hissi yok; çok yavaş
        // bir döngüde (6sn) çalışıyor.
        if (MILESTONE_MIST_EFFECT_ENABLED) {
            float mistBaseSize = ringOuterRadius * 2.3f;
            milestoneMist1 = createMistRing(mistBaseSize);
            milestoneMist2 = createMistRing(mistBaseSize);
            milestoneMist3 = createMistRing(mistBaseSize);
            positionCentered(milestoneMist1, ringCenterX, ringCenterY);
            positionCentered(milestoneMist2, ringCenterX, ringCenterY);
            positionCentered(milestoneMist3, ringCenterX, ringCenterY);
            milestoneContainer.addActor(milestoneMist1);
            milestoneContainer.addActor(milestoneMist2);
            milestoneContainer.addActor(milestoneMist3);
            startMistAnimation(milestoneMist1, 0f);
            startMistAnimation(milestoneMist2, 2f);
            startMistAnimation(milestoneMist3, 4f);
        }

        // Hediye kutusunun etrafını saran ilerleme halkası: TEK PARÇA, TAM
        // (dolu) bir daire - küçük çentik/tire parçalarından ÖRÜLMÜYOR, bu
        // yüzden dikiş/boşluk görünmüyor. Yine de 5 eşit bölüme ayrılmış
        // durumda; görünür boşluk SADECE bu 5 bölümün arasında.
        //
        // ÖNEMLİ (context-loss dayanıklılığı): milestoneRingPixmap KALICI -
        // asla dispose edilmiyor. managed=true ile bir PixmapTextureData'ya
        // sarılıp Texture'a öyle veriliyor; bu sayede libGDX bu dokuyu kendi
        // otomatik context-kurtarma listesine dahil ediyor (AssetManager'dan
        // yüklenmiş dokularla AYNI şekilde). Önceki yöntemde (managed
        // OLMAYAN, elle oluşturulmuş doku) uygulama arka plana alınıp GL
        // context kaybolduğunda doku geçersiz bir GL ID ile "hayalet" halde
        // kalabiliyordu; o ID başka bir dokuya (ör. fontun kendi atlas
        // dokusuna) yeniden atanınca halka/buton o dokunun içeriğini
        // (rastgele harfler) gösteriyordu - rapor edilen, "her zaman
        // olabilen, bir kere olunca oyun kapanana kadar süren" bozulmanın
        // gerçek kök nedeni tam olarak buydu.
        int ringTexSize = MILESTONE_RING_TEXTURE_SIZE;
        milestoneRingPixmap = new Pixmap(ringTexSize, ringTexSize, Pixmap.Format.RGBA8888);
        renderRingPixmap(0, milestoneRingPixmap);
        milestoneRingTextureData = new PixmapTextureData(milestoneRingPixmap, milestoneRingPixmap.getFormat(), false, false, true);
        milestoneRingTexture = new Texture(milestoneRingTextureData);
        milestoneRingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        milestoneRingImage = new Image(milestoneRingTexture);
        milestoneRingImage.setSize(ringOuterRadius * 2f, ringOuterRadius * 2f);
        positionCentered(milestoneRingImage, ringCenterX, ringCenterY);
        milestoneContainer.addActor(milestoneRingImage);


        // Hediye kutusu ikonu, halkanın tam ortasında.
        milestoneGiftIcon = new Image(AtlasRegions.giftbox_closed);
        milestoneGiftIcon.setSize(giftSize, giftSize);
        milestoneGiftIcon.setOrigin(Align.center);
        milestoneGiftIcon.setPosition(ringCenterX - giftSize * 0.5f, ringCenterY - giftSize * 0.5f);
        milestoneGiftIcon.setTouchable(Touchable.disabled);
        milestoneGiftIcon.addListener(milestoneWheelListener);
        milestoneContainer.addActor(milestoneGiftIcon);

        // Alt yazı ("3/5"), halkanın hemen altında, 5_Bölüm.html'deki gibi
        // hafif saydam/camsı bir hap (pill) zemin üzerinde.
        Image counterPillBorder = new Image(NinePatches.rrect);
        counterPillBorder.setSize(pillWidth + 2f, pillHeight + 2f);
        counterPillBorder.setPosition((containerWidth - counterPillBorder.getWidth()) * 0.5f, -1f);
        counterPillBorder.setColor(1f, 1f, 1f, 0.3f);
        milestoneContainer.addActor(counterPillBorder);

        Image counterPillBg = new Image(NinePatches.rrect);
        counterPillBg.setSize(pillWidth, pillHeight);
        counterPillBg.setPosition((containerWidth - pillWidth) * 0.5f, 0);
        counterPillBg.setColor(1f, 1f, 1f, 0.18f);
        milestoneContainer.addActor(counterPillBg);

        milestoneProgressLabel.setX(0);
        milestoneProgressLabel.setY((pillHeight - milestoneProgressLabel.getHeight()) * 0.5f);
        milestoneContainer.addActor(milestoneProgressLabel);

        milestoneContainer.setSize(containerWidth, containerHeight);
        milestoneContainer.setX((getWidth() - containerWidth) * 0.5f);
        milestoneContainer.setY(getHeight() * 0.56f - containerHeight * 0.5f);
        milestoneContainer.setOrigin(Align.center);
        addActor(milestoneContainer);
    }

    private Image createMistRing(float size) {
        Image mist = new Image(AtlasRegions.glow);
        mist.setSize(size, size);
        mist.setOrigin(Align.center);
        mist.setColor(MILESTONE_MIST_COLOR);
        return mist;
    }

    private void positionCentered(Image image, float centerX, float centerY) {
        image.setPosition(centerX - image.getWidth() * 0.5f, centerY - image.getHeight() * 0.5f);
    }

    // Halkadan dışa doğru yayılan bir dalga: tam halka kenarında (startScale)
    // en belirgin halinde başlıyor ve dışa doğru yayıldıkça (scale büyüdükçe)
    // opaklığı SÜREKLİ ve DOĞRUSAL (linear) azalıyor - yayılma oranıyla solma
    // oranı birebir eşleşiyor. Yayılma/solma kısmı olduğu gibi korunuyor
    // (zaten "çok güzel" olarak onaylandı); TEK fark, her döngünün EN
    // BAŞINDA artık ani bir "pop" / kalp atışı gibi sıçrama YOK - dalga çok
    // kısa (0.3sn) ve yumuşak bir şekilde beliriyor, sonra normal yavaş
    // (6sn) yayılma+solma fazına geçiyor. Toplam döngü süresi (6sn) aynı
    // kalıyor, sadece başlangıç ânı yumuşatıldı.
    private void startMistAnimation(final Image mist, float delay) {
        final float startScale = 1f;    // dalga tam halka kenarından başlar
        final float endScale = 2.6f;    // dışa doğru geniş bir alana yayılır
        final float duration = 6f;      // toplam döngü süresi
        final float peakAlpha = 0.32f;  // dalga halka kenarındayken en belirgin hali
        final float fadeInTime = 0.3f;  // ani sıçrama yerine çok kısa, yumuşak bir beliriş

        mist.setScale(startScale);
        mist.getColor().a = 0f;

        mist.addAction(Actions.sequence(
                Actions.delay(delay),
                Actions.forever(Actions.sequence(
                        Actions.run(new Runnable() {
                            @Override
                            public void run() {
                                mist.setScale(startScale);
                                mist.getColor().a = 0f;
                            }
                        }),
                        Actions.alpha(peakAlpha, fadeInTime),
                        Actions.parallel(
                                Actions.scaleTo(endScale, endScale, duration - fadeInTime),
                                Actions.alpha(0f, duration - fadeInTime)
                        )
                ))
        ));
    }

    // completedLevels: az önce bitirilen seviye dahil, o ana kadar tamamlanan
    // toplam seviye sayısı (nextLevelIndex ile aynı değer).
    private void updateMilestoneBadge(int completedLevels) {
        // ANA ANAHTAR KAPALI: MILESTONE_FEATURE_ENABLED = false ise "5
        // seviyede bir ödül" sisteminin tamamı (halka, hediye kutusu,
        // sis efekti, dalga efekti, coin ödülü) devre dışı - rozet hiç
        // oluşturulmaz/gösterilmez, ödül verilmez.
        if (!MILESTONE_FEATURE_ENABLED) return;

        if (milestoneContainer == null) setMilestoneBadge();

        int progress = completedLevels % MILESTONE_LEVEL_INTERVAL;
        boolean milestoneReached = completedLevels > 0 && progress == 0;
        int filled = milestoneReached ? MILESTONE_LEVEL_INTERVAL : progress;

        // Halka dokusu, yeni dolu bölüm sayısıyla GÜNCELLENİYOR. Yeni bir
        // Texture (ve dolayısıyla yeni bir GL doku ID'si) OLUŞTURMUYORUZ -
        // KALICI pixmap'in İÇERİĞİNİ yeniden çizip, AYNI managed
        // TextureData/Texture üzerinden GPU'ya tekrar yüklüyoruz (bkz.
        // milestoneRingPixmap alanının yorumu - context-loss dayanıklılığı
        // için pixmap KALICI olmak zorunda, her seferinde yeni bir pixmap
        // oluşturup eskisini dispose etmiyoruz).
        renderRingPixmap(filled, milestoneRingPixmap);
        milestoneRingTexture.load(milestoneRingTextureData);

        milestoneProgressLabel.setText(filled + "/" + MILESTONE_LEVEL_INTERVAL);
        milestoneGiftIcon.setDrawable(new TextureRegionDrawable(
                milestoneReached ? AtlasRegions.giftbox_open : AtlasRegions.giftbox_closed));

        milestoneWheelAvailable = milestoneReached;
        milestoneGiftIcon.setTouchable(milestoneReached ? Touchable.enabled : Touchable.disabled);

        if (milestoneReached) awardMilestoneReward();
    }

    // 10 seviyede bir kutlama: artık konfeti YOK. Bunun yerine, ortamda
    // sürekli dönen sisli dalgayla (startMistAnimation) AYNI görsel dilde,
    // ama ondan daha BELİRGİN (daha yüksek opaklık + daha geniş yayılma)
    // 3 adet dalga halkası halkanın kenarından dışa doğru yayılıp kayboluyor.
    // Ortam sis dalgasının aksine SÜREKLİ dönmüyor - sadece milestone anında
    // bir kez tetiklenir ve kendini otomatik temizler (removeActor).
    private void celebrateWithRingPulse() {
        float ringOuterRadius = milestoneRingRadius + milestoneRingThickness * 0.5f;
        float centerX = milestoneRingImage.getX() + milestoneRingImage.getWidth() * 0.5f;
        float centerY = milestoneRingImage.getY() + milestoneRingImage.getHeight() * 0.5f;
        float waveSize = ringOuterRadius * 2.3f;

        final float startScale = 1f;
        final float endScale = 3.2f;
        final float peakAlpha = 0.8f;
        final float fadeInTime = 0.12f;
        final float spreadTime = 0.9f;
        final float staggerDelay = 0.28f;

        for (int i = 0; i < 3; i++) {
            final Image wave = createMistRing(waveSize);
            wave.setColor(Color.WHITE);
            positionCentered(wave, centerX, centerY);
            wave.setScale(startScale);
            wave.getColor().a = 0f;
            milestoneContainer.addActor(wave);

            wave.addAction(Actions.sequence(
                    Actions.delay(i * staggerDelay),
                    Actions.alpha(peakAlpha, fadeInTime),
                    Actions.parallel(
                            Actions.scaleTo(endScale, endScale, spreadTime),
                            Actions.alpha(0f, spreadTime)
                    ),
                    Actions.removeActor()
            ));
        }
    }

    // 10 seviyede bir kazanılan "büyük ödül" çarkı: hediye kutusu o an
    // açıksa (milestoneWheelAvailable) dokunulabilir olur ve normal günlük
    // 24 saatlik bekleme süresine tabi olmayan ayrı bir WheelDialog açar.
    private final ClickListener milestoneWheelListener = new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            if (!milestoneWheelAvailable) return;
            milestoneWheelAvailable = false;
            milestoneGiftIcon.setTouchable(Touchable.disabled);

            // Bu bonus çevirme, oyuncunun normal günlük ücretsiz çevirme
            // hakkını (KEY_LAST_WHEEL_SPIN_TIME) tüketmemeli. WheelDialog
            // her çevirmede bu zamanı güncellediği için, diyaloğu açmadan
            // önceki değeri yedekleyip kapandığında geri yüklüyoruz.
            final Preferences preferences = Gdx.app.getPreferences(Constants.PREFS_NAME);
            final long savedSpinTime = preferences.getLong(Constants.KEY_LAST_WHEEL_SPIN_TIME, 0);

            final WheelDialog bonusWheel = new WheelDialog(screen.stage.getWidth(), screen.stage.getHeight(), screen);
            bonusWheel.setDialogId(Constants.WHEEL_DIALOG);
            screen.stage.addActor(bonusWheel);
            bonusWheel.setVisible(true);
            bonusWheel.show();

            addAction(new Action() {
                @Override
                public boolean act(float delta) {
                    if (bonusWheel.getStage() != null) return false;

                    preferences.putLong(Constants.KEY_LAST_WHEEL_SPIN_TIME, savedSpinTime);
                    preferences.flush();
                    return true;
                }
            });
        }
    };

    private void awardMilestoneReward() {
        // "Dalga efekti" (ışık dairesi patlaması / celebrateWithRingPulse),
        // MILESTONE_WAVE_EFFECT_ENABLED anahtarına bağlı. ŞU AN KAPALI
        // (false). Tekrar açmak için yukarıdaki anahtarı true yapmanız
        // yeterli - başka hiçbir yeri değiştirmenize gerek yok.
        if (MILESTONE_WAVE_EFFECT_ENABLED) celebrateWithRingPulse();

        // Coin bakiyesini güncelliyoruz. GameScreen, bu çağrıdan hemen sonra
        // kendi combo ödülünü HintManager.setCoinCount(...) ile senkron
        // olarak yazıyor; aynı anda burada yazarsak o satır bizim eklediğimiz
        // bonusu ezer. Bunu önlemek için güncellemeyi bir sonraki frame'e
        // erteliyoruz.
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                HintManager.setCoinCount(HintManager.getRemainingCoins() + MILESTONE_REWARD_COINS);
            }
        });

        milestoneGiftIcon.clearActions();
        milestoneGiftIcon.setScale(0.7f);
        milestoneGiftIcon.addAction(Actions.sequence(
                Actions.scaleTo(1.25f, 1.25f, 0.18f, Interpolation.backOut),
                Actions.scaleTo(1f, 1f, 0.15f)
        ));

        Label.LabelStyle rewardStyle = new Label.LabelStyle(milestoneProgressLabel.getStyle().font, MILESTONE_REWARD_TEXT_COLOR);
        Label rewardPopup = new Label("+" + MILESTONE_REWARD_COINS, rewardStyle);
        rewardPopup.setPosition(milestoneContainer.getWidth() * 0.5f - rewardPopup.getWidth() * 0.5f, milestoneContainer.getHeight() + 4f);
        rewardPopup.getColor().a = 0f;
        milestoneContainer.addActor(rewardPopup);
        rewardPopup.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.fadeIn(0.2f),
                        Actions.moveBy(0, 24f, 0.9f, Interpolation.cubicOut)
                ),
                Actions.fadeOut(0.4f),
                Actions.removeActor()
        ));

        // 10 seviye tamamlanınca açılan hediye kutusundan (halkanın
        // ortasından), üst paneldeki coin göstergesine doğru uçan coin
        // animasyonu - SESSİZ (coin sesi YOK). Bu, kullanıcı isteği
        // doğrultusunda SADECE bu milestone ödülüne özel; oyundaki diğer
        // TÜM coin animasyonları (combo, UFO vurma, bonus kelime ödülü vb.)
        // bu değişiklikten etkilenmiyor ve normal sesiyle çalışmaya devam
        // ediyor (bkz. CoinView.createCoinAnimation / Coin.animateForCoinView
        // "silent" parametresi - sadece burada true veriliyor).
        flyMilestoneCoinsToTopPanel(MILESTONE_REWARD_COINS);
    }

    // Hediye kutusundan üst paneldeki coin ikonuna uçan coinler. Coin
    // göstergesi (screen.topPanel.coinView), uçuş sırasında görünür olması
    // için (level-end ekranının opak arka planının ÜSTÜNDE) geçici olarak
    // bu görünüme taşınıyor - "Sonraki Seviye" butonuna basılana ya da
    // uçuş bitene kadar aynı teknik (setCoinView/putBackCoinView) kullanılıyor;
    // önceden seviye başı combo ödülü için kullanılan yöntemle birebir aynı.
    private void flyMilestoneCoinsToTopPanel(int count) {
        setCoinView();

        Vector2 pos = milestoneGiftIcon.localToActorCoordinates(
                screen.topPanel.coinView,
                new Vector2(milestoneGiftIcon.getWidth() * 0.5f, milestoneGiftIcon.getHeight() * 0.5f));

        screen.topPanel.coinView.createCoinAnimation(count, pos.x, pos.y, true, milestoneCoinAnimComplete);
    }

    private Runnable milestoneCoinAnimComplete = new Runnable() {
        @Override
        public void run() {
            Action fadeOut = Actions.fadeOut(0.3f);
            RunnableAction runnableAction = new RunnableAction();
            runnableAction.setRunnable(new Runnable() {
                @Override
                public void run() {
                    putBackCoinView();
                }
            });
            screen.topPanel.coinView.addAction(new SequenceAction(fadeOut, runnableAction));
        }
    };

    public void hide() {
        float time = 0f;
        if (dictButton != null) {
            time += 0.1f;
            UiUtil.actorAnimOut(dictButton, time, null);
        }

        // cupContainer yok, atlandı

        if (milestoneContainer != null) {
            time += 0.1f;
            UiUtil.actorAnimOut(milestoneContainer, time, null);
        }

        if (rewardLabelContainer != null) {
            time += 0.1f;
            UiUtil.actorAnimOut(rewardLabelContainer, time, null);
        }

        time += 0.1f;
        UiUtil.actorAnimOut(nextLevelShadow, time, null);
        UiUtil.actorAnimOut(nextLevel, time, fadeOut);
        // Yazı, butonun kendisinden önce kaybolmasın diye ölçek animasyonuna
        // paralel bir alfa (şeffaflık) geçişi ekleniyor - ikisi aynı anda söner.
        nextLevel.addAction(Actions.sequence(Actions.delay(time), Actions.fadeOut(0.3f)));

        // endedContainer yok
    }

    private AlphaAction fadeOutAlphaAction;
    private RunnableAction fadeOutRunnableAction;
    private SequenceAction fadeOutSequenceAction;

    private Runnable fadeOut = new Runnable() {
        @Override
        public void run() {
            if (fadeOutAlphaAction == null) fadeOutAlphaAction = new AlphaAction();
            else fadeOutAlphaAction.reset();
            fadeOutAlphaAction.setAlpha(0f);
            fadeOutAlphaAction.setDuration(0.15f);

            if (fadeOutRunnableAction == null) fadeOutRunnableAction = new RunnableAction();
            else fadeOutRunnableAction.reset();
            fadeOutRunnableAction.setRunnable(nextLevelCallback);

            if (fadeOutSequenceAction == null) fadeOutSequenceAction = new SequenceAction();
            else fadeOutSequenceAction.reset();
            fadeOutSequenceAction.addAction(fadeOutAlphaAction);
            fadeOutSequenceAction.addAction(fadeOutRunnableAction);

            addAction(fadeOutSequenceAction);
        }
    };

    private void checkGameEnd(int nextLevelIndex) {
        if (nextLevelIndex == LanguageManager.locale.LevelCount) {
            nextLevel.setText(LanguageManager.get("back"));

            String font = UIConfig.GAME_COMPLETELY_FINISHED_TEXT_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
            Label.LabelStyle style = new Label.LabelStyle();
            style.font = screen.wordConnectGame.resourceManager.get(font, BitmapFont.class);
            style.fontColor = UIConfig.GAME_COMPLETELY_FINISHED_TEXT_COLOR;

            Label label = new Label(LanguageManager.get("to_be_continued"), style);

            // endedContainer oluşturma (isteğe bağlı)
        }
    }

    public void startComboAnimWithRewards(int coinCount, int nextLevelIndex, boolean hintUsed) {
        checkGameEnd(nextLevelIndex);
        updateMilestoneBadge(nextLevelIndex);

        String nextGameScreenBackground = UIConfig.getGameScreenBackgroundImage(GameData.findFirstIncompleteLevel());
        if (!screen.wordConnectGame.resourceManager.contains(nextGameScreenBackground)) {
            screen.wordConnectGame.resourceManager.load(nextGameScreenBackground, Texture.class);
            screen.wordConnectGame.resourceManager.finishLoading();
        }

        // cupContainer yok, atlandı

        // NOT: Seviye sonunda (her bölüm bitişinde) uçan coin hediye
        // animasyonu ve "+X coin" ödül yazısı KALDIRILDI (kullanıcı
        // isteğiyle iptal edildi). Coin bakiyesi GameScreen tarafında
        // (HintManager.setCoinCount) normal şekilde güncellenmeye devam
        // ediyor - sadece burada gösterilen uçan coin/ödül metni kaldırıldı.
        // Coin ödülü artık SADECE 10 seviyede bir tamamlanan hediye
        // kutusundan (bkz. awardMilestoneReward) veriliyor. Kombo ödülü
        // artık coin bakiyesine eklenmiyor (bkz. GameScreen.showLevelFinishedView).

        animateAllCombo();
        getStage().getRoot().setTouchable(Touchable.enabled);
    }

    private void animateAllCombo() {
        if (dictButton != null) UiUtil.actorAnimIn(dictButton, 0f, null);
        if (screen.topPanel.coinView.getParent().equals(this))
            UiUtil.actorAnimIn(screen.topPanel.coinView, 0f, null);

        float time = 0.1f;
        // cupContainer animasyonu yok

        if (milestoneContainer != null) {
            UiUtil.actorAnimIn(milestoneContainer, time, null);
            time += 0.1f;
        }

        if (rewardLabelContainer != null) {
            time += 0.1f;
            UiUtil.actorAnimIn(rewardLabelContainer, time, null);
        }

        time += 0.1f;
        UiUtil.actorAnimIn(nextLevelShadow, time, null);
        UiUtil.actorAnimIn(nextLevel, time, null);
        // Yazı, butonun kendisinden önce belirmesin diye ölçek animasyonuna
        // paralel bir alfa (şeffaflık) geçişi ekleniyor - ikisi aynı anda beliriyor.
        nextLevel.getColor().a = 0f;
        nextLevel.addAction(Actions.sequence(Actions.delay(time), Actions.fadeIn(0.3f)));

        // endedContainer animasyonu yok
    }
}