package ogzapp.wordgame.ui.dialogs.bonus_words;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pools;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.managers.HintManager;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.model.GameData;
import ogzapp.wordgame.screens.BaseScreen;
import ogzapp.wordgame.ui.ProgressBar;
import ogzapp.wordgame.ui.dialogs.BaseDialog;
import ogzapp.wordgame.ui.top_panel.TopPanel;

public class BonusWordsCompleteDialog extends BaseDialog {

    private TopPanel topPanel;
    private ProgressBar progressBar;
    private Label countLabel;
    private TextButton claim;
    private float coinViewX, coinViewY;

    public BonusWordsCompleteDialog(float width, float height, BaseScreen screen, TopPanel topPanel) {
        super(width, height, screen);
        this.topPanel = topPanel;

        // Pencereyi kare yap
        float size = Math.min(width, height) * 0.7f;
        content.setSize(size, size);

        setContentBackground();
        setContentBackgroundColor(UIConfig.BWD_DIALOG_REWARD_MODE_BG_COLOR);

        String font = UIConfig.DIALOG_BODY_TEXT_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        Label.LabelStyle bodyTextStyle = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(font, BitmapFont.class), UIConfig.DIALOG_BODY_TEXT_COLOR);

        setTitleLabel(LanguageManager.get("extra_words_complete"));

