package ogzapp.wordgame.ui.top_panel;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import ogzapp.wordgame.config.ConfigProcessor;
import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;
import ogzapp.wordgame.screens.GameScreen;
import ogzapp.wordgame.util.UiUtil;


public class TopPanel extends Group {


    public CoinView coinView;
    public TopComboAndLevelDisplay topComboDisplay;
    public BaseScreen screen;
    public ImageButton btnMenu;
    public ImageButton backBtn;
    private float width;


    public TopPanel(BaseScreen screen, float width){
        this.screen = screen;
        this.width = width;
        ResourceManager resourceManager = screen.wordConnectGame.resourceManager;

        float sayi= (AtlasRegions.coin_view_bg.originalHeight+screen.wordConnectGame.getYukseklik()) * (UiUtil.isScreenWide() ? UIConfig.MARGIN_TOP_WIDE_SCREEN : UIConfig.MARGIN_TOP_NORMAL_SCREEN);
        setWidth(width);
        setHeight(sayi);

        if(screen instanceof GameScreen) {
            topComboDisplay = new TopComboAndLevelDisplay(resourceManager);
            topComboDisplay.setY((getHeight() - topComboDisplay.getHeight()) * 0.5f);
            addActor(topComboDisplay);

            if(!GameConfig.SKIP_INTRO) {
                backBtn = new ImageButton(new TextureRegionDrawable(AtlasRegions.back_up), new TextureRegionDrawable(AtlasRegions.back_down));
                addActor(backBtn);
                backBtn.setTransform(true);
                backBtn.setOrigin(Align.center);
                backBtn.setScale(0.88f);
                // Dışa (sola) yarı ebat + Seviye yazısına doğru yarım ikon.
                backBtn.setX(backBtn.getWidth());
                backBtn.setY((getHeight() - backBtn.getHeight()) * 0.5f);
                backBtn.addListener(((GameScreen) screen).gotoIntroScreen);
            }
        }

        coinView = new CoinView(screen);
        coinView.setX(width - coinView.getWidth());
        coinView.setY((getHeight() - coinView.getHeight()) * 0.5f);
        addActor(coinView);

        boolean inEu = screen.wordConnectGame.adManager != null && screen.wordConnectGame.adManager.isUserInEU();
        if(ConfigProcessor.isMenuEnabled(inEu, GameConfig.availableLanguages.size() > 1)) {
            btnMenu = new ImageButton(new TextureRegionDrawable(AtlasRegions.settings_up), new TextureRegionDrawable(AtlasRegions.settings_down));
            addActor(btnMenu);
            if(backBtn != null) btnMenu.setX(backBtn.getX() + backBtn.getWidth() * 1.2f);
            else btnMenu.setX(btnMenu.getWidth());

            btnMenu.setY((getHeight() - btnMenu.getHeight()) * 0.5f);
        }

        if(topComboDisplay != null ) {
            // Üst barın TAM ORTASI (geri ile coin arasında, soldan sağa eşit).
            topComboDisplay.setX(0);
            topComboDisplay.setWidth(width);
            topComboDisplay.setComboCount(0, null);
        }

        if (backBtn != null) backBtn.toFront();
        coinView.toFront();


    }





    public void addMenuButtonListener(ChangeListener changeListener){
        if(btnMenu != null) btnMenu.addListener(changeListener);
    }


}
