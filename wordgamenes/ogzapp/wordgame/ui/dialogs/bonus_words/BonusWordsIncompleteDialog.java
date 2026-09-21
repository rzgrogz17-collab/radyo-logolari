package ogzapp.wordgame.ui.dialogs.bonus_words;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.model.GameData;
import ogzapp.wordgame.screens.BaseScreen;
import ogzapp.wordgame.screens.GameScreen;
import ogzapp.wordgame.ui.MarqueeLabel;
import ogzapp.wordgame.ui.ProgressBar;
import ogzapp.wordgame.ui.dialogs.BaseDialog;
import ogzapp.wordgame.ui.tutorial.Tutorial;
import ogzapp.wordgame.ui.tutorial.TutorialBooster;

public class BonusWordsIncompleteDialog extends BaseDialog {

    // Ekrandaki çapraz çizgiyle aynı çelik-mavi: kelime listesi çerçevesi.
    private static final Color FRAME_COLOR = new Color(0x6A8AA3FF);
    private static final Color PROGRESS_TRACK_BORDER = new Color(0x0C1E32FF);

    private ProgressBar progressBar;
    private MarqueeLabel countMarquee;
    private Label.LabelStyle wordsStyle;
    private Label.LabelStyle headerStyle;
    private Table wordsTable;
    private Group wordsGroup;
    private ScrollPane wordsPane;
    private GameScreen gameScreen;
    private TutorialBooster tutorialBooster;
    private float textShadowOffset;
    private float frameStroke;

    public BonusWordsIncompleteDialog(float width, float height, BaseScreen screen) {
        super(width, height, screen);
        gameScreen = (GameScreen) screen;

        float minSide = Math.min(width, height);
        content.setSize(Math.min(width * 0.90f, minSide * 0.92f), minSide * 0.94f);
        textShadowOffset = content.getWidth() * 0.008f;
        frameStroke = Math.max(3f, content.getWidth() * 0.008f);

        setContentBackground();
        contentBackground.setSize(content.getWidth(), content.getHeight());
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);
        BitmapFont bodyFont = screen.wordConnectGame.resourceManager.get(ResourceManager.fontSemiBoldShadow, BitmapFont.class);
        Label.LabelStyle bodyTextStyle = new Label.LabelStyle(bodyFont, Color.WHITE);
        Label.LabelStyle bodyShadowStyle = new Label.LabelStyle(bodyFont, UIConfig.FROSTED_ALERT_DIALOG_TEXT_SHADOW_COLOR);
        wordsStyle = new Label.LabelStyle(bodyFont, UIConfig.BWD_WORDS_TEXT_COLOR);
        headerStyle = new Label.LabelStyle(bodyFont, UIConfig.BWD_WORDS_TITLE_COLOR);

        setTitleLabel(LanguageManager.get("extra_words_incomplete"));
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);

        setCloseButton();
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });
        expandTitleToFrame();

        float pad = content.getWidth() * 0.045f;
        float barWidth = content.getWidth() * 0.78f;

        progressBar = new ProgressBar(AtlasRegions.bonus_bar_bg, AtlasRegions.bonus_words_bar_track);
        progressBar.setSize(barWidth, progressBar.getHeight());
        progressBar.setOrigin(Align.center);
        progressBar.setX((content.getWidth() - progressBar.getWidth()) * 0.5f);
        progressBar.setY(titleContainer.getY() - progressBar.getHeight() - content.getHeight() * 0.03f);
        progressBar.setRoundedTrack(UIConfig.BWD_WORDS_BG_COLOR, PROGRESS_TRACK_BORDER,
                Math.max(2f, progressBar.getHeight() * 0.10f));
        content.addActor(progressBar);

        countMarquee = new MarqueeLabel(bodyTextStyle, bodyShadowStyle, textShadowOffset);
        countMarquee.setFontScale(0.82f);
        countMarquee.setSize(content.getWidth() * 0.90f, countMarquee.getHeight());
        countMarquee.setX((content.getWidth() - countMarquee.getWidth()) * 0.5f);
        countMarquee.setY(progressBar.getY() - countMarquee.getHeight() - content.getHeight() * 0.012f);
        countMarquee.setMarqueeText(LanguageManager.get("extra_words_collected"));
        content.addActor(countMarquee);

        wordsGroup = new Group();
        wordsGroup.setWidth(content.getWidth() - pad * 2f);
        float wordsBottom = content.getHeight() * 0.045f;
        float wordsTop = countMarquee.getY() - content.getHeight() * 0.018f;
        wordsGroup.setHeight(Math.max(8f, wordsTop - wordsBottom));
        wordsGroup.setOrigin(Align.center);
        wordsGroup.setX((content.getWidth() - wordsGroup.getWidth()) * 0.5f);
        wordsGroup.setY(wordsBottom);
        content.addActor(wordsGroup);

        float radius = Math.min(wordsGroup.getWidth(), wordsGroup.getHeight()) * 0.06f;
        Image wordsGroupBg = new Image(new NinePatchDrawable(
                createRoundedFillNinePatch(wordsGroup.getHeight(), radius, UIConfig.BWD_WORDS_BG_COLOR)));
        wordsGroupBg.setSize(wordsGroup.getWidth(), wordsGroup.getHeight());
        wordsGroupBg.setTouchable(Touchable.disabled);
        wordsGroup.addActor(wordsGroupBg);
        Image wordsFrame = new Image(new NinePatchDrawable(
                createRoundedStrokeNinePatch(wordsGroup.getHeight(), radius, frameStroke, FRAME_COLOR)));
        wordsFrame.setSize(wordsGroup.getWidth(), wordsGroup.getHeight());
        wordsFrame.setTouchable(Touchable.disabled);
        wordsGroup.addActor(wordsFrame);

        wordsTable = new Table();
        wordsTable.top();
        wordsTable.defaults().growX();
        wordsTable.setFillParent(false);

        wordsPane = new ScrollPane(wordsTable);
        wordsPane.setScrollingDisabled(true, false);
        wordsPane.setScrollbarsVisible(true);
        wordsPane.setFadeScrollBars(true);
        wordsPane.setupFadeScrollBars(0.4f, 0.2f);
        wordsPane.setFlickScroll(true);
        wordsPane.setOverscroll(false, true);
        float inner = frameStroke * 1.5f;
        wordsPane.setSize(Math.max(8f, wordsGroup.getWidth() - inner * 2f),
                Math.max(24f, wordsGroup.getHeight() - inner * 2f));
        wordsPane.setX((wordsGroup.getWidth() - wordsPane.getWidth()) * 0.5f);
        wordsPane.setY((wordsGroup.getHeight() - wordsPane.getHeight()) * 0.5f);
        wordsGroup.addActor(wordsPane);
        // Çerçeve görsel olarak üstte kalsın ama dokunuş ScrollPane'e gitsin;
        // aksi halde Image tüm alanı kaplayıp kaydırmayı yutuyordu.
        wordsFrame.toFront();
        wordsFrame.setTouchable(Touchable.disabled);

        titleContainer.toFront();
        closeButton.toFront();
    }

    @Override
    public void show() {
        super.show();
        int current = GameData.getExtraWordsCount();
        int target = GameConfig.NUMBER_OF_BONUS_WORDS_TO_FIND_FOR_REWARD;
        updateViewWithData((float) current / (float) target, current, target);
    }

    public void checkTutorial() {
        if (GameData.isExtraWordsTutorialDisplayed1() && !GameData.isExtraWordsTutorialDisplayed2()) {
            gameScreen.tutorial = new TutorialBooster(screen);
            screen.stage.addActor(gameScreen.tutorial);
            gameScreen.tutorial.paddingX = 1.05f;
            gameScreen.tutorial.paddingY = 1.4f;

            Actor dummy = new Actor();
            dummy.setSize(progressBar.getWidth(), progressBar.getHeight());
            dummy.setPosition(progressBar.getX(), progressBar.getY());
            content.addActor(dummy);

            gameScreen.tutorial.highlightActor(dummy, Tutorial.Shape.RECT);
            gameScreen.tutorial.showText(LanguageManager.format("bonus_word_tutorial_2", GameConfig.NUMBER_OF_BONUS_WORDS_TO_FIND_FOR_REWARD));

            Vector2 pos = progressBar.localToActorCoordinates(gameScreen.tutorial, new Vector2());
            gameScreen.tutorial.textContainer.setY(pos.y - gameScreen.tutorial.textContainer.getHeight() - progressBar.getHeight());
            tutorialBooster = (TutorialBooster) gameScreen.tutorial;
            tutorialBooster.setGotIt(LanguageManager.get("got_it"));
            gameScreen.tutorial.fadeIn(null);
            tutorialBooster.tutorialSaver = new Tutorial.TutorialSaver() {
                @Override
                public void save() {
                    tutorialBooster.gotit.setDisabled(true);
                    GameData.setExtraWordsTutorialComplete2();
                }
            };
            tutorialBooster.setGotItListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    tutorialBooster.gotit.setDisabled(true);
                    GameData.setExtraWordsTutorialComplete2();
                    gameScreen.tutorial.fadeOut(gameScreen.tutorialRemover, true);
                }
            });
        }
    }

    private void expandTitleToFrame() {
        float gap = frameStroke * 1.5f;
        float left = gap;
        float right = content.getWidth() - gap;
        if (closeButton != null) {
            right = Math.min(right, closeButton.getX() - gap);
        }
        float titleW = Math.max(titleContainer.getWidth() * 1.08f, right - left);
        if (titleW > right - left) titleW = Math.max(8f, right - left);
        float titleH = titleContainer.getHeight() * 1.22f;
        titleBackground.setSize(titleW, titleH);
        titleContainer.setSize(titleW, titleH);
        titleContainer.setOrigin(Align.center);
        titleContainer.setX(left + (right - left - titleW) * 0.5f);
        titleContainer.setY(content.getHeight() - titleH * 0.83f);
        shrinkTitleToFit(Math.max(8f, titleW - gap * 2f));
    }

    private void shrinkTitleToFit(float titleMaxW) {
        titleLabel.setWrap(false);
        titleLabel.setVisible(true);
        titleLabel.setAlignment(Align.center);
        float baseScale = titleLabel.getFontScaleX() * 1.22f;
        titleLabel.setFontScale(baseScale);
        float prefW = titleLabel.getPrefWidth();
        if (prefW > titleMaxW && prefW > 0f) {
            titleLabel.setFontScale(baseScale * titleMaxW / prefW);
        }
        titleLabel.setWidth(titleMaxW);
        titleLabel.setX((titleContainer.getWidth() - titleMaxW) * 0.5f);
        titleLabel.setY(titleContainer.getHeight() * 0.28f - titleLabel.getPrefHeight() * 0.5f);

        Label titleShadow = new Label(titleLabel.getText(), new Label.LabelStyle(
                titleLabel.getStyle().font, UIConfig.FROSTED_ALERT_DIALOG_TEXT_SHADOW_COLOR));
        titleShadow.setAlignment(Align.center);
        titleShadow.setWrap(false);
        titleShadow.setFontScale(titleLabel.getFontScaleX());
        titleShadow.setSize(titleMaxW, titleLabel.getPrefHeight());
        titleShadow.setPosition(titleLabel.getX() + textShadowOffset, titleLabel.getY() - textShadowOffset);
        titleContainer.addActor(titleShadow);
        titleLabel.toFront();
    }

    public void updateViewWithData(float percent, int current, int target) {
        progressBar.setPercent(percent);
        String text = LanguageManager.format("extra_words_collected", current, target);
        if (countMarquee != null)
            countMarquee.setMarqueeText(text);

        if (wordsTable == null || wordsPane == null) return;
        wordsTable.clearChildren();
        float paneWidth = wordsPane.getWidth();
        float rowPad = Math.max(4f, paneWidth * 0.012f);

        IntMap<Array<String>> groups = new IntMap<Array<String>>();
        IntArray levels = new IntArray();
        Array<GameData.ExtraWordEntry> entries = GameData.getExtraWordEntries();
        for (int i = 0; i < entries.size; i++) {
            GameData.ExtraWordEntry entry = entries.get(i);
            if (entry == null || entry.word == null) continue;
            Array<String> list = groups.get(entry.levelNumber);
            if (list == null) {
                list = new Array<String>();
                groups.put(entry.levelNumber, list);
                levels.add(entry.levelNumber);
            }
            list.add(entry.word);
        }
        levels.sort();

        for (int i = 0; i < levels.size; i++) {
            int levelNumber = levels.get(i);
            Array<String> list = groups.get(levelNumber);
            if (list == null || list.size == 0) continue;

            if (levelNumber > 0) {
                addFittedRow(LanguageManager.format("level", levelNumber), headerStyle, paneWidth, 0.88f, rowPad * 2.2f, rowPad);
            }
            for (int w = 0; w < list.size; w++) {
                addFittedRow(list.get(w), wordsStyle, paneWidth, 1.08f, rowPad, rowPad * 0.4f);
            }
        }

        wordsTable.invalidateHierarchy();
        wordsTable.pack();
        wordsTable.setWidth(paneWidth);
        wordsTable.setHeight(Math.max(1f, wordsTable.getPrefHeight()));
        wordsPane.invalidateHierarchy();
        wordsPane.layout();
        wordsPane.setScrollY(0);
    }

    private void addFittedRow(String text, Label.LabelStyle style, float width, float fontScale, float padTop, float padBottom) {
        Label label = new Label(text, style);
        label.setAlignment(Align.center);
        label.setWrap(false);
        label.setFontScale(fontScale);
        float prefW = label.getPrefWidth();
        if (prefW > width && prefW > 0f) {
            label.setFontScale(fontScale * width / prefW);
        }
        label.setWrap(true);
        label.setWidth(width);
        wordsTable.add(label).width(width).padTop(padTop).padBottom(padBottom).row();
    }

    @Override
    protected void openAnimFinished() {
        super.openAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);
        checkTutorial();
    }

    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);
        remove();
    }

    private static NinePatch createRoundedFillNinePatch(float height, float radius, Color color) {
        int h = Math.max(8, Math.round(height));
        int r = Math.max(2, Math.round(radius));
        int stretch = 4;
        int w = r * 2 + stretch;
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        fillRoundedRect(pixmap, 0, 0, w, h, r, color);
        PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
        Texture tex = new Texture(texData);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return new NinePatch(tex, r, r, r, r);
    }

    private static NinePatch createRoundedStrokeNinePatch(float height, float radius, float stroke, Color color) {
        int h = Math.max(8, Math.round(height));
        int r = Math.max(2, Math.round(radius));
        int s = Math.max(1, Math.round(stroke));
        int stretch = 4;
        int w = r * 2 + stretch;
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        fillRoundedRect(pixmap, 0, 0, w, h, r, color);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        int innerR = Math.max(1, r - s);
        fillRoundedRect(pixmap, s, s, Math.max(1, w - s * 2), Math.max(1, h - s * 2), innerR, new Color(0, 0, 0, 0));
        PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
        Texture tex = new Texture(texData);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return new NinePatch(tex, r, r, r, r);
    }

    private static void fillRoundedRect(Pixmap pixmap, int x, int y, int w, int h, int radius, Color color) {
        if (w <= 0 || h <= 0) return;
        int rad = Math.min(radius, Math.min(w, h) / 2);
        pixmap.setColor(color);
        pixmap.fillRectangle(x + rad, y, Math.max(1, w - rad * 2), h);
        pixmap.fillRectangle(x, y + rad, w, Math.max(1, h - rad * 2));
        pixmap.fillCircle(x + rad, y + rad, rad);
        pixmap.fillCircle(x + w - rad - 1, y + rad, rad);
        pixmap.fillCircle(x + rad, y + h - rad - 1, rad);
        pixmap.fillCircle(x + w - rad - 1, y + h - rad - 1, rad);
    }
}