        setCloseButton();
        closeButton.setVisible(false);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });

        progressBar = new ProgressBar(AtlasRegions.bonus_bar_bg, AtlasRegions.bonus_words_bar_track);
        progressBar.setOrigin(Align.center);
        progressBar.setX((content.getWidth() - progressBar.getWidth()) * 0.5f);
        progressBar.setY(content.getHeight() * 0.4f);
        content.addActor(progressBar);

        countLabel = new Label(LanguageManager.get("extra_words_collected"), bodyTextStyle);
        countLabel.setFontScale(progressBar.getWidth() / countLabel.getWidth());
        countLabel.getStyle().fontColor = UIConfig.BWD_DIALOG_REWARD_MODE_BODY_TEXT_COLOR;
        countLabel.setY((progressBar.getY() - countLabel.getHeight() * 1.3f));
        content.addActor(countLabel);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = screen.wordConnectGame.resourceManager.get(ResourceManager.fontSemiBold, BitmapFont.class);
        style.up = new NinePatchDrawable(NinePatches.btn_dialog_up);
        style.down = new NinePatchDrawable(NinePatches.btn_dialog_down);
        style.disabled = new NinePatchDrawable(NinePatches.btn_dialog_disabled);

        claim = new TextButton(LanguageManager.get("claim"), style);
        claim.setWidth(progressBar.getWidth());
        claim.setOrigin(Align.center);
        claim.setX((content.getWidth() - claim.getWidth()) * 0.5f);
        claim.setY(getHeight() * 0.03f);
        claim.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                claimCoins();
            }
        });
        content.addActor(claim);

        Image reward_count_bg = new Image(AtlasRegions.reward_count_bg);
        reward_count_bg.setOrigin(Align.center);
        reward_count_bg.setX((content.getWidth() - reward_count_bg.getWidth()) * 0.5f);

        // Reward badge now sits in the empty area above the progress bar
        // (where the removed gift graphic used to be) instead of overlapping
        // the "Topla" claim button and the count label below the bar.
        float progressBarTop = progressBar.getY() + progressBar.getHeight();
        float titleBottom = titleContainer.getY();
        float giftAreaHeight = titleBottom - progressBarTop;
        reward_count_bg.setY(progressBarTop + (giftAreaHeight - reward_count_bg.getHeight()) * 0.5f);
        content.addActor(reward_count_bg);

        Image coin = new Image(AtlasRegions.coin_small);
        coin.setOrigin(Align.center);
        coin.setX(reward_count_bg.getX() + reward_count_bg.getWidth() * 0.07f);
        coin.setY(reward_count_bg.getY() + (reward_count_bg.getHeight() - coin.getHeight()) * 0.5f);
        content.addActor(coin);

        Label.LabelStyle rewardStyle = new Label.LabelStyle(bodyTextStyle);
        rewardStyle.fontColor = Color.BLACK;

        Label rewardLabel = new Label("+" + GameConfig.NUMBER_OF_COINS_AWARDED_FOR_BONUS_WORDS_REWARD, rewardStyle);

        Group rewardLabelContainer = new Group();
        rewardLabelContainer.addActor(rewardLabel);
        rewardLabelContainer.setSize(rewardLabel.getWidth(), rewardLabel.getHeight());
        rewardLabelContainer.setOrigin(Align.center);
        rewardLabelContainer.setX(reward_count_bg.getX() + reward_count_bg.getWidth() * 0.46f);
        rewardLabelContainer.setY(reward_count_bg.getY() + (reward_count_bg.getHeight() - rewardLabelContainer.getHeight()) * 0.5f);
        content.addActor(rewardLabelContainer);

        // Hediye logosu (rays, boxOpened, glitter) kaldırıldı.
    }

    private void claimCoins() {
        if (!claim.isVisible())
            return;

        getStage().getRoot().setTouchable(Touchable.disabled);
        claim.setDisabled(true);

        coinViewX = topPanel.coinView.getX();
        coinViewY = topPanel.coinView.getY();

        Vector2 pos1 = topPanel.coinView.localToActorCoordinates(content, new Vector2());

        topPanel.coinView.remove();
        topPanel.coinView.setPosition(pos1.x, pos1.y);
        if (topPanel.coinView.plus != null)
            topPanel.coinView.plus.setDisabled(true);
        content.addActor(topPanel.coinView);

        Vector2 pos = topPanel.coinView.localToActorCoordinates(content, new Vector2(0, 0));
        topPanel.coinView.createCoinAnimation(GameConfig.NUMBER_OF_COINS_AWARDED_FOR_BONUS_WORDS_REWARD, pos.x, pos.y, coinAnimComplete);
    }

    private Runnable coinAnimComplete = new Runnable() {
        @Override
        public void run() {
            topPanel.coinView.addAction(new SequenceAction(Actions.fadeOut(0.1f), Actions.run(closeAfterCoinAnim)));
        }
    };

    @Override
    public boolean navigateBack() {
        if (claim.isDisabled()) {
            topPanel.coinView.cancel(true);
            topPanel.coinView.update(HintManager.getRemainingCoins());
            closeAfterCoinAnim.run();
            return true;
        }
        return super.navigateBack();
    }

    private Runnable closeAfterCoinAnim = new Runnable() {
        @Override
        public void run() {
            topPanel.coinView.remove();
            topPanel.coinView.setPosition(coinViewX, coinViewY);
            if (topPanel.coinView.plus != null)
                topPanel.coinView.plus.setDisabled(true);
            topPanel.coinView.getColor().a = 1f;
            topPanel.addActor(topPanel.coinView);
            topPanel.coinView.cancel(false);
            hide();
        }
    };

    @Override
    public void show() {
        super.show();
        GameData.resetExtraWordCount();
        GameData.clearExtraWords();
        int current = HintManager.getRemainingCoins();
        HintManager.setCoinCount(current + GameConfig.NUMBER_OF_COINS_AWARDED_FOR_BONUS_WORDS_REWARD);
        int target = GameConfig.NUMBER_OF_BONUS_WORDS_TO_FIND_FOR_REWARD;
        updateViewWithData(1f, target, target);
    }

    public void updateViewWithData(float percent, int current, int target) {
        progressBar.setPercent(percent);
        String text = LanguageManager.format("extra_words_collected", current, target);
        countLabel.setText(text);
        GlyphLayout countLayout = Pools.obtain(GlyphLayout.class);
        countLayout.setText(countLabel.getStyle().font, text);

        countLabel.setFontScale(progressBar.getWidth() / countLabel.getWidth());
        Pools.free(countLayout);
        countLabel.setX((content.getWidth() - countLabel.getWidth() * countLabel.getFontScaleX()) * 0.5f);
        claim.setDisabled(false);
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
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        // rays döndürme kaldırıldı
    }
}