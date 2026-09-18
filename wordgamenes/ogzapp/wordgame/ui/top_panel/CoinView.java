package ogzapp.wordgame.ui.top_panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.IntAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pools;

import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.managers.HintManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;

public class CoinView extends Group {

    public Image coin;
    private Label label;
    private float maxWidth;
    public BaseScreen screen;
    private CoinCountIncrementer coinCountIncrementer;
    private SequenceAction sequenceAction;
    private RunnableAction runnableAction;
    public ImageButton plus;
    private Runnable coinAnimFinishedCompleted;
    private boolean cancelled;
    private Image frameFill;
    private Image frameBorder;
    private float numberSlotX;
    private float numberSlotWidth;

    // Daha şeffaf buzlu cam; tam beyaz/opak kutu yok.
    private static final Color FRAME_FILL_COLOR   = new Color(0xFFFFFF48);
    private static final Color FRAME_EDGE_COLOR   = new Color(0xFFFFFF30);
    private static final String FULL_SCALE_COIN_TEXT = "9999";
    private static final String MAX_COIN_TEXT        = "99999";

    public CoinView(BaseScreen screen) {
        this.screen = screen;

        coin = new Image(AtlasRegions.coin_view_coin);
        coin.setOrigin(Align.center);
        coin.setScale(0.72f);

        boolean hasPlus = screen.wordConnectGame.shoppingProcessor != null
                && screen.wordConnectGame.shoppingProcessor.isIAPEnabled();
        if (hasPlus) {
            plus = new ImageButton(new TextureRegionDrawable(AtlasRegions.coin_view_plus_up),
                    new TextureRegionDrawable(AtlasRegions.coin_view_plus_down));
        }

        String font = UIConfig.REMAINING_COINS_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        BitmapFont bitmapFont = screen.wordConnectGame.resourceManager.get(font, BitmapFont.class);
        // Buzlu cam üzerinde beyaz okunmuyor; dial harflerinin koyu rengi.
        Color numberColor = new Color(UIConfig.getDialButtonTextColorUpStateByLevelIndex(0));
        Label.LabelStyle style = new Label.LabelStyle(bitmapFont, numberColor);
        label = new Label("", style);
        label.setAlignment(Align.right);

        GlyphLayout glyphLayout = Pools.obtain(GlyphLayout.class);
        glyphLayout.setText(bitmapFont, FULL_SCALE_COIN_TEXT);
        float numberWidth = glyphLayout.width;
        float numberHeight = glyphLayout.height;
        Pools.free(glyphLayout);

        float coinW = coin.getWidth() * coin.getScaleX();
        float coinH = coin.getHeight() * coin.getScaleY();
        float plusW = plus != null ? plus.getWidth() * 0.85f : 0f;
        float padX = Math.max(8f, coinW * 0.18f);
        float padY = Math.max(5f, coinH * 0.16f);
        // İkon ile rakam yapışmasın; slot 9999 tam boy, 99999 orantılı küçülerek sığar.
        float gap = Math.max(18f, coinW * 0.48f);
        float innerW = coinW + gap + numberWidth + (plus != null ? gap * 0.45f + plusW : 0f);
        float innerH = Math.max(coinH, Math.max(numberHeight, plus != null ? plus.getHeight() * 0.7f : 0f));
        float frameW = innerW + padX * 2f;
        float frameH = innerH + padY * 2f;
        setSize(frameW, frameH);

        float radius = frameH * 0.5f;
        float borderPx = Math.max(1.25f, frameH * 0.06f);
        frameBorder = new Image(new NinePatchDrawable(createRoundedRectNinePatch(frameH, radius, FRAME_EDGE_COLOR, FRAME_EDGE_COLOR, 0f)));
        frameBorder.setSize(frameW, frameH);
        addActor(frameBorder);

        float inset = borderPx;
        frameFill = new Image(new NinePatchDrawable(createRoundedRectNinePatch(
                Math.max(8f, frameH - inset * 2f), Math.max(2f, radius - inset), FRAME_FILL_COLOR, FRAME_FILL_COLOR, 0f)));
        frameFill.setSize(frameW - inset * 2f, frameH - inset * 2f);
        frameFill.setPosition(inset, inset);
        addActor(frameFill);

        coin.setX(padX);
        coin.setY((getHeight() - coin.getHeight()) * 0.5f + coin.getHeight() * (1f - coin.getScaleY()) * 0.15f);
        addActor(coin);

        numberSlotX = coin.getX() + coinW + gap;
        numberSlotWidth = numberWidth;
        maxWidth = numberWidth;

        if (plus != null) {
            plus.setX(getWidth() - padX - plus.getWidth() * 0.92f);
            plus.setY((getHeight() - plus.getHeight()) * 0.5f);
            addActor(plus);
        }

        addActor(label);
        update(HintManager.getRemainingCoins());
    }

    private static NinePatch createRoundedRectNinePatch(float height, float radius, Color fill, Color border, float borderPx) {
        int h = Math.max(8, Math.round(height));
        int r = Math.max(2, Math.round(radius));
        int b = Math.max(0, Math.round(borderPx));
        int stretch = 4;
        int w = r * 2 + stretch;
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setBlending(Pixmap.Blending.SourceOver);
        fillRoundedRect(pixmap, 0, 0, w, h, r, border != null ? border : fill);
        if (b > 0 && fill != null) {
            int innerR = Math.max(1, r - b);
            fillRoundedRect(pixmap, b, b, w - b * 2, h - b * 2, innerR, fill);
        } else if (fill != null) {
            fillRoundedRect(pixmap, 0, 0, w, h, r, fill);
        }
        PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
        Texture tex = new Texture(texData);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return new NinePatch(tex, r, r, r, r);
    }

