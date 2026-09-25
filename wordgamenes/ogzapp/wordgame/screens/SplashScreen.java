package ogzapp.wordgame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.ShaderProgramLoader;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.I18NBundle;

import ogzapp.wordgame.WordConnectGame;
import ogzapp.wordgame.config.ConfigProcessor;
import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.model.GameData;
import ogzapp.wordgame.ui.board.CellView;
import ogzapp.wordgame.ui.dial.DialButton;
import ogzapp.wordgame.ui.dialogs.menu.LanguageDialog;
import ogzapp.wordgame.util.UiUtil;

public class SplashScreen extends BaseScreen {

    private boolean loading;
    private Image loadingImg;
    private Image loadingBgImg;
    private Image loadingGlowImg;     // çubuğun arkasında yumuşak, nabız gibi atan parıltı
    private Image loadingHighlightImg; // dolan çubuğun üstünde ince, parlak "cam" şeridi
    private Texture glowTex;          // kod içinde üretilen yumuşak dairesel parıltı dokusu
    private float maxBarWidth;
    private float displayedProgress;  // çubuğun aniden zıplamak yerine yumuşak dolması için
    private Texture loading_bg, loadingTex;
    private Texture stripedFillTex;     // "Bonus Kelimeler" tarzı yeşil/çapraz çizgili dolgu dokusu
    private Texture frostedBgTex;       // yükleme çubuğunun altındaki buzlu/şeffaf zemin dokusu
    private final Group group = new Group();
    private Image logoImg;
    private float time;
    // Çözünürlüğe göre seçilen dial/kutu BitmapFont yolu (gdx-freetype yok).
    private String dialBoardFontPath;

    public SplashScreen(WordConnectGame wordConnectGame) {
        super(wordConnectGame);
        ShaderProgram.pedantic = false;
    }

