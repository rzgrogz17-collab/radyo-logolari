package ogzapp.wordgame.ui.dialogs.wheel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RotateToAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;



import java.util.HashMap;
import java.util.Map;

import ogzapp.wordgame.actions.Interpolation;
import ogzapp.wordgame.config.ConfigProcessor;
import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.SoundConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.managers.ResourceManager;


import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.COINS;
import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.FINGER_REVEAL;
import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.MULTI_RANDOM_REVEAL;
import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.ROCKET_REVEAL;
import static ogzapp.wordgame.ui.dialogs.wheel.RewardRevealType.SINGLE_RANDOM_REVEAL;


public class LuckyWheel extends Group {

    public Slice selectedReward;

    private Array<Integer> randomWeights;
    private Array<Integer> sectorAngles;
    private final int TOTAL_SLICES = 8;

    private Image[] leds;

    private Map<RewardRevealType, Drawable> rewardTypeToIconMappingSmall;
    public Map<RewardRevealType, Drawable> rewardTypeToIconMappingBig;

    private Group background;
    private ResourceManager resourceManager;
    private Image arrow;
    private Runnable spinFinished;
    private Slice[] sliceConfig;

    public LuckyWheel(Runnable spinFinished, ResourceManager resourceManager){
        this(spinFinished, resourceManager, GameConfig.slices);
    }

    public LuckyWheel(Runnable spinFinished, ResourceManager resourceManager, Slice[] slices){

        this.spinFinished = spinFinished;
        this.resourceManager = resourceManager;
        this.sliceConfig = (slices != null && slices.length > 0) ? slices : GameConfig.slices;

        rewardTypeToIconMappingSmall = new HashMap<>();
        rewardTypeToIconMappingSmall.put(COINS, new TextureRegionDrawable(AtlasRegions.coin_small));
        rewardTypeToIconMappingSmall.put(SINGLE_RANDOM_REVEAL, new TextureRegionDrawable(AtlasRegions.random_hint_small));
        rewardTypeToIconMappingSmall.put(FINGER_REVEAL, new TextureRegionDrawable(AtlasRegions.finger_hint_small));
        rewardTypeToIconMappingSmall.put(MULTI_RANDOM_REVEAL, new TextureRegionDrawable(AtlasRegions.multi_random_hint_small));
        rewardTypeToIconMappingSmall.put(ROCKET_REVEAL, new TextureRegionDrawable(AtlasRegions.rocket_small));

        rewardTypeToIconMappingBig = new HashMap<>();
        rewardTypeToIconMappingBig.put(COINS, new TextureRegionDrawable(AtlasRegions.coin_big));
        rewardTypeToIconMappingBig.put(SINGLE_RANDOM_REVEAL, new TextureRegionDrawable(AtlasRegions.random_hint_big));
        rewardTypeToIconMappingBig.put(FINGER_REVEAL, new TextureRegionDrawable(AtlasRegions.finger_hint_big));
        rewardTypeToIconMappingBig.put(MULTI_RANDOM_REVEAL, new TextureRegionDrawable(AtlasRegions.multi_random_hint_big));
        rewardTypeToIconMappingBig.put(ROCKET_REVEAL, new TextureRegionDrawable(AtlasRegions.rocket_big));


        background = new Group();

        Image wheel = new Image(AtlasRegions.lucky_wheel);
        wheel.setOrigin(Align.center);
        background.addActor(wheel);
        background.setSize(wheel.getWidth(), wheel.getHeight());
        background.setOrigin(Align.center);
        addActor(background);

        setSize(background.getWidth(), background.getHeight());
        setOrigin(Align.center);

        leds = new Image[TOTAL_SLICES];

        Drawable ledDrawable = new TextureRegionDrawable(AtlasRegions.discParticle);
        createSlices(ledDrawable);

        setupArrow();
    }




    private void setupArrow(){
        arrow = new Image(AtlasRegions.wheel_arrow);
        arrow.setOrigin(arrow.getWidth() * 0.5f, arrow.getHeight() * 0.704980f);
        arrow.setX((getWidth() - arrow.getWidth()) * 0.5f);
        arrow.setY(getHeight() - arrow.getHeight() * 0.35f);
        addActor(arrow);
    }




    private void createSlices(Drawable ledDrawable){
        randomWeights = new Array<>();
        sectorAngles = new Array<>();

        for(int i = 0; i < sliceConfig.length; i++){
            populateSlice(i, sliceConfig[i], ledDrawable);

            for(int j = 0; j < sliceConfig[i].probability; j++){
                randomWeights.add(i);
            }
        }

    }





