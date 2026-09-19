package ogzapp.wordgame.ui.dialogs.bonus_words;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
import com.badlogic.gdx.utils.Pools;
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
import ogzapp.wordgame.ui.ProgressBar;
import ogzapp.wordgame.ui.dialogs.BaseDialog;
import ogzapp.wordgame.ui.tutorial.Tutorial;
import ogzapp.wordgame.ui.tutorial.TutorialBooster;

public class BonusWordsIncompleteDialog extends BaseDialog {

    private ProgressBar progressBar;
    private Label countLabel;
    private Label countShadow;
    private Label wordsLabel;
    private Label wordsShadow;
    private Group wordsTextGroup;
    private GameScreen gameScreen;
    private TutorialBooster tutorialBooster;
    private float textShadowOffset;

    public BonusWordsIncompleteDialog(float width, float height, BaseScreen screen) {
        super(width, height, screen);
        gameScreen = (GameScreen) screen;

        float minSide = Math.min(width, height);
        content.setSize(minSide * 0.76f, minSide * 0.80f);
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
        titleLabel.setFontScale(titleLabel.getFontScaleX() * 1.12f);
        titleLabel.setY(titleContainer.getHeight() * 0.28f - titleLabel.getPrefHeight() * 0.5f);
        titleContainer.setY(titleContainer.getY() - titleContainer.getHeight() * 0.08f);
        addDropShadowBehind(titleLabel, titleContainer, textShadowOffset);
        setCloseButton();

        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });

        // Hediye logosu (boxClosed ve badgeGroup) kaldırıldı.

        progressBar = new ProgressBar(AtlasRegions.bonus_bar_bg, AtlasRegions.bonus_words_bar_track);
        progressBar.setOrigin(Align.center);
        progressBar.setX((content.getWidth() - progressBar.getWidth()) * 0.5f);
        progressBar.setY(content.getHeight() * 0.58f);
        // Boş şerit: alttaki bulunan-kelime kutusunun koyu lacivert rengi.
        // Kenar: hafif belirgin, yuvarlak/kapsül çerçeve.
        Color barBorder = new Color(0x8BB0D4CC);
        progressBar.setRoundedTrack(UIConfig.BWD_WORDS_BG_COLOR, barBorder, Math.max(2f, progressBar.getHeight() * 0.10f));
        content.addActor(progressBar);

        countShadow = new Label(LanguageManager.get("extra_words_collected"), bodyShadowStyle);
        countShadow.setFontScale(0.82f);
        countLabel = new Label(LanguageManager.get("extra_words_collected"), bodyTextStyle);
        countLabel.setFontScale(0.82f);
        countLabel.setY(progressBar.getY() - countLabel.getPrefHeight() * 1.15f);
        countShadow.setY(countLabel.getY() - textShadowOffset);
        content.addActor(countShadow);
        content.addActor(countLabel);

        Image wordsGroupBg = new Image(NinePatches.iap_card2);
        wordsGroupBg.setColor(UIConfig.BWD_WORDS_BG_COLOR);

        Group wordsGroup = new Group();
        wordsGroup.addActor(wordsGroupBg);
        wordsGroup.setWidth(progressBar.getWidth() * 1.05f);
        wordsGroup.setHeight(Math.max(countLabel.getY() * 0.82f, content.getHeight() * 0.28f));
        wordsGroup.setOrigin(Align.center);

        wordsGroup.setX((content.getWidth() - wordsGroup.getWidth()) * 0.5f);
        wordsGroup.setY(Math.max(content.getHeight() * 0.06f, (countLabel.getY() - wordsGroup.getHeight()) * 0.42f));
        content.addActor(wordsGroup);

        wordsGroupBg.setSize(wordsGroup.getWidth(), wordsGroup.getHeight());

        Label.LabelStyle wordsTitleStyle = new Label.LabelStyle();
        wordsTitleStyle.font = bodyFont;
        wordsTitleStyle.fontColor = UIConfig.BWD_WORDS_TITLE_COLOR;

        Label thisLabel = new Label(LanguageManager.get("extra_words_in_this_level"), wordsTitleStyle);
        thisLabel.setFontScale(0.92f);
        thisLabel.setAlignment(Align.center);
        float maxLabelWidth = wordsGroup.getWidth() * 0.9f;
        thisLabel.setWidth(maxLabelWidth);
        thisLabel.setWrap(true);
        thisLabel.setHeight(thisLabel.getPrefHeight());
        thisLabel.setX((wordsGroup.getWidth() - maxLabelWidth) * 0.5f);
        thisLabel.setY(wordsGroup.getHeight() - thisLabel.getHeight() * 1.05f);

        Label thisShadow = new Label(LanguageManager.get("extra_words_in_this_level"), bodyShadowStyle);
        thisShadow.setFontScale(0.92f);
        thisShadow.setAlignment(Align.center);
        thisShadow.setWidth(maxLabelWidth);
        thisShadow.setWrap(true);
        thisShadow.setHeight(thisLabel.getHeight());
        thisShadow.setX(thisLabel.getX() + textShadowOffset);
        thisShadow.setY(thisLabel.getY() - textShadowOffset);
        wordsGroup.addActor(thisShadow);
        wordsGroup.addActor(thisLabel);

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

        ScrollPane pane = new ScrollPane(wordsTextGroup, paneStyle);
        pane.setScrollbarsVisible(false);
        pane.setSize(wordsGroup.getWidth(), thisLabel.getY() * 0.9f);
        pane.setY(wordsGroup.getHeight() * 0.03f);
        pane.setupFadeScrollBars(0, 0);

        wordsGroup.addActor(pane);
    }

    private void addDropShadowBehind(Label source, Group parent, float offset) {
        Label.LabelStyle shadowStyle = new Label.LabelStyle(source.getStyle().font, UIConfig.FROSTED_ALERT_DIALOG_TEXT_SHADOW_COLOR);
        Label shadow = new Label(source.getText(), shadowStyle);
        shadow.setFontScale(source.getFontScaleX(), source.getFontScaleY());
        shadow.setAlignment(Align.center);
        shadow.setWidth(source.getWidth());
        shadow.setWrap(true);
        shadow.setX(source.getX() + offset);
        shadow.setY(source.getY() - offset);
        int idx = parent.getChildren().indexOf(source, true);
        if (idx >= 0) parent.addActorAt(idx, shadow);
        else parent.addActor(shadow);
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
        countLabel.setText(text);
        GlyphLayout countLayout = Pools.obtain(GlyphLayout.class);
        countLayout.setText(countLabel.getStyle().font, text);
        float targetWidth = progressBar.getWidth() * 0.98f;
        float scale = countLayout.width > 0 ? targetWidth / countLayout.width : 0.82f;
        countLabel.setFontScale(Math.max(0.72f, Math.min(0.92f, scale)));
        Pools.free(countLayout);

        countLabel.setX((content.getWidth() - countLabel.getWidth() * countLabel.getFontScaleX()) * 0.5f);
        countShadow.setText(text);
        countShadow.setFontScale(countLabel.getFontScaleX());
        countShadow.setX(countLabel.getX() + textShadowOffset);
        countShadow.setY(countLabel.getY() - textShadowOffset);

        if (wordsLabel != null) {
            String list = getWordList();
            float paneWidth = progressBar.getWidth() * 1.05f;
            wordsLabel.setText(list);
            wordsShadow.setText(list);
            wordsLabel.setWidth(paneWidth);
            wordsShadow.setWidth(paneWidth);
            float listHeight = Math.max(wordsLabel.getPrefHeight(), 1f);
            wordsLabel.setHeight(listHeight);
            wordsShadow.setHeight(listHeight);
            wordsLabel.setPosition(0, 0);
            wordsShadow.setPosition(textShadowOffset, -textShadowOffset);
            wordsTextGroup.setSize(paneWidth + textShadowOffset, listHeight + textShadowOffset);
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
