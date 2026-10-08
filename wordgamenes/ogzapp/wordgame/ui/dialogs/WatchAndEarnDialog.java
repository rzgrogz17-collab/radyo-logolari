package ogzapp.wordgame.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;


public class WatchAndEarnDialog extends BaseDialog{


    private Runnable callback;
    private boolean wantsToWatch;
    private Image coins;
    private Group countContainer;
    private TextButton watchButton;
    private float buttonScale = 1.0f;


    public WatchAndEarnDialog(float width, float height, BaseScreen screen, Runnable callback) {
        super(width, height, screen);

        content.setWidth(width * 0.7f);

        String font = UIConfig.DIALOG_BODY_TEXT_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        Label.LabelStyle bodyTextStyle = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(font, BitmapFont.class), UIConfig.DIALOG_BODY_TEXT_COLOR);

        Label adInvite = new Label(LanguageManager.format("ad_invite", GameConfig.NUMBER_OF_COINS_EARNED_FOR_WATCHING_VIDEO), bodyTextStyle);
        adInvite.setAlignment(Align.center);
        adInvite.setWrap(true);
        adInvite.setWidth(content.getWidth() * 0.8f);
        if(adInvite.getWidth() > content.getWidth() * 0.8f){
            adInvite.setFontScale(content.getWidth() * 0.8f / adInvite.getWidth());
        }

        content.setHeight(adInvite.getHeight() + NinePatches.play_r_up.getTotalHeight() + AtlasRegions.invite_coins.getRegionHeight() * 2.2f + AtlasRegions.coins_to_earn.getRegionHeight());
        setContentBackground();
        // Diğer buzlu cam dialoglarla AYNI paylaşılan renk sabitleri
        // kullanılıyor (UIConfig.java'ya yeni bir şey EKLENMEDİ).
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);

        this.callback = callback;

        setTitleLabel(LanguageManager.get("free_hints"));
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        // Başlık yazısı üst kenara çok yakın duruyordu - biraz aşağı
        // indirildi ve çok az büyütüldü (diğer 4 dialogla aynı oranlarda).
        titleLabel.setFontScale(titleLabel.getFontScaleX() * 1.10f);
        titleLabel.setY(titleContainer.getHeight() * 0.18f);

        setCloseButton();

        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });

        coins = new Image(AtlasRegions.invite_coins);
        float coinsScale = 1.42f; // orantılı olarak belirgin şekilde büyütüldü
        coins.setSize(coins.getWidth() * coinsScale, coins.getHeight() * coinsScale);
        coins.setOrigin(Align.center);
        coins.setX((content.getWidth() - coins.getWidth()) * .5f);
        coins.setY((content.getHeight() - coins.getHeight()) * 0.35f);
        content.addActor(coins);


        adInvite.setX((content.getWidth() - adInvite.getWidth()) * 0.5f);
        float dist = titleContainer.getY() - (coins.getY() + coins.getHeight());
        adInvite.setY(coins.getY() + coins.getHeight() + (dist - adInvite.getHeight()) * 0.5f);
        content.addActor(adInvite);

        TextButton.TextButtonStyle watchStyle = new TextButton.TextButtonStyle();
        String fontStr = UIConfig.INTRO_PLAY_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        watchStyle.font = screen.wordConnectGame.resourceManager.get(fontStr, BitmapFont.class);
        watchStyle.fontColor = Color.WHITE;

        watchStyle.up = new NinePatchDrawable(NinePatches.play_r_up);
        watchStyle.down = new NinePatchDrawable(NinePatches.play_r_down);

        watchButton = new TextButton(LanguageManager.get("watch"), watchStyle);
        watchButton.getLabel().setFontScale(UIConfig.WATCH_AND_EARN_DIALOG_BUTTON_FONT_SCALE);
        watchButton.getLabel().setFontScale(1.2f);
        watchButton.setWidth(content.getWidth() * UIConfig.WATCH_AND_EARN_DIALOG_BUTTON_WIDTH_COEF);

        watchButton.setOrigin(Align.center);
        watchButton.setX((content.getWidth() - watchButton.getWidth()) * 0.5f);
        watchButton.setY(getHeight() * 0.03f - (watchButton.getHeight() * (1f - buttonScale) * 0.5f));
        content.addActor(watchButton);

        watchButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                wantsToWatch = true;
                hide();
            }
        });

        Image countbg = new Image(AtlasRegions.coins_to_earn);

        Label.LabelStyle countStyle = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(ResourceManager.fontSemiBold, BitmapFont.class), Color.WHITE);
        Label count = new Label(GameConfig.NUMBER_OF_COINS_EARNED_FOR_WATCHING_VIDEO + "", countStyle);
        count.setFontScale(1.1f);
        count.setAlignment(Align.center);

        countContainer = new Group();
        countContainer.addActor(countbg);
        countContainer.addActor(count);
        countContainer.setSize(countbg.getWidth(), countbg.getHeight());
        countContainer.setOrigin(Align.center);
        countContainer.setScale(0);

        float coinArea = countbg.getWidth() * 0.298f;
        float textArea = countbg.getWidth() - coinArea;
        count.setX(coinArea + (textArea - count.getWidth()) * 0.5f);
        count.setY((countContainer.getHeight() - count.getHeight()) * 0.5f);

        dist = coins.getY() - (watchButton.getY() + watchButton.getHeight());
        countContainer.setX((content.getWidth() - countContainer.getWidth() * count.getFontScaleX()) * 0.5f);
        countContainer.setY(watchButton.getY() + watchButton.getHeight() + dist * 0.5f - countContainer.getHeight() * 0.5f);
        content.addActor(countContainer);
    }


    @Override
    public void show() {
        super.show();
        wantsToWatch = false;
    }

    @Override
    public boolean navigateBack() {
        wantsToWatch = false;
        hide();
        return true;
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

        if(wantsToWatch) {
            callback.run();
        }
    }
}