    private void populateSlice(int index, Slice slice, Drawable ledDrawable) {

        float angle = getAngleByIndex(index);
        sectorAngles.add(90 - (int)angle);

        BitmapFont sliceFont = resourceManager.get(ResourceManager.fontSemiBoldShadow, BitmapFont.class);
        Color sliceTextColor;
        if (sliceConfig == GameConfig.dailyGiftSlices) {
            sliceTextColor = new Color(0x2A2A2AFF);
        } else {
            sliceTextColor = index % 2 == 0 ? UIConfig.WHEEL_DIALOG_ITEM_QUANTITY_TEXT_COLOR_DARK : UIConfig.WHEEL_DIALOG_ITEM_QUANTITY_TEXT_COLOR_LIGHT;
        }
        Label.LabelStyle shadowStyle = new Label.LabelStyle(sliceFont, new Color(0x000000E6));
        Label.LabelStyle labelStyle = new Label.LabelStyle(sliceFont, sliceTextColor);

        Label shadow = new Label(slice.text, shadowStyle);
        Label label = new Label(slice.text, labelStyle);
        float shadowOx = 2.4f;
        float shadowOy = -2.4f;
        shadow.setPosition(shadowOx, shadowOy);
        label.setPosition(0f, 0f);

        Group group = new Group();
        group.addActor(shadow);
        group.addActor(label);
        group.setSize(label.getWidth() + Math.abs(shadowOx), label.getHeight() + Math.abs(shadowOy));
        group.setOrigin(Align.center);
        group.setTransform(true);
        group.setRotation(angle - 90);

        float centerX = background.getWidth() * 0.5f;
        float centerY = background.getHeight() * 0.5f;

        float dx = centerX * MathUtils.cos(angle * MathUtils.degreesToRadians);
        float dy = centerY * MathUtils.sin(angle * MathUtils.degreesToRadians);

        float labelMargin = 0.7f;
        float x = centerX + dx * labelMargin - group.getWidth() * 0.5f;
        float y = centerY + dy * labelMargin - group.getHeight() * 0.5f;
        group.setPosition(x, y);
        background.addActor(group);

        ///////icon  (PASS diliminde ikon yok)
        Drawable iconDrawable = rewardTypeToIconMappingSmall.get(slice.reward);
        if(iconDrawable != null) {
            Image icon = new Image(iconDrawable);
            icon.setOrigin(Align.center);

            float iconMargin = 0.45f;

            x = centerX + dx * iconMargin - icon.getWidth() * 0.5f;
            y = centerY + dy * iconMargin - icon.getHeight() * 0.5f;

            icon.setPosition(x, y);
            icon.setRotation(angle);
            background.addActor(icon);
        }

        ///////Led

        Image led = new Image(ledDrawable);
        led.setColor(UIConfig.WHEEL_DIALOG_LIGHT_BULB_COLOR);

        angle -= 22.5;

        dx = centerX * MathUtils.cos(angle * MathUtils.degreesToRadians);
        dy = centerY * MathUtils.sin(angle * MathUtils.degreesToRadians);

        float ledMargin = 0.935f;
        x = centerX + dx * ledMargin - led.getWidth() * 0.5f;
        y = centerY + dy * ledMargin - led.getHeight() * 0.5f;
        led.setPosition(x, y);
        background.addActor(led);

        led.setVisible(index % 2 == 0);
        leds[index] = led;
    }




    private float getAngleByIndex(int index){
        return (float)index / TOTAL_SLICES * 360.0f;
    }



    public void spin(){
        spin1();
    }



    private void spin1() {
        Action rotateby = Actions.rotateBy(-25, 0.3f);

        RunnableAction runnableAction = new RunnableAction();
        runnableAction.setRunnable(new Runnable() {
            @Override
            public void run() {
                spin2();
            }
        });

        background.addAction(new SequenceAction(rotateby, runnableAction));
    }




    private void spin2(){
        int fullCircles = 5;
        int rnd = 0;
        if(randomWeights.size > 0){
            rnd = randomWeights.get(MathUtils.random(0, randomWeights.size - 1));
        }
        int randomFinalAngle = sectorAngles.get(rnd);
        float _finalAngle = (fullCircles * 360 + randomFinalAngle);

        selectedReward = sliceConfig[rnd];

	    float duration = MathUtils.random(3.0f, 6.0f);
        Action rotate = Actions.rotateTo(_finalAngle, duration + 1, Interpolation.cubicOut);
        RunnableAction runnableAction = new RunnableAction();
        runnableAction.setRunnable(spinFinished);
        SequenceAction sequenceAction = new SequenceAction(rotate, runnableAction);
        background.addAction(sequenceAction);
    }



    private SequenceAction sequenceAction;
    private RotateToAction rotateToAction1, rotateToAction2;
    private float time;




    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);

        if(leds.length == TOTAL_SLICES){
            time += Gdx.graphics.getDeltaTime();

            if(time > 0.5f){
                time = 0;

                for(int i = 0; i < leds.length; i++){
                    leds[i].setVisible(!leds[i].isVisible());
                }
            }
        }

        int sliceAngle = Math.round(360f / TOTAL_SLICES);

        if(arrow.getRotation() > -2.5f && Math.abs(background.getRotation() - sliceAngle * 2.5f) % sliceAngle <= 5){
            arrow.clearActions();
            if(rotateToAction1 == null) rotateToAction1 = new RotateToAction();
            else rotateToAction1.reset();
            rotateToAction1.setRotation(-30);
            rotateToAction1.setDuration(0.1f);

            if(rotateToAction2 == null) rotateToAction2 = new RotateToAction();
            else rotateToAction2.reset();
            rotateToAction2.setRotation(0);
            rotateToAction2.setDuration(0.05f);

            if(sequenceAction == null) sequenceAction = new SequenceAction();
            else sequenceAction.reset();
            sequenceAction.addAction(rotateToAction1);
            sequenceAction.addAction(rotateToAction2);
            arrow.addAction(sequenceAction);

            if(!ConfigProcessor.muted) {
                Sound click = resourceManager.get(ResourceManager.SFX_SPIN_CLICK, Sound.class);
                click.play(SoundConfig.SFX_WHEEL_SPIN_CLICK_VOLUME);
            }
        }
    }
}
