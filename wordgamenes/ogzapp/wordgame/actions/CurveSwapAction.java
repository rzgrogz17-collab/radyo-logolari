package ogzapp.wordgame.actions;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.actions.FloatAction;

import ogzapp.wordgame.ui.dial.Dial;
import ogzapp.wordgame.ui.dial.DialButton;


/**
 * Swaps a dial letter directly to its new slot along the ring of the dial.
 *
 * Unlike the old CurveActionIn/CurveActionOut pair, the letter never travels
 * toward the center of the dial: it always stays on (or slightly outside of)
 * its resting radius while it sweeps around to its new angle, with a small
 * outward "pop" at the midpoint and a full spin for a livelier swap feel.
 */
public class CurveSwapAction extends FloatAction {


    private DialButton dialButton;

    private float center;
    private float baseRadius;
    private float startAngle;
    private float angleDelta;



    public void init(DialButton actor, Dial dial, float targetAngle){
        this.dialButton = actor;

        center = dial.getOriginX();
        baseRadius = dial.calculateRadius();
        startAngle = actor.angle;

        // Take the shortest angular path between the old and new slot so the
        // letter always sweeps around the ring rather than through the middle.
        float rawDelta = (targetAngle - startAngle) % MathUtils.PI2;
        if(rawDelta < -MathUtils.PI) rawDelta += MathUtils.PI2;
        if(rawDelta > MathUtils.PI) rawDelta -= MathUtils.PI2;
        angleDelta = rawDelta;
    }



    @Override
    protected void update(float percent) {

        float angle = startAngle + angleDelta * percent;

        // Small outward pop at the midpoint of the swap, clamped so the
        // letter never dips below its resting radius (i.e. never shrinks
        // toward the center of the dial).
        float bulgePercent = MathUtils.clamp(percent, 0f, 1f);
        float bulge = MathUtils.sin(bulgePercent * MathUtils.PI) * 0.15f;
        float radius = baseRadius * (1f + bulge);

        float x = MathUtils.cos(angle) * radius;
        float y = MathUtils.sin(angle) * radius;

        dialButton.setX(center + x - dialButton.getOriginX());
        dialButton.setY(center + y - dialButton.getOriginY());

        dialButton.setRotation(360f * percent);
    }


}