    private static void fillRoundedRect(Pixmap pixmap, int x, int y, int w, int h, int radius, Color color) {
        if (w <= 0 || h <= 0) return;
        int rad = Math.min(radius, Math.min(w, h) / 2);
        pixmap.setColor(color);
        pixmap.fillRectangle(x + rad, y, Math.max(1, w - rad * 2), h);
        pixmap.fillRectangle(x, y + rad, w, Math.max(1, h - rad * 2));
        pixmap.fillCircle(x + rad, y + rad, rad);
        pixmap.fillCircle(x + w - rad - 1, y + rad, rad);
        pixmap.fillCircle(x + rad, y + h - rad - 1, rad);
        pixmap.fillCircle(x + w - rad - 1, y + h - rad - 1, rad);
    }

    public void setPlusListener(ChangeListener changeListener) {
        if (plus != null)
            plus.addListener(changeListener);
    }

    public void update(int count) {
        label.setText(String.valueOf(count));
        GlyphLayout glyphLayout = Pools.obtain(GlyphLayout.class);
        glyphLayout.setText(label.getStyle().font, label.getText());

        float scale = 1f;
        if (count > 9999 && glyphLayout.width > numberSlotWidth && glyphLayout.width > 0) {
            scale = numberSlotWidth / glyphLayout.width;
        }
        label.setFontScale(scale);
        label.setSize(numberSlotWidth, getHeight());
        label.setAlignment(Align.right | Align.center);
        label.setPosition(numberSlotX, 0f);
        Pools.free(glyphLayout);
    }

    public void incrementCoinLabelWithAnimationAndDeleteCoinImages(int startFrom, int count, Runnable callback) {
        if (coinCountIncrementer == null) {
            coinCountIncrementer = new CoinCountIncrementer();
        } else {
            coinCountIncrementer.reset();
        }

        coinCountIncrementer.setInterpolation(Interpolation.sineOut);
        coinCountIncrementer.setDuration(count * 0.05f);
        coinCountIncrementer.setStart(startFrom);
        coinCountIncrementer.setEnd(startFrom + count);

        if (callback == null) {
            label.addAction(coinCountIncrementer);
        } else {
            if (sequenceAction == null)
                sequenceAction = new SequenceAction();
            else
                sequenceAction.reset();

            if (runnableAction == null)
                runnableAction = new RunnableAction();
            else
                runnableAction.reset();

            runnableAction.setRunnable(callback);

            sequenceAction.addAction(coinCountIncrementer);
            sequenceAction.addAction(runnableAction);
            label.addAction(sequenceAction);
        }
    }

    private class CoinCountIncrementer extends IntAction {
        @Override
        protected void update(float percent) {
            super.update(percent);
            CoinView.this.update(getValue());
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel(boolean flag) {
        cancelled = flag;
    }

    private static final int MAX_ANIMATED_COINS = 6;
    private static final float COIN_ANIMATION_STAGGER = 0.06f;

    public void createCoinAnimation(int count, float x, float y, Runnable coinAnimFinishedCompleted) {
        createCoinAnimation(count, x, y, false, coinAnimFinishedCompleted);
    }

    // silent=true: her coin uçuşu tamamlandığında normalde çalan coin sesi
    // ÇALINMAZ. Diğer TÜM çağıranlar (combo, UFO vurma, bonus kelime ödülü
    // vb.) eski 4 parametreli metodu kullanmaya devam ediyor ve dolayısıyla
    // her zamanki gibi sesli çalışıyor - bu parametre SADECE açıkça
    // isteyen (silent=true veren) çağıranları etkiliyor.
    public void createCoinAnimation(int count, float x, float y, boolean silent, Runnable coinAnimFinishedCompleted) {
        this.coinAnimFinishedCompleted = coinAnimFinishedCompleted;

        int coinsToAnimate = MathUtils.clamp(count, 1, MAX_ANIMATED_COINS);

        for (int i = 0; i < coinsToAnimate; i++) {
            Coin newCoin = ogzapp.wordgame.pool.Pools.coinPool.obtain();
            newCoin.setPosition(x, y);
            addActor(newCoin);
            boolean last = i == coinsToAnimate - 1;
            newCoin.animateForCoinView(this, i * COIN_ANIMATION_STAGGER, last, count, silent);
        }
    }

    private ScaleToAction grow, shrink;
    private SequenceAction pulseSequence;
    private RunnableAction pulseRunnable;

    public void coinPulseAnimation(Runnable callback) {
        if (grow == null) grow = new ScaleToAction();
        else grow.reset();
        grow.setScale(1.1f);
        grow.setDuration(0.05f);

        if (shrink == null) shrink = new ScaleToAction();
        else shrink.reset();
        shrink.setScale(0.72f);
        shrink.setDuration(0.05f);

        if (pulseSequence == null) pulseSequence = new SequenceAction();
        else pulseSequence.reset();

        pulseSequence.addAction(grow);
        pulseSequence.addAction(shrink);

        if (callback != null) {
            if (pulseRunnable == null) pulseRunnable = new RunnableAction();
            else pulseRunnable.reset();
            pulseRunnable.setRunnable(callback);
            pulseSequence.addAction(pulseRunnable);
        }

        coin.addAction(pulseSequence);
    }

    public Runnable incrementCoinsWithAnimation(final int count) {
        return new Runnable() {
            @Override
            public void run() {
                int totalCoins = HintManager.getRemainingCoins() - count;
                incrementCoinLabelWithAnimationAndDeleteCoinImages(totalCoins, count, coinAnimFinishedCompleted);
            }
        };
    }
}