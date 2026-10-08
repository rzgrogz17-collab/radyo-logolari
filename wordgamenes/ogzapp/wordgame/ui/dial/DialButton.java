package ogzapp.wordgame.ui.dial;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.actions.ParallelAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

import ogzapp.wordgame.actions.CurveSwapAction;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.graphics.shader.LineShader;


public class DialButton extends Group implements Pool.Poolable {


    public static Label.LabelStyle labelStyleUp;
    public static Label.LabelStyle labelStyleDown;

    private Image background;

    public int id;
    public boolean isLineSnapped;
    public LineShader line;
    private char c;
    public Label label;
    private Image bgEffect;
    public float angle;
    private Dial dial;

    private ScaleToAction selectionScaleAction;
    private AlphaAction selectionAlphaAction;
    private ParallelAction selectionParallelAction;

    private Drawable bgDrawable;


    public DialButton(){

        if(bgDrawable == null) bgDrawable = new TextureRegionDrawable(AtlasRegions.dial_button);

        bgEffect = new Image(new TextureRegionDrawable(AtlasRegions.dial_button));
        bgEffect.setOrigin(Align.center);

        addActor(bgEffect);

        background = new Image(bgDrawable);

        setSize(background.getWidth(), background.getHeight());
        setOrigin(Align.center);
        addActor(background);

        label = createLabel(c, labelStyleUp);
        addActor(label);
    }



    private CurveSwapAction curveSwapAction;
    private RunnableAction shuffleEnd;
    private SequenceAction shuffleSequence;

    public void shuffle(float angle, Runnable callback){

        float speed = 0.35f;

        if(curveSwapAction == null) curveSwapAction = new CurveSwapAction();
        else curveSwapAction.reset();

        curveSwapAction.init(this, dial, angle);
        curveSwapAction.setDuration(speed);
        curveSwapAction.setInterpolation(Interpolation.swingOut);

        this.angle = angle;

        if(shuffleSequence == null) shuffleSequence = new SequenceAction();
        else shuffleSequence.reset();

        shuffleSequence.addAction(curveSwapAction);

        if(callback != null){
            if(shuffleEnd == null) shuffleEnd = new RunnableAction();
            else shuffleEnd.reset();

            shuffleEnd.setRunnable(callback);
            shuffleSequence.addAction(shuffleEnd);
        }

        addAction(shuffleSequence);
    }






    public void init(char c, int id, Dial dial){
        this.c = c;
        this.id = id;
        this.dial = dial;

        bgEffect.setVisible(false);

        label.setText(c+"");
        setSelected(false);
    }






    private void centerText(){
        GlyphLayout layout = Pools.obtain(GlyphLayout.class);
        layout.setText(label.getStyle().font, label.getText());
        label.setX((getWidth() - layout.width * label.getFontScaleX()) * 0.5f);
        label.setY((getHeight() - label.getHeight()) * 0.5f);
        Pools.free(layout);
    }





    private static Label createLabel(char c, Label.LabelStyle labelStyle){
        Label label = new Label(String.valueOf(c), labelStyle);
        label.setOrigin(Align.bottomLeft);
        return label;
    }





    private void resetAnimation(){
        if(selectionParallelAction == null){
            selectionParallelAction = new ParallelAction();
            selectionScaleAction = new ScaleToAction();
            selectionAlphaAction = new AlphaAction();
        }else{
            selectionParallelAction.reset();
            selectionScaleAction.reset();
            selectionAlphaAction.reset();
        }

        selectionScaleAction.setScale(1.3f);
        selectionScaleAction.setDuration(0.3f);

        selectionAlphaAction.setAlpha(0.0f);
        selectionAlphaAction.setDuration(0.3f);

        selectionParallelAction.addAction(selectionScaleAction);
        selectionParallelAction.addAction(selectionAlphaAction);
    }





    @Override
    public void setColor(Color color) {
        background.setColor(color);
        bgEffect.setColor(color);
    }




    @Override
    public boolean equals(Object o) {
        DialButton other = (DialButton)o;
        return this.id == other.id;
    }




    public char getChar(){
        return c;
    }




    public void setSelected(boolean selected){
        background.setVisible(selected);
        if(selected){
            label.setStyle(labelStyleDown);
            animateBgEffect();
        }else{
            removeLine();
            label.setStyle(labelStyleUp);
        }
    }





    private void animateBgEffect(){
        resetAnimation();
        bgEffect.setVisible(true);
        bgEffect.setScale(1.0f);
        Color color = bgEffect.getColor();
        bgEffect.setColor(color.r, color.g, color.b,1);
        bgEffect.addAction(selectionParallelAction);
    }





    public void removeLine(){
        isLineSnapped = false;

        if(line != null){
            line.setVisible(false);
        }
    }







    public void setScale(float scaleXY, int numButtons) {
        setScale(scaleXY);
        if(label != null){
            label.setFontScale((this.getHeight() * this.getScaleX() * (UIConfig.getDialButtonLetterFontScale(numButtons))) / label.getHeight());
            centerText();
        }
    }




    @Override
    public String toString() {
        return c+"";
    }




    @Override
    public void reset() {
        isLineSnapped = false;
    }


}
