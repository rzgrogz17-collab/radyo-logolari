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
import ogzapp.wordgame.util.UiUtil;

public class AlertDialog extends BaseDialog{

    protected Runnable callback;
    private TextButton okay;


    public AlertDialog(float width, float height, BaseScreen screen, String titleText, String msgText, String yesText, final Runnable okCallback) {
        this(width, height, screen, titleText, msgText, yesText, okCallback, false);
    }

    // "frostedStyle" true verilirse: Menü dialogundaki AYNI buzlu cam (yarı
    // saydam panel + görünmez başlık kutusu) görünümü + koyu (siyaha yakın)
    // yazı rengiyle çizilir. Şu an SADECE GameScreen'deki "Başarısız" (bomba
    // patlaması) dialogu bunu true olarak kullanıyor; eski 7 parametreli
    // constructor'ı çağıran TÜM diğer yerler (mağaza, GDPR/genel uyarılar
    // vb.) otomatik olarak false alır ve görünümleri hiç DEĞİŞMEZ.
    public AlertDialog(float width, float height, BaseScreen screen, String titleText, String msgText, String yesText, final Runnable okCallback, boolean frostedStyle) {
        super(width, height, screen);
        callback = okCallback;

        boolean isWide = UiUtil.isScreenWide();

        content.setWidth(isWide ? width * 0.7f : width * 0.8f);

        String font = UIConfig.DIALOG_BODY_TEXT_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        Color msgTextColor = frostedStyle ? UIConfig.FROSTED_ALERT_DIALOG_TEXT_COLOR : UIConfig.DIALOG_BODY_TEXT_COLOR;
        Label.LabelStyle msgTextStyle = new Label.LabelStyle(screen.wordConnectGame.resourceManager.get(font, BitmapFont.class), msgTextColor);
        Label desc = new Label(msgText, msgTextStyle);
        desc.setWrap(true);
        desc.setWidth(content.getWidth() * 0.8f);
        desc.setAlignment(Align.center);

        content.setHeight(desc.getHeight() + NinePatches.btn_dialog_up.getTotalHeight() * 4.5f);

        setContentBackground();
        if (frostedStyle) setContentBackgroundColor(UIConfig.FROSTED_ALERT_DIALOG_BACKGROUND_COLOR);

        setTitleLabel(titleText);
        if (frostedStyle) {
            setTitleBackgroundColor(UIConfig.FROSTED_ALERT_DIALOG_TITLE_BACKGROUND_COLOR);
            titleLabel.getStyle().fontColor = UIConfig.FROSTED_ALERT_DIALOG_TEXT_COLOR;
        }

        desc.setX((content.getWidth() - desc.getWidth()) * 0.5f);
        desc.setY(content.getHeight() * 0.5f);

        if (frostedStyle) {
            // Kullanıcı isteğiyle: açık/buzlu-cam panel üzerindeki BEYAZ
            // yazının okunabilirliği için, yazının hemen arkasına hafif
            // (yarı saydam) SİYAH bir gölge kopyası ekleniyor - klasik
            // "text shadow" tekniği: aynı yazının, çok az kaydırılmış,
            // koyu renkli bir kopyası ARKAYA (desc'ten ÖNCE) ekleniyor.
            // Orijinal yazıyla AYNI genişlik/hizalama/satır kaydırma
            // ayarları kullanılıyor ki satırlar birebir ÇAKIŞSIN.
            Label.LabelStyle shadowStyle = new Label.LabelStyle(msgTextStyle.font, UIConfig.FROSTED_ALERT_DIALOG_TEXT_SHADOW_COLOR);
            Label descShadow = new Label(msgText, shadowStyle);
            descShadow.setWrap(true);
            descShadow.setWidth(desc.getWidth());
            descShadow.setAlignment(Align.center);
            float shadowOffset = content.getWidth() * 0.006f;
            descShadow.setX(desc.getX() + shadowOffset);
            descShadow.setY(desc.getY() - shadowOffset);
            content.addActor(descShadow);
        }

        content.addActor(desc);

        TextButton.TextButtonStyle okayStyle = new TextButton.TextButtonStyle();
        String fontName = UIConfig.ALERT_DIALOG_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        okayStyle.font = screen.wordConnectGame.resourceManager.get(fontName, BitmapFont.class);
        okayStyle.up = new NinePatchDrawable(NinePatches.btn_dialog_up);
        okayStyle.down = new NinePatchDrawable(NinePatches.btn_dialog_down);

        okay = new TextButton(yesText, okayStyle);
        okay.getLabel().setFontScale(UIConfig.ALERT_DIALOG_BUTTON_FONT_SCALE);
        okay.setWidth(content.getWidth() * UIConfig.ALERT_DIALOG_BUTTON_WIDTH_COEF);
        okay.setX((content.getWidth() - okay.getWidth()) * 0.5f);
        okay.setY(getHeight() * 0.03f);
        content.addActor(okay);

        okay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                getStage().getRoot().setTouchable(Touchable.disabled);

                hide();
            }
        });
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

        if(callback != null) callback.run();
        screen.nullifyDialog(getDialogId());
    }
}
