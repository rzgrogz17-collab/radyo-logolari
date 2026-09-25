package ogzapp.wordgame.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;

import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;


public class ConfirmDialog extends BaseDialog{


    private ConfirmCallback callback;
    private String clickedButtonLabel;
    private TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
    private TextButton yes, no;


    public ConfirmDialog(float width, float height, BaseScreen screen, String titleText, String msgText, String yesText, String noText) {
        super(width, height, screen);

        content.setSize(width * 0.7f, height * 0.6f);

        String font = UIConfig.DIALOG_BODY_TEXT_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        Label.LabelStyle msgStyle = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(font, BitmapFont.class), UIConfig.DIALOG_BODY_TEXT_COLOR);
        Label msgLabel = new Label(msgText, msgStyle);
        msgLabel.setFontScale(UIConfig.DIALOG_BODY_TEXT_FONT_SCALE);
        msgLabel.setWrap(true);
        msgLabel.setWidth(content.getWidth() * 0.8f);
        msgLabel.setAlignment(Align.center);

        content.setHeight(msgLabel.getHeight() + NinePatches.play_r_up.getTotalHeight() * 3.5f);

        setContentBackground();
        // Referans görseldeki buzlu cam panel görünümü: Diğer dialoglarla
        // AYNI paylaşılan renk sabitleri kullanılıyor (Menu ekranında
        // eklenen MENU_DIALOG_BACKGROUND_COLOR / MENU_DIALOG_TITLE_
        // BACKGROUND_COLOR) - UIConfig.java'ya yeni bir şey EKLENMEDİ.
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);
        setTitleLabel(titleText);
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        // Başlık yazısı üst kenara çok yakın duruyordu - biraz aşağı
        // indirildi ve çok az büyütüldü (diğer 4 dialogla aynı oranlarda).
        titleLabel.setFontScale(titleLabel.getFontScaleX() * 1.10f);
        titleLabel.setY(titleContainer.getHeight() * 0.18f);


        msgLabel.setX((content.getWidth() - msgLabel.getWidth()) * 0.5f);
        msgLabel.setY(content.getHeight() * 0.5f);

        String fontName = UIConfig.CONFIRM_DIALOG_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        buttonStyle.font = screen.wordConnectGame.resourceManager.get(fontName, BitmapFont.class);
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.up = new NinePatchDrawable(NinePatches.play_r_up);
        buttonStyle.down = new NinePatchDrawable(NinePatches.play_r_down);
        buttonStyle.disabled = new NinePatchDrawable(NinePatches.play_r_down);
        content.addActor(msgLabel);

        float bWidth = content.getWidth() * UIConfig.CONFIRM_DIALOG_BUTTON_WIDTH_COEF;
        float halfWidth = content.getWidth() * 0.5f;

        yes = getButton(yesText);
        yes.setWidth(bWidth);
        yes.setX((halfWidth - bWidth * yes.getScaleX()) * 0.5f);
        yes.setY(Math.max(10f, NinePatches.play_r_up.getTotalHeight() * 0.18f));
        content.addActor(yes);

        no = getButton(noText);
        no.setWidth(bWidth);
        no.setX(halfWidth + (halfWidth - bWidth * no.getScaleX()) * 0.5f);
        no.setY(yes.getY());
        content.addActor(no);

        yes.addListener(changeListener);
        no.addListener(changeListener);

        clickedButtonLabel = noText;
    }




    private TextButton getButton(String text){
        TextButton button = new TextButton(text, buttonStyle);
        button.setTransform(true);
        button.setScale(UIConfig.CONFIRM_DIALOG_BUTTON_SCALE);
        button.setHeight(Math.max(button.getPrefHeight(), NinePatches.play_r_up.getTotalHeight()));
        button.getLabel().setFontScale(UIConfig.CONFIRM_DIALOG_BUTTON_FONT_SCALE);
        button.getLabel().setColor(Color.WHITE);
        return button;
    }



    private ChangeListener changeListener = new ChangeListener() {

        @Override
        public void changed(ChangeEvent event, Actor actor) {
            getStage().getRoot().setTouchable(Touchable.disabled);
            TextButton textButton = (TextButton)actor;
            clickedButtonLabel = textButton.getLabel().getText().toString();
            hide();
        }
    };






    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);

        if(callback != null){
            callback.confirmClicked(clickedButtonLabel);
        }
        screen.nullifyDialog(getDialogId());
    }






    public void setConfirmCallback(ConfirmCallback callback){
        this.callback = callback;
    }



    public interface ConfirmCallback{
        void confirmClicked(String buttonLabel);
    }


}
