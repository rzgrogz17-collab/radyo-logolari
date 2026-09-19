package ogzapp.wordgame.ui.dialogs;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;


public class HowToPlayDialog extends BaseDialog{

    private Label.LabelStyle bodyTextStyle;
    private float itemHeight;


    public HowToPlayDialog(float width, float height, BaseScreen screen) {
        super(width, height, screen);

        itemHeight = AtlasRegions.howtoplay_1.getRegionHeight() * 1.4f;

        // Kırmızı çerçeve alanına yakın: neredeyse tam genişlik, dial'e
        // değmeyecek kadar yüksek - uzun dillerde satırların sığması için.
        content.setSize(width * 0.92f, Math.min(height * 0.56f, Math.max(itemHeight * 4.8f, height * 0.50f)));
        setContentBackground();
        // Kullanıcı isteğiyle: diğer dialoglarla (Dil, Günlük Ödül vb.)
        // AYNI paylaşılan "buzlu cam" renk sabitleri kullanılıyor.
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);

        String font = UIConfig.DIALOG_BODY_TEXT_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        bodyTextStyle = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(font, BitmapFont.class), UIConfig.DIALOG_BODY_TEXT_COLOR);

        setTitleLabel(LanguageManager.get("how_to_play"));
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        setCloseButton();

        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });
    }





    public void setContent(String desc1, String desc2, String desc3, TextureAtlas.AtlasRegion region1, TextureAtlas.AtlasRegion region2, TextureAtlas.AtlasRegion region3){

        float areaTop = titleContainer.getY();
        float bottomPad = content.getHeight() * 0.035f;
        float gap = content.getHeight() * 0.012f;
        float sliceHeight = (areaTop - bottomPad - gap * 2f) / 3f;

        Group g3 = createSlice(sliceHeight, region3, desc3);
        g3.setY(bottomPad);
        content.addActor(g3);

        Group g2 = createSlice(sliceHeight, region2, desc2);
        g2.setY(bottomPad + sliceHeight + gap);
        content.addActor(g2);

        Group g1 = createSlice(sliceHeight, region1, desc1);
        g1.setY(bottomPad + (sliceHeight + gap) * 2f);
        content.addActor(g1);

        titleContainer.toFront();
        closeButton.toFront();
    }





    private Group createSlice(float height, TextureAtlas.AtlasRegion icon, String text){
        ClipGroup group = new ClipGroup();
        group.setSize(content.getWidth(), height);
        group.setTransform(false);

        float margin = content.getWidth() * 0.045f;

        Image img = new Image(icon);
        float iconMax = height * 0.70f;
        if (img.getHeight() > iconMax && img.getHeight() > 0) {
            float s = iconMax / img.getHeight();
            img.setSize(img.getWidth() * s, img.getHeight() * s);
        }
        img.setX(margin);
        img.setY((height - img.getHeight()) * 0.5f);
        group.addActor(img);

        float textX = img.getX() + img.getWidth() + margin;
        float textW = Math.max(8f, group.getWidth() - textX - margin * 1.15f);
        float maxTextH = height * 0.90f;

        Label descLabel = new Label(text, new Label.LabelStyle(bodyTextStyle));
        descLabel.setWrap(true);
        descLabel.setAlignment(Align.left);
        descLabel.setWidth(textW);

        float scale = 1f;
        while (descLabel.getPrefHeight() > maxTextH && scale > 0.58f) {
            scale *= 0.9f;
            descLabel.setFontScale(scale);
            descLabel.setWidth(textW);
        }
        descLabel.setHeight(Math.min(descLabel.getPrefHeight(), maxTextH));
        descLabel.setX(textX);
        descLabel.setY((height - descLabel.getHeight()) * 0.5f);

        Label.LabelStyle descShadowStyle = new Label.LabelStyle(bodyTextStyle.font, UIConfig.FROSTED_ALERT_DIALOG_TEXT_SHADOW_COLOR);
        Label descShadow = new Label(text, descShadowStyle);
        descShadow.setWrap(true);
        descShadow.setAlignment(Align.left);
        descShadow.setWidth(textW);
        descShadow.setFontScale(descLabel.getFontScaleX());
        descShadow.setHeight(descLabel.getHeight());
        float descShadowOffset = content.getWidth() * 0.006f;
        descShadow.setX(descLabel.getX() + descShadowOffset);
        descShadow.setY(descLabel.getY() - descShadowOffset);
        group.addActor(descShadow);

        group.addActor(descLabel);
        group.setOrigin(Align.center);

        return group;
    }


    private static class ClipGroup extends Group {
        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (clipBegin()) {
                super.draw(batch, parentAlpha);
                batch.flush();
                clipEnd();
            }
        }
    }







    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);
        remove();
        setVisible(false);
    }
}
