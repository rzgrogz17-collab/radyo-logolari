package ogzapp.wordgame.ui.dialogs.wheel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.managers.DailyRewardManager;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;
import ogzapp.wordgame.ui.dialogs.BaseDialog;
import ogzapp.wordgame.ui.dialogs.iap.ItemContent;
import ogzapp.wordgame.ui.hint.RewardedAdAnimation;

import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.COINS;
import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.PASS;


/**
 * Günlük hediye çarkı: tablo/panel yok, yalnızca çark + altındaki yeşil buton.
 * Aç/kapa: GameConfig.DAILY_REWARD_WHEEL_ENABLED
 */
public class DailyGiftWheelDialog extends BaseDialog {

    private static final float CLOSE_DURATION = 0.85f;
    private static final int MAX_PASS_SPINS = 2;

    private LuckyWheel luckyWheel;
    private TextButton spinButton;
    private int passCount;
    private boolean spinning;
    private boolean finished;

    private final Runnable spinFinished = new Runnable() {
        @Override
        public void run() {
            spinning = false;
            Slice result = luckyWheel.selectedReward;
            if (result == null) {
                enableSpinAgain();
                return;
            }
            if (result.reward == PASS) {
                onPass();
            } else {
                onCoinsWon(result);
            }
        }
    };


    public DailyGiftWheelDialog(float width, float height, BaseScreen screen) {
        super(width, height, screen);

        // Arka plan tablosu / başlık / kapatma yok. Tam ekran içerik.
        content.setSize(width, height);
        content.setOrigin(Align.center);

        luckyWheel = new LuckyWheel(spinFinished, screen.wordConnectGame.resourceManager, GameConfig.dailyGiftSlices);
        luckyWheel.setOrigin(Align.center);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        String font = UIConfig.INTRO_PLAY_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        style.font = screen.wordConnectGame.resourceManager.get(font, BitmapFont.class);
        style.fontColor = Color.WHITE;
        style.up = new NinePatchDrawable(NinePatches.play_r_up);
        style.down = new NinePatchDrawable(NinePatches.play_r_down);
        style.disabled = new NinePatchDrawable(NinePatches.play_r_down);

        spinButton = new TextButton(LanguageManager.get("spin_btn_label"), style);
        spinButton.getLabel().setFontScale(UIConfig.WATCH_AND_EARN_DIALOG_BUTTON_FONT_SCALE);
        spinButton.setWidth(Math.min(luckyWheel.getWidth() * 0.78f, width * 0.62f));
        spinButton.setHeight(Math.max(spinButton.getPrefHeight(), NinePatches.play_r_up.getTotalHeight()));
        spinButton.setOrigin(Align.center);

        float gap = Math.max(18f, luckyWheel.getHeight() * 0.07f);
        float blockHeight = luckyWheel.getHeight() + gap + spinButton.getHeight();
        float blockY = (height - blockHeight) * 0.52f;

        luckyWheel.setX((width - luckyWheel.getWidth()) * 0.5f);
        luckyWheel.setY(blockY + spinButton.getHeight() + gap);
        spinButton.setX((width - spinButton.getWidth()) * 0.5f);
        spinButton.setY(blockY);

        content.addActor(luckyWheel);
        content.addActor(spinButton);

        spinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startSpin();
            }
        });
    }


    @Override
    protected void openDialog() {
        content.setScale(1f);
        content.getColor().a = 0f;
        Action run = Actions.run(new Runnable() {
            @Override
            public void run() {
                openAnimFinished();
            }
        });
        content.addAction(new SequenceAction(Actions.fadeIn(0.25f), run));
    }


    private void startSpin() {
        if (spinning || finished || spinButton.isDisabled()) return;
        spinning = true;
        spinButton.setDisabled(true);
        if (getStage() != null) getStage().getRoot().setTouchable(Touchable.disabled);
        luckyWheel.spin();
    }


    private void onPass() {
        passCount++;
        if (passCount >= MAX_PASS_SPINS) {
            DailyRewardManager.markClaimed();
            closeSlowly();
            return;
        }
        spinButton.setText(LanguageManager.format("spin_again", 1));
        enableSpinAgain();
    }


    private void onCoinsWon(Slice result) {
        finished = true;
        spinButton.setDisabled(true);

        int coins = result.reward == COINS ? Math.max(0, result.quantity) : 0;
        DailyRewardManager.markClaimed();

        if (coins > 0) {
            ItemContent reward = new ItemContent();
            reward.coins = coins;
            screen.updateCoinsAndHints(reward);

            RewardedAdAnimation coinText = new RewardedAdAnimation(screen, coins);
            if (getStage() != null) getStage().addActor(coinText);
            coinText.show();
        }

        addAction(Actions.sequence(
                Actions.delay(0.35f),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        closeSlowly();
                    }
                })
        ));
    }


    private void enableSpinAgain() {
        spinButton.setDisabled(false);
        if (getStage() != null) getStage().getRoot().setTouchable(Touchable.enabled);
    }


    private void closeSlowly() {
        finished = true;
        if (getStage() != null) getStage().getRoot().setTouchable(Touchable.disabled);
        if (screen.backNavQueue != null && !screen.backNavQueue.isEmpty()) {
            screen.backNavQueue.pop();
        }
        content.clearActions();
        content.addAction(new SequenceAction(
                Actions.fadeOut(CLOSE_DURATION),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        modal.addAction(Actions.sequence(
                                Actions.fadeOut(0.2f),
                                Actions.run(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideAnimFinished();
                                    }
                                })
                        ));
                    }
                })
        ));
    }


    @Override
    public boolean navigateBack() {
        return true;
    }


    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        if (getStage() != null) getStage().getRoot().setTouchable(Touchable.enabled);
        remove();
    }
}
