package ogzapp.wordgame.ui.dialogs.bonus_words;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.graphics.NinePatches;
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

    private ProgressBar progressBar;
    private MarqueeLabel titleMarquee;
    private MarqueeLabel countMarquee;
    private MarqueeLabel thisMarquee;
    private Label wordsLabel;
    private Label wordsShadow;
    private Group wordsTextGroup;
    private Group wordsGroup;
    private ScrollPane wordsPane;
    private GameScreen gameScreen;
    private TutorialBooster tutorialBooster;
    private float textShadowOffset;

    public BonusWordsIncompleteDialog(float width, float height, BaseScreen screen) {
        super(width, height, screen);
        gameScreen = (GameScreen) screen;

        float minSide = Math.min(width, height);
        content.setSize(Math.min(width * 0.90f, minSide * 0.92f), minSide * 0.94f);
        textShadowOffset = content.getWidth() * 0.008f;

        setContentBackground();
        // Diğer buzlu cam dialoglarla AYNI paylaşılan renk sabitleri
        // kullanılıyor (UIConfig.java'ya yeni bir şey EKLENMEDİ).
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);
        BitmapFont bodyFont = screen.wordConnectGame.resourceManager.get(ResourceManager.fontSemiBoldShadow, BitmapFont.class);
        Label.LabelStyle bodyTextStyle = new Label.LabelStyle(bodyFont, Color.WHITE);
        Label.LabelStyle bodyShadowStyle = new Label.LabelStyle(bodyFont, UIConfig.FROSTED_ALERT_DIALOG_TEXT_SHADOW_COLOR);

        setTitleLabel(LanguageManager.get("extra_words_incomplete"));
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        titleLabel.setWrap(false);
        titleLabel.setVisible(false);
        titleLabel.setFontScale(titleLabel.getFontScaleX() * 1.08f);
        titleContainer.setY(titleContainer.getY() - titleContainer.getHeight() * 0.04f);

        titleMarquee = new MarqueeLabel(titleLabel.getStyle(), bodyShadowStyle, textShadowOffset);
        titleMarquee.setFontScale(titleLabel.getFontScaleX());
        float titleMaxW = titleContainer.getWidth() * 0.72f;
        titleMarquee.setSize(titleMaxW, Math.max(titleLabel.getPrefHeight(), titleContainer.getHeight() * 0.55f));
        titleMarquee.setPosition((titleContainer.getWidth() - titleMaxW) * 0.5f,
                titleContainer.getHeight() * 0.28f - titleMarquee.getHeight() * 0.5f);
        titleMarquee.setMarqueeText(LanguageManager.get("extra_words_incomplete"));
        titleContainer.addActor(titleMarquee);

        setCloseButton();

        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });

        float pad = content.getWidth() * 0.045f;
        float barWidth = content.getWidth() * 0.78f;

        progressBar = new ProgressBar(AtlasRegions.bonus_bar_bg, AtlasRegions.bonus_words_bar_track);
        progressBar.setSize(barWidth, progressBar.getHeight());
        progressBar.setOrigin(Align.center);
        progressBar.setX((content.getWidth() - progressBar.getWidth()) * 0.5f);
        // Üstteki boşluğa doğru kaydır: başlığın hemen altı.
        progressBar.setY(titleContainer.getY() - progressBar.getHeight() - content.getHeight() * 0.03f);
        Color barBorder = new Color(0x8BB0D4CC);
        progressBar.setRoundedTrack(UIConfig.BWD_WORDS_BG_COLOR, barBorder, Math.max(2f, progressBar.getHeight() * 0.10f));
        content.addActor(progressBar);

        countMarquee = new MarqueeLabel(bodyTextStyle, bodyShadowStyle, textShadowOffset);
        countMarquee.setFontScale(0.82f);
        countMarquee.setSize(content.getWidth() * 0.90f, countMarquee.getHeight());
        countMarquee.setX((content.getWidth() - countMarquee.getWidth()) * 0.5f);
        countMarquee.setY(progressBar.getY() - countMarquee.getHeight() - content.getHeight() * 0.012f);
        countMarquee.setMarqueeText(LanguageManager.get("extra_words_collected"));
        content.addActor(countMarquee);

        Image wordsGroupBg = new Image(NinePatches.iap_card2);
        wordsGroupBg.setColor(UIConfig.BWD_WORDS_BG_COLOR);

        wordsGroup = new Group();
        wordsGroup.addActor(wordsGroupBg);
        wordsGroup.setWidth(content.getWidth() - pad * 2f);
        float wordsBottom = content.getHeight() * 0.045f;
        float wordsTop = countMarquee.getY() - content.getHeight() * 0.018f;
        wordsGroup.setHeight(Math.max(8f, wordsTop - wordsBottom));
        wordsGroup.setOrigin(Align.center);
        wordsGroup.setX((content.getWidth() - wordsGroup.getWidth()) * 0.5f);
        wordsGroup.setY(wordsBottom);
        content.addActor(wordsGroup);
        wordsGroupBg.setSize(wordsGroup.getWidth(), wordsGroup.getHeight());

        Label.LabelStyle wordsTitleStyle = new Label.LabelStyle();
        wordsTitleStyle.font = bodyFont;
        wordsTitleStyle.fontColor = UIConfig.BWD_WORDS_TITLE_COLOR;

        float headerPad = wordsGroup.getHeight() * 0.04f;
        thisMarquee = new MarqueeLabel(wordsTitleStyle, bodyShadowStyle, textShadowOffset);
        thisMarquee.setFontScale(0.90f);
        thisMarquee.setSize(wordsGroup.getWidth() * 0.90f, thisMarquee.getHeight());
        thisMarquee.setX((wordsGroup.getWidth() - thisMarquee.getWidth()) * 0.5f);
        thisMarquee.setY(wordsGroup.getHeight() - thisMarquee.getHeight() - headerPad);
        thisMarquee.setMarqueeText(LanguageManager.get("extra_words_in_this_level"));
        wordsGroup.addActor(thisMarquee);

        Label.LabelStyle wordsStyle = new Label.LabelStyle(bodyFont, UIConfig.BWD_WORDS_TEXT_COLOR);
        wordsShadow = new Label(" ", bodyShadowStyle);
        wordsShadow.setAlignment(Align.center);
        wordsShadow.setWrap(true);
        wordsShadow.setFontScale(0.92f);
        wordsLabel = new Label(" ", wordsStyle);
        wordsLabel.setAlignment(Align.center);
        wordsLabel.setWrap(true);
        wordsLabel.setFontScale(0.92f);

        wordsTextGroup = new Group();
        wordsTextGroup.addActor(wordsShadow);
        wordsTextGroup.addActor(wordsLabel);

        ScrollPane.ScrollPaneStyle paneStyle = new ScrollPane.ScrollPaneStyle();
        paneStyle.vScrollKnob = new TextureRegionDrawable(AtlasRegions.rect);

        wordsPane = new ScrollPane(wordsTextGroup, paneStyle);
        wordsPane.setScrollingDisabled(true, false);
        wordsPane.setScrollbarsVisible(false);
        wordsPane.setFadeScrollBars(true);
        wordsPane.setupFadeScrollBars(0.4f, 0.2f);
        wordsPane.setFlickScroll(true);
        float paneTop = thisMarquee.getY() - headerPad * 0.6f;
        wordsPane.setSize(wordsGroup.getWidth() * 0.94f, Math.max(24f, paneTop - wordsGroup.getHeight() * 0.03f));
        wordsPane.setX((wordsGroup.getWidth() - wordsPane.getWidth()) * 0.5f);
        wordsPane.setY(wordsGroup.getHeight() * 0.03f);
        wordsGroup.addActor(wordsPane);

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

    private String getWordList() {
        Array<String> words = GameData.getExtraWords();
        StringBuilder sb = new StringBuilder();
        String lineBreak = "";
        for (int i = 0; i < words.size; i++) {
            sb.append(lineBreak);
            sb.append(words.get(i));
            if (lineBreak.isEmpty())
                lineBreak = "\n";
        }
        return sb.toString();
    }

    public void updateViewWithData(float percent, int current, int target) {
        progressBar.setPercent(percent);
        String text = LanguageManager.format("extra_words_collected", current, target);
        if (countMarquee != null)
            countMarquee.setMarqueeText(text);

        if (wordsLabel != null && wordsGroup != null) {
            String list = getWordList();
            float paneWidth = wordsPane.getWidth();
            wordsLabel.setText(list);
            wordsShadow.setText(list);
            wordsLabel.setWidth(paneWidth);
            wordsShadow.setWidth(paneWidth);
            float listHeight = Math.max(wordsLabel.getPrefHeight(), wordsPane.getHeight());
            wordsLabel.setHeight(listHeight);
            wordsShadow.setHeight(listHeight);
            wordsLabel.setPosition(0, 0);
            wordsShadow.setPosition(textShadowOffset, -textShadowOffset);
            wordsTextGroup.setSize(paneWidth, listHeight + textShadowOffset);
            wordsPane.layout();
        }
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
}