    @Override
    public void show() {
        super.show();

        ResourceManager.introBackground = UIConfig.getIntroScreenBackgroundImage(wordConnectGame);
        wordConnectGame.resourceManager.load(ResourceManager.introBackground, Texture.class);
        wordConnectGame.resourceManager.finishLoading();
        setBackground(UIConfig.INTRO_SCREEN_BACKGROUND_COLOR, ResourceManager.introBackground);

        // Siyah "GİRİŞ" splash (logo.png) yok. Atlas ikonu, manzara
        // yükleme ekranında yeşil dairenin olduğu yerde durur.
        try {
            ResourceManager.ATLAS_1 = ResourceManager.resolveResolutionAwarePath(ResourceManager.ATLAS_1);
            if (!wordConnectGame.resourceManager.contains(ResourceManager.ATLAS_1)) {
                wordConnectGame.resourceManager.load(ResourceManager.ATLAS_1, TextureAtlas.class);
                wordConnectGame.resourceManager.finishLoading();
            }
            TextureAtlas.AtlasRegion splashLogo = wordConnectGame.resourceManager
                    .get(ResourceManager.ATLAS_1, TextureAtlas.class).findRegion("splash_logo");
            if (splashLogo != null) {
                logoImg = new Image(splashLogo);
                logoImg.setOrigin(Align.center);
                float logoSize = stage.getWidth() * 0.36f;
                logoImg.setSize(logoSize, logoSize);
                logoImg.setPosition((stage.getWidth() - logoSize) * 0.5f, stage.getHeight() * 0.58f);
                stage.addActor(logoImg);
            }
        } catch (Exception e) {
            // logo yoksa sorun değil
        }

        // Kod içinde üretilen yumuşak dairesel parıltı dokusu (yeni bir
        // görsel dosyası eklemeden "premium" bir glow efekti için).
        glowTex = generateGlowTexture(128);

        // Yükleme çubuğu arka planı (track)
        loading_bg = new Texture(Gdx.files.internal("textures/loading_bg.png"));
        int w = loading_bg.getWidth() / 3;
        NinePatch bgPatch = new NinePatch(loading_bg, w, w, w, w);
        loadingBgImg = new Image(bgPatch);

        // NOT: Kullanıcı isteğiyle biraz daha da genişletildi.
        if (UiUtil.isScreenWide()) loadingBgImg.setWidth(stage.getWidth() * 0.30f);
        else loadingBgImg.setWidth(stage.getWidth() * 0.75f);

        loadingBgImg.setHeight(16 * Gdx.graphics.getDensity() * ResourceManager.scaleFactor);

        // NOT: Kullanıcı, çubuğun altındaki zeminin SİYAH göründüğünü
        // belirtti. Orijinal "loading_bg.png" dokusunun üstüne beyaz/opak
        // tonlama uygulamak yeterli olmadığı için, zemin artık (dolgu
        // çubuğuyla AYNI Pixmap tekniğiyle) kod içinde üretilen, yarı
        // saydam "buzlu cam" (frosted glass) görünümlü yuvarlak bir hap
        // ile değiştirildi - böylece görünüm dokudan bağımsız, garanti
        // şekilde açık/şeffaf oluyor.
        int bgHeightPx = Math.max(8, Math.round(loadingBgImg.getHeight()));
        if (bgHeightPx % 2 != 0) bgHeightPx++;
        frostedBgTex = createFrostedPillTexture(bgHeightPx, new Color(0.86f, 0.93f, 1f, 0.32f));
        NinePatch frostedBgPatch = new NinePatch(frostedBgTex, bgHeightPx / 2, bgHeightPx / 2, 0, 0);
        loadingBgImg.setDrawable(new NinePatchDrawable(frostedBgPatch));
        loadingBgImg.setSize(loadingBgImg.getWidth(), bgHeightPx);

        // Çubuğun arkasında hafif, yavaşça nabız gibi atan bir parıltı halesi.
        float glowSize = loadingBgImg.getHeight() * 5f;
        loadingGlowImg = new Image(glowTex);
        loadingGlowImg.setSize(glowSize, glowSize);
        loadingGlowImg.setOrigin(Align.center);
        loadingGlowImg.setColor(UIConfig.LOADING_BAR_COLOR);
        loadingGlowImg.getColor().a = 0.35f;
        loadingGlowImg.addAction(Actions.forever(Actions.sequence(
                Actions.parallel(
                        Actions.scaleTo(1.15f, 1.15f, 1.4f, Interpolation.sine),
                        Actions.alpha(0.55f, 1.4f, Interpolation.sine)
                ),
                Actions.parallel(
                        Actions.scaleTo(0.9f, 0.9f, 1.4f, Interpolation.sine),
                        Actions.alpha(0.25f, 1.4f, Interpolation.sine)
                )
        )));
        group.addActor(loadingGlowImg);

        // NOT: Burada önceden çubuğun altında SİYAH tonlu (0,0,0,0.28 alfa)
        // bir "derinlik gölgesi" (loadingShadowImg) vardı. Track artık buzlu/
        // şeffaf beyaz olduğu için bu siyah gölge, kullanıcının şikayet
        // ettiği "siyah zemin" görüntüsünün asıl kaynağıydı - kaldırıldı.
        group.addActor(loadingBgImg);
        group.setSize(loadingBgImg.getWidth(), loadingBgImg.getHeight());
        group.setOrigin(Align.center);
        group.setX((stage.getWidth() - group.getWidth()) * 0.5f);
        group.setY(stage.getHeight() * 0.1f);
        stage.addActor(group);

        loadingTex = new Texture(Gdx.files.internal("textures/loading.png"));
        w = loadingTex.getWidth() / 3;
        NinePatch loadingPatch = new NinePatch(loadingTex, w, w, w, w);

        // "Bonus Kelimeler" ekranındaki yeşil, çapraz çizgili ilerleme
        // çubuğuyla AYNI görsel dilde bir dolgu - yeni bir görsel dosyası
        // eklemeden, LevelEndView.createPillNinePatch/renderRingPixmap ile
        // AYNI Pixmap tekniğiyle kod içinde üretiliyor (bkz. aşağıdaki
        // createStripedPillNinePatch). Sadece dolgunun kendisi (loadingImg)
        // değişti; alttaki track (loadingBgImg), gölge, parıltı ve üstteki
        // ince cam highlight şeridi (loadingHighlightImg) OLDUĞU GİBİ kaldı.
        int fillHeightPx = Math.max(8, Math.round(loadingBgImg.getHeight() * 0.8f));
        if (fillHeightPx % 2 != 0) fillHeightPx++; // uçların simetrik yuvarlak kalması için çift sayı
        // NOT: Kullanıcı isteğiyle dolgu rengi koyu yeşilden BEYAZA çevrildi.
        // Aynı createStripedPillTexture metodu kullanılıyor; taban ve çizgi
        // rengi AYNI (beyaz) verildiği için çizgi deseni görünmez oluyor ve
        // sonuç düz/opak beyaz bir dolgu oluyor - yeni bir metoda gerek kalmadı.
        stripedFillTex = createStripedPillTexture(
                fillHeightPx,
                new Color(1f, 1f, 1f, 1f), // beyaz
                new Color(1f, 1f, 1f, 1f)  // beyaz (çizgisiz düz dolgu)
        );
        NinePatch stripedPatch = new NinePatch(stripedFillTex, fillHeightPx / 2, fillHeightPx / 2, 0, 0);

        loadingImg = new Image(stripedPatch);
        loadingImg.setWidth(0);
        loadingImg.setHeight(loadingBgImg.getHeight() * 0.8f);
        group.addActor(loadingImg);

        // Dolan çubuğun üst kısmında ince, parlak bir "cam" şeridi - klasik
        // premium/glossy görünüm. loadingImg ile aynı genişlikte tutulur
        // (bkz. update()).
        loadingHighlightImg = new Image(loadingPatch);
        loadingHighlightImg.setColor(1f, 1f, 1f, 0.35f);
        loadingHighlightImg.setWidth(0);
        loadingHighlightImg.setHeight(loadingImg.getHeight() * 0.42f);
        group.addActor(loadingHighlightImg);

        float margin = (loadingBgImg.getHeight() - loadingImg.getHeight()) * 0.52f;
        loadingImg.setPosition(margin, margin);
        loadingHighlightImg.setPosition(margin, margin + loadingImg.getHeight() - loadingHighlightImg.getHeight() - loadingImg.getHeight() * 0.06f);
        maxBarWidth = group.getWidth() - margin * 2f;

        loadAssets();
    }

