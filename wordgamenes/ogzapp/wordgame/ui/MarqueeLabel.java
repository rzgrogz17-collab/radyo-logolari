package ogzapp.wordgame.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;

/**
 * Tek satır yazı sığmazsa bant gibi soldan sağa kayarak okunur.
 * Sığarsa ortalanır. Gölge etiketi isteğe bağlıdır.
 */
public class MarqueeLabel extends Group {

    private final Label label;
    private final Label shadow;
    private final Group textGroup;
    private final float shadowOffset;
    private float pixelsPerSecond = 46f;
    private float pause = 0.85f;
    private boolean layingOut;

    public MarqueeLabel(Label.LabelStyle style) {
        this(style, null, 0f);
    }

    public MarqueeLabel(Label.LabelStyle style, Label.LabelStyle shadowStyle, float shadowOffset) {
        setTouchable(Touchable.disabled);
        setTransform(false);
        this.shadowOffset = shadowOffset;

        textGroup = new Group();
        if (shadowStyle != null) {
            shadow = new Label("", shadowStyle);
            shadow.setAlignment(Align.left);
            textGroup.addActor(shadow);
        } else {
            shadow = null;
        }
        label = new Label("", style);
        label.setAlignment(Align.left);
        textGroup.addActor(label);
        addActor(textGroup);
    }

    public void setFontScale(float scale) {
        label.setFontScale(scale);
        if (shadow != null) shadow.setFontScale(scale);
        if (getWidth() > 0) relayout();
    }

    public void setMarqueeText(CharSequence text) {
        label.setText(text);
        if (shadow != null) shadow.setText(text);
        relayout();
    }

    public Label getLabel() {
        return label;
    }

    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        if (label != null && getWidth() > 0 && !layingOut) relayout();
    }

    public void relayout() {
        if (label == null || layingOut) return;
        layingOut = true;
        try {
            textGroup.clearActions();
            float textW = Math.max(1f, label.getPrefWidth());
            float textH = Math.max(1f, label.getPrefHeight());
            if (getHeight() < textH) super.setSize(getWidth(), textH);

            label.setSize(textW, textH);
            label.setPosition(0, 0);
            if (shadow != null) {
                shadow.setSize(textW, textH);
                shadow.setPosition(shadowOffset, -shadowOffset);
            }
            textGroup.setSize(textW + shadowOffset, textH + shadowOffset);

            float y = (getHeight() - textH) * 0.5f;
            if (textW <= getWidth() + 0.5f) {
                textGroup.setPosition((getWidth() - textW) * 0.5f, y);
                return;
            }

            textGroup.setPosition(0, y);
            float dist = textW - getWidth();
            float dur = Math.max(1.4f, dist / pixelsPerSecond);
            textGroup.addAction(Actions.forever(Actions.sequence(
                    Actions.delay(pause),
                    Actions.moveTo(-dist, y, dur),
                    Actions.delay(pause * 0.55f),
                    Actions.moveTo(0, y)
            )));
        } finally {
            layingOut = false;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (clipBegin()) {
            super.draw(batch, parentAlpha);
            batch.flush();
            clipEnd();
        }
    }
}
