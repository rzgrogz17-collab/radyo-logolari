package ogzapp.wordgame.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.managers.DailyRewardManager;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;
import ogzapp.wordgame.ui.dialogs.iap.ItemContent;
import ogzapp.wordgame.ui.dialogs.wheel.LuckyWheel;
import ogzapp.wordgame.ui.dialogs.wheel.Slice;
import ogzapp.wordgame.ui.hint.RewardedAdAnimation;

import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.COINS;
import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.PASS;

// GameConfig.DAILY_REWARD_WHEEL_ENABLED = true  -> kilitli tablo YOK, hediye çarkı
// GameConfig.DAILY_REWARD_WHEEL_ENABLED = false -> eski 7 günlük kilitli kutu tablosu
public class DailyRewardDialog extends BaseDialog {

    private static final Color DAY_BG_LOCKED_COLOR       = new Color(0x27394CE6);
    private static final Color DAY_BG_CLAIMED_COLOR      = new Color(0x2ECC71CC);
    private static final Color DAY_BG_TODAY_COLOR        = new Color(0x1DBAACFF);
    private static final Color DAY_TEXT_COLOR             = Color.WHITE;
    private static final Color DAY_TEXT_TODAY_COLOR       = Color.WHITE;
    private static final Color DAY_TODAY_GLOW_COLOR       = new Color(0xFFC940FF);
    private static final float DAY_LOCKED_CONTENT_ALPHA   = 0.6f;
    private static final float DAY_CLAIMED_CONTENT_ALPHA  = 0.85f;

    private static final float WHEEL_CLOSE_DURATION = 0.85f;
    private static final int MAX_PASS_SPINS = 2;

    private TextButton claimButton;
    private boolean wheelMode;

    private LuckyWheel luckyWheel;
    private TextButton spinButton;
    private int passCount;
    private boolean spinning;
    private boolean finished;

    private final Runnable spinFinished = new Runnable() {
        @Override
        public void run() {
            spinning = false;
            Slice result = luckyWheel == null ? null : luckyWheel.selectedReward;
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

    public DailyRewardDialog(float width, float height, BaseScreen screen) {
        super(width, height, screen);

        // true / false: GameConfig.DAILY_REWARD_WHEEL_ENABLED
        if (GameConfig.DAILY_REWARD_WHEEL_ENABLED) {
            setupDailyGiftWheel(width, height);
            return;
        }

        setupLockedTable(width, height);
    }

    private void setupDailyGiftWheel(float width, float height) {
        wheelMode = true;
        // Arka plan tablosu / başlık / kapatma / kilitli kutular YOK.
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

    private void startSpin() {
        if (spinning || finished || spinButton == null || spinButton.isDisabled()) return;
        spinning = true;
        spinButton.setDisabled(true);
        if (getStage() != null) getStage().getRoot().setTouchable(Touchable.disabled);
        luckyWheel.spin();
    }

    private void onPass() {
        passCount++;
        if (passCount >= MAX_PASS_SPINS) {
            DailyRewardManager.markClaimed();
            closeWheelSlowly();
            return;
        }
        spinButton.setText(LanguageManager.format("spin_again", 1));
        enableSpinAgain();
    }

    private void onCoinsWon(Slice result) {
        finished = true;
        if (spinButton != null) spinButton.setDisabled(true);

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
                        closeWheelSlowly();
                    }
                })
        ));
    }

    private void enableSpinAgain() {
        if (spinButton != null) spinButton.setDisabled(false);
        if (getStage() != null) getStage().getRoot().setTouchable(Touchable.enabled);
    }

    private void closeWheelSlowly() {
        finished = true;
        if (getStage() != null) getStage().getRoot().setTouchable(Touchable.disabled);
        if (screen.backNavQueue != null && !screen.backNavQueue.isEmpty()) {
            screen.backNavQueue.pop();
        }
        content.clearActions();
        content.addAction(new SequenceAction(
                Actions.fadeOut(WHEEL_CLOSE_DURATION),
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
    protected void openDialog() {
        if (!wheelMode) {
            super.openDialog();
            return;
        }
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

    @Override
    public boolean navigateBack() {
        if (wheelMode) return true;
        return super.navigateBack();
    }

    private void setupLockedTable(float width, float height) {
        content.setSize(width * 0.85f, height * 0.55f);

        int today = DailyRewardManager.getUpcomingStreakDay();
        int totalDays = GameConfig.DAILY_REWARD_COINS.length;
        int claimedUpTo = (today == 1) ? 0 : today - 1;

        BitmapFont dayFont = screen.wordConnectGame.resourceManager.get(ResourceManager.fontSemiBold, BitmapFont.class);
        Label.LabelStyle dayNumberStyle = new Label.LabelStyle(dayFont, DAY_TEXT_COLOR);
        Label.LabelStyle coinAmountStyle = new Label.LabelStyle(dayFont, DAY_TEXT_COLOR);

        Table table = new Table();
        table.setWidth(content.getWidth() * 0.92f);

        float cellSize = table.getWidth() * 0.22f;
        float pad = cellSize * 0.06f;

        for (int day = 1; day <= totalDays; day++) {
            boolean isToday = day == today;
            boolean isClaimed = day <= claimedUpTo;

            Group cell = new Group();
            cell.setSize(cellSize, cellSize);
            cell.setOrigin(Align.center);

            if (isToday) {
                float glowSize = cellSize * 1.35f;
                Image glow = new Image(AtlasRegions.glow);
                glow.setSize(glowSize, glowSize);
                glow.setPosition((cellSize - glowSize) * 0.5f, (cellSize - glowSize) * 0.5f);
                glow.setColor(DAY_TODAY_GLOW_COLOR);
                glow.getColor().a = 0.2f;
                cell.addActor(glow);
                glow.addAction(Actions.forever(Actions.sequence(
                        Actions.alpha(0.5f, 1.1f),
                        Actions.alpha(0.18f, 1.1f)
                )));
            }

            Image bg = new Image(NinePatches.round_rect_shadow);
            bg.setSize(cellSize, cellSize);
            bg.setColor(isToday ? DAY_BG_TODAY_COLOR : (isClaimed ? DAY_BG_CLAIMED_COLOR : DAY_BG_LOCKED_COLOR));
            cell.addActor(bg);

            Label dayLabel = new Label(LanguageManager.format("daily_reward_day", day), dayNumberStyle);
            dayLabel.setFontScale(0.58f);
            dayLabel.setAlignment(Align.center);
            dayLabel.setWidth(cellSize);
            dayLabel.setColor(isToday ? DAY_TEXT_TODAY_COLOR : DAY_TEXT_COLOR);
            dayLabel.setX(0);
            float dayLabelTopMargin = cellSize * 0.10f;
            dayLabel.setY(cellSize - dayLabel.getPrefHeight() - dayLabelTopMargin);
            cell.addActor(dayLabel);

            Label amountLabel = new Label(String.valueOf(GameConfig.DAILY_REWARD_COINS[day - 1]), coinAmountStyle);
            amountLabel.setFontScale(0.50f);
            amountLabel.setAlignment(Align.center);
            amountLabel.setWidth(cellSize);
            amountLabel.setColor(isToday ? DAY_TEXT_TODAY_COLOR : DAY_TEXT_COLOR);
            amountLabel.setX(0);
            float amountLabelBottomMargin = cellSize * 0.07f;
            amountLabel.setY(amountLabelBottomMargin);
            cell.addActor(amountLabel);

            float middleTop = dayLabel.getY();
            float middleBottom = amountLabel.getY() + amountLabel.getPrefHeight();
            float middleSpace = middleTop - middleBottom;
            Image coinIcon = new Image(AtlasRegions.coin_small);
            float gapAboveAmount = cellSize * 0.12f;
            float coinSize = Math.min(cellSize * 0.34f, Math.max(1f, (middleSpace - gapAboveAmount) * 0.78f));
            coinIcon.setSize(coinSize, coinSize);
            coinIcon.setX((cellSize - coinSize) * 0.5f);
            coinIcon.setY(middleBottom + gapAboveAmount);
            cell.addActor(coinIcon);

            if (!isToday) {
                float contentAlpha = isClaimed ? DAY_CLAIMED_CONTENT_ALPHA : DAY_LOCKED_CONTENT_ALPHA;
                dayLabel.getColor().a = contentAlpha;
                coinIcon.getColor().a = contentAlpha;
                amountLabel.getColor().a = contentAlpha;
            }

            if (isClaimed) {
                float badgeSize = cellSize * 0.22f;
                float badgeMargin = cellSize * 0.08f;
                Image check = new Image(AtlasRegions.calendar_cell_checked);
                check.setSize(badgeSize, badgeSize);
                check.setColor(DAY_TODAY_GLOW_COLOR);
                check.setPosition(cellSize - badgeSize - badgeMargin, cellSize - badgeSize - badgeMargin);
                cell.addActor(check);
            } else if (!isToday) {
                Image lock = new Image(AtlasRegions.padlock);
                float badgeSize = cellSize * 0.28f;
                lock.setSize(badgeSize, badgeSize);
                lock.setPosition(cellSize - badgeSize * 0.62f, cellSize - badgeSize * 0.62f);
                lock.setColor(DAY_TODAY_GLOW_COLOR);
                cell.addActor(lock);
            }

            if (isToday) {
                cell.addAction(Actions.forever(Actions.sequence(
                        Actions.scaleTo(1.045f, 1.045f, 1.1f),
                        Actions.scaleTo(1f, 1f, 1.1f)
                )));
            }

            table.add(cell).size(cellSize, cellSize).pad(pad);
            if (day % 4 == 0) table.row();
        }
        table.pack();

        claimButton = new TextButton(LanguageManager.format("daily_reward_claim", DailyRewardManager.getCoinsForDay(today)), buildButtonStyle());
        claimButton.getLabel().setFontScale(0.85f);
        claimButton.setWidth(table.getWidth() * 0.86f);

        float titleGap = AtlasRegions.dialog_title.getRegionHeight() * 0.2f;
        float innerGap = cellSize * 0.15f;
        float bottomGap = innerGap * 1.3f;

        content.setHeight(AtlasRegions.dialog_title.getRegionHeight() * 0.83f + titleGap
                + table.getHeight() + innerGap + claimButton.getHeight() + bottomGap);

        setContentBackground();
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);

        table.setX((content.getWidth() - table.getWidth()) * 0.5f);
        table.setY(bottomGap + claimButton.getHeight() + innerGap);
        content.addActor(table);

        claimButton.setX((content.getWidth() - claimButton.getWidth()) * 0.5f);
        claimButton.setY(bottomGap);
        content.addActor(claimButton);

        setTitleLabel(LanguageManager.get("daily_reward_title"));
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        titleContainer.setY(titleContainer.getY() - titleContainer.getHeight() * 0.08f);
        titleLabel.setY(titleLabel.getY() - titleContainer.getHeight() * 0.05f);
        setCloseButton();

        claimButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DailyRewardManager.claim();
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });
    }

    private TextButton.TextButtonStyle buildButtonStyle() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        String font = UIConfig.WHEEL_DIALOG_SPIN_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        style.font = screen.wordConnectGame.resourceManager.get(font, BitmapFont.class);
        style.fontColor = UIConfig.WHEEL_DIALOG_SPIN_BUTTON_TEXT_COLOR;
        style.up = new NinePatchDrawable(NinePatches.btn_dialog_up);
        style.down = new NinePatchDrawable(NinePatches.btn_dialog_down);
        return style;
    }

    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        if (getStage() != null) getStage().getRoot().setTouchable(Touchable.enabled);
        remove();
    }
}