    // 128x128'lik, merkezden dışa doğru yumuşakça sönen dairesel bir alfa
    // (glow) dokusu üretir. Herhangi bir renkle tonlanabilir.
    private Texture generateGlowTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float center = size / 2f;
        float maxDist = size / 2f;

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float alpha = MathUtils.clamp(1f - dist / maxDist, 0f, 1f);
                alpha *= alpha; // yumuşak (kuadratik) düşüş
                pixmap.setColor(1f, 1f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }

        // KÖK NEDEN DÜZELTMESİ (context-loss dayanıklılığı - bkz.
        // LevelEndView.createPillNinePatch ve IntroScreen'deki AYNI
        // düzeltme): pixmap dispose EDİLMİYOR ve managed=true ile
        // PixmapTextureData'ya sarılıyor - böylece Android'de GL context
        // kaybından (uygulama arka plana alınıp geri gelince) sonra bu doku
        // da libGDX'in otomatik kurtarma sistemiyle doğru içerikle kendini
        // yeniden oluşturabiliyor.
        PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
        Texture texture = new Texture(texData);
        return texture;
    }

    // "Bonus Kelimeler" ekranındaki ilerleme çubuğuyla aynı görsel dilde:
    // yuvarlak (hap/pill) uçlu, zemin rengi + 45° çapraz açık renkli
    // çizgilerden oluşan bir dolgu dokusu üretir. Aynı teknik
    // LevelEndView.createPillNinePatch / renderRingPixmap içinde de
    // kullanılıyor - burada da yeni bir görsel dosyasına ihtiyaç yok.
    // Üretilen dokunun sol/sağ "radius" kadarlık kısmı NinePatch ile SABİT
    // (esnetilmeyen) uç olarak kullanılacağı için yuvarlaklık her genişlikte
    // korunur; sadece ortadaki çizgili şerit yatayda esner.
    private Texture createStripedPillTexture(int height, Color base, Color stripe) {
        int h = Math.max(8, height);
        int radius = h / 2;

        int stripeWidthPx = Math.max(2, Math.round(h * 0.35f));
        int stripeGapPx = Math.max(2, Math.round(h * 0.35f));
        int stripePeriod = stripeWidthPx + stripeGapPx;
        int middle = Math.max(stripePeriod * 3, h); // birkaç tam çizgi döngüsü sığacak kadar geniş orta şerit
        int w = radius * 2 + middle;

        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setColor(base);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(w - radius, radius, radius);
        pixmap.fillRectangle(radius, 0, middle, h);

        pixmap.setColor(stripe);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int existing = pixmap.getPixel(x, y);
                if ((existing & 0xFF) == 0) continue; // yuvarlak ucun dışı (şeffaf) - dokunma
                if ((x + y) % stripePeriod < stripeWidthPx) {
                    pixmap.drawPixel(x, y);
                }
            }
        }

        // KÖK NEDEN DÜZELTMESİ (context-loss dayanıklılığı): bkz.
        // generateGlowTexture() yorumu - aynı managed doku tekniği.
        PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
        Texture texture = new Texture(texData);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    // Yükleme çubuğunun altındaki track için: düz, yarı saydam "buzlu cam"
    // (frosted glass) renginde, yuvarlak (hap/pill) uçlu bir zemin dokusu
    // üretir. createStripedPillTexture ile AYNI yuvarlatma tekniği,
    // sadece çizgi deseni yok - tek düz renk + alfa.
    private Texture createFrostedPillTexture(int height, Color fill) {
        int h = Math.max(8, height);
        int radius = h / 2;
        int middle = Math.max(h, 4);
        int w = radius * 2 + middle;

        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setColor(fill);
        pixmap.fillCircle(radius, radius, radius);
        pixmap.fillCircle(w - radius, radius, radius);
        pixmap.fillRectangle(radius, 0, middle, h);

        // KÖK NEDEN DÜZELTMESİ (context-loss dayanıklılığı): bkz.
        // generateGlowTexture() yorumu - aynı managed doku tekniği.
        PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
        Texture texture = new Texture(texData);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }


    private void loadAssets() {
        GameData.resourceManager = wordConnectGame.resourceManager;
        I18NBundle.setSimpleFormatter(true);
        String localeCode = LanguageManager.getSelectedLocaleCode();

        if (localeCode != null) {
            setNewLanguage(localeCode);
        }

        // Asset yüklemeleri...
        ResourceManager.ATLAS_1 = ResourceManager.resolveResolutionAwarePath(ResourceManager.ATLAS_1);
        ResourceManager.ATLAS_2 = ResourceManager.resolveResolutionAwarePath(ResourceManager.ATLAS_2);
        ResourceManager.ATLAS_3 = ResourceManager.resolveResolutionAwarePath(ResourceManager.ATLAS_3);
        ResourceManager.ATLAS_4 = ResourceManager.resolveResolutionAwarePath(ResourceManager.ATLAS_4);

        if (!wordConnectGame.resourceManager.contains(ResourceManager.ATLAS_1)) {
            wordConnectGame.resourceManager.load(ResourceManager.ATLAS_1, TextureAtlas.class);
        }
        wordConnectGame.resourceManager.load(ResourceManager.ATLAS_2, TextureAtlas.class);
        wordConnectGame.resourceManager.load(ResourceManager.ATLAS_3, TextureAtlas.class);
        wordConnectGame.resourceManager.load(ResourceManager.ATLAS_4, TextureAtlas.class);

        wordConnectGame.resourceManager.load(ResourceManager.SHADER_LINE, ShaderProgram.class, new ShaderProgramLoader.ShaderProgramParameter() {{
            vertexFile = ResourceManager.SHADER_VERTEX;
        }});
        wordConnectGame.resourceManager.load(ResourceManager.SHADER_DIAL, ShaderProgram.class, new ShaderProgramLoader.ShaderProgramParameter() {{
            vertexFile = ResourceManager.SHADER_VERTEX;
        }});
        wordConnectGame.resourceManager.load(ResourceManager.SHADER_OVERLAY, ShaderProgram.class, new ShaderProgramLoader.ShaderProgramParameter() {{
            vertexFile = ResourceManager.SHADER_VERTEX;
        }});

        ResourceManager.fontSemiBold = ResourceManager.resolveResolutionAwarePath(ResourceManager.fontSemiBold);
        wordConnectGame.resourceManager.load(ResourceManager.fontSemiBold, BitmapFont.class);
        ResourceManager.fontSemiBoldShadow = ResourceManager.resolveResolutionAwarePath(ResourceManager.fontSemiBoldShadow);
        wordConnectGame.resourceManager.load(ResourceManager.fontSemiBoldShadow, BitmapFont.class);
        ResourceManager.fontBlack = ResourceManager.resolveResolutionAwarePath(ResourceManager.fontBlack);
        wordConnectGame.resourceManager.load(ResourceManager.fontBlack, BitmapFont.class);
        ResourceManager.fontBoardAndDialFont = ResourceManager.resolveResolutionAwarePath(ResourceManager.fontBoardAndDialFont);
        wordConnectGame.resourceManager.load(ResourceManager.fontBoardAndDialFont, BitmapFont.class);

        // Dial + çözülen kutu harfleri: TTF yerine hazır .fnt/.png.
        // android/assets/fonts/dial_board_{sd|hd|hdr}.fnt + aynı isimli .png
        dialBoardFontPath = ResourceManager.resolveResolutionAwarePath("fonts/dial_board.fnt");
        if (!Gdx.files.internal(dialBoardFontPath).exists()) {
            if (Gdx.files.internal("fonts/dial_board.fnt").exists()) {
                dialBoardFontPath = "fonts/dial_board.fnt";
            } else {
                dialBoardFontPath = null;
            }
        }
        if (dialBoardFontPath != null) {
            wordConnectGame.resourceManager.load(dialBoardFontPath, BitmapFont.class);
        }

        // Sesler...
        wordConnectGame.resourceManager.load(ResourceManager.SFX_BLAST, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_HINT, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_MONSTER_JUMP, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_LEVEL_END, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_ROCKET, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SELECT_1, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SELECT_2, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SELECT_3, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SELECT_4, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SELECT_5, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SELECT_6, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SELECT_7, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SELECT_8, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SPIN_CLICK, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SUCCESS, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_WHEEL_SPIN, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_FOUND_BEFORE, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_BONUS_WORD, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_WRONG, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_SHUFFLE, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_NOTIFICATION, Sound.class);
        wordConnectGame.resourceManager.load(ResourceManager.SFX_HIT_BOOSTER, Sound.class);

        ResourceManager.gameBackground = UIConfig.getGameScreenBackgroundImage(GameData.findFirstIncompleteLevel());
        wordConnectGame.resourceManager.load(ResourceManager.gameBackground, Texture.class);

        loading = true;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        time += delta;

        if (loading) {
            update(delta);
            loadingImg.setScale(1f + 0.015f * (float) Math.sin(time * 5f));
        }
    }

    private void update(float delta) {
        wordConnectGame.resourceManager.update();
        float progress = wordConnectGame.resourceManager.getProgress();

        // Çubuğun aniden zıplamak yerine yumuşak bir şekilde dolması için
        // gösterilen ilerlemeyi gerçek ilerlemeye doğru kademeli yaklaştırıyoruz.
        displayedProgress += (progress - displayedProgress) * Math.min(1f, 6f * delta);
        if (progress >= 1f && (1f - displayedProgress) < 0.01f) displayedProgress = 1f;

        float barWidth = maxBarWidth * displayedProgress;
        loadingImg.setWidth(barWidth);
        loadingHighlightImg.setWidth(Math.max(0f, barWidth - loadingImg.getHeight() * 0.3f));

        if (progress == 1f) {
            loading = false;

            AtlasRegions.init(wordConnectGame.resourceManager);
            NinePatches.init(wordConnectGame.resourceManager);
            GameConfig.setUpIAPToItemMapping();

            BitmapFont font1 = wordConnectGame.resourceManager.get(ResourceManager.fontSemiBoldShadow, BitmapFont.class);
            font1.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            BitmapFont font2 = wordConnectGame.resourceManager.get(ResourceManager.fontSemiBold, BitmapFont.class);
            font2.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            // Dial yuvarlak tablodaki harfler + üstteki çözülen kutu harfleri.
            // gdx-freetype yok: TTF çalışma anında üretilmez. Bunun yerine
            // assets/fonts/dial_board_{sd|hd|hdr}.fnt +.png (BitmapFont)
            // yüklenir. Dosyalar yoksa montserrat_semibold_board'a düşülür.
            BitmapFont gameFont = null;
            if (dialBoardFontPath != null && wordConnectGame.resourceManager.contains(dialBoardFontPath)) {
                gameFont = wordConnectGame.resourceManager.get(dialBoardFontPath, BitmapFont.class);
            }
            if (gameFont == null) {
                gameFont = wordConnectGame.resourceManager.get(ResourceManager.fontBoardAndDialFont, BitmapFont.class);
            }
            gameFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            ConfigProcessor.muted = GameData.isGameMuted();

            CellView.labelStyleSolved = new Label.LabelStyle(gameFont, UIConfig.GAME_GRID_LETTER_COLOR_SOLVED);
            CellView.labelStyleRevealed = new Label.LabelStyle(gameFont, UIConfig.GAME_GRID_LETTER_COLOR_REVEALED);

            DialButton.labelStyleUp = new Label.LabelStyle();
            DialButton.labelStyleUp.font = gameFont;
            DialButton.labelStyleDown = new Label.LabelStyle();
            DialButton.labelStyleDown.font = gameFont;

            checkLanguage();
        }
    }

    private void checkLanguage() {
        if (GameConfig.availableLanguages == null || GameConfig.availableLanguages.isEmpty()) {
            Gdx.app.log("game.log", "No language has been configured in GameConfig.");
            return;
        }

        String localeCode = LanguageManager.getSelectedLocaleCode();
        if (localeCode == null) {
            dispose();

            if (GameConfig.availableLanguages.size() > 1) {
                LanguageDialog languageDialog = new LanguageDialog(stage.getWidth(), stage.getHeight(), this, languageSelectionComplete);
                stage.addActor(languageDialog);
                languageDialog.show();
            } else {
                for (String code : GameConfig.availableLanguages.keySet()) {
                    setNewLanguage(code);
                    languageSelectionComplete.run();
                }
            }
        } else {
            LanguageManager.bundle = wordConnectGame.resourceManager.get(ResourceManager.LOCALE_PROPERTIES_FILE, I18NBundle.class);

            // NOT: Kullanıcı isteğiyle burada artık HİÇBİR gecikme/animasyon
            // yok - yükleme dolar dolmaz ekran ANINDA değişiyor. Önceden
            // "içe doğru küçülüp kaybolma" (scaleTo 0,0), sonra "yavaş yavaş
            // kapanma" (5 saniyelik fadeOut) denenmişti; ikisi de kaldırıldı.
            dispose();
            wordConnectGame.setScreen(new IntroScreen(wordConnectGame));
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        loadingImg.remove();
        loadingBgImg.remove();
        if (loadingGlowImg != null) loadingGlowImg.remove();
        if (loadingHighlightImg != null) loadingHighlightImg.remove();

        if (logoImg != null) logoImg.remove();

        if (glowTex != null) glowTex.dispose();

        loading_bg.dispose();
        loadingTex.dispose();
        if (stripedFillTex != null) stripedFillTex.dispose();
        if (frostedBgTex != null) frostedBgTex.dispose();
    }
}
