package ogzapp.wordgame.ui.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.model.CellModel;
import ogzapp.wordgame.model.Constants;
import ogzapp.wordgame.pool.Pools;
import ogzapp.wordgame.ui.hint.HintComet;


public class CellView extends Group implements Pool.Poolable {

    public static Label.LabelStyle labelStyleSolved;
    public static Label.LabelStyle labelStyleRevealed;

    public static float solvedTileSizeCoef = 0.85f;

    public CellModel cellData;

    private Image unsolvedImg;
    private Image solvedImg;

    public Label label;
    private Actor coin;
    public Ufo ufo;
    public Bomb bomb;
    public GoldPack goldPack;
    public boolean hasMonster;
    public InputListener inputListener;

    private Array<CellViewParticle> sparkles = new Array<>();

    private Color solvedColor;
    private ResourceManager resourceManager;
    private BoardView boardView;
    private Vector2 temp = new Vector2();
    public HintComet hintComet;
    private Color unsolvedBgColor;

    private NinePatchDrawable unsolvedBgDrawable, solvedBgDrawable;


    public CellView(){
        if(unsolvedBgDrawable == null) unsolvedBgDrawable = new NinePatchDrawable(NinePatches.board_cell);
        // board_cell_solved atlası mavi/bordo pişmiş renk taşıdığı için
        // gold gibi bir tema rengi çarpılınca kutu içinde ikinci bir hue
        // görünüyordu. Aynı nötr board_cell dokusu boyanınca tek renk kalır.
        if(solvedBgDrawable == null) solvedBgDrawable = new NinePatchDrawable(NinePatches.board_cell);
        createUnsolvedBg();
    }


    public void construct(float width, CellModel cellData, Color solvedColor, Color unsolvedColor, ResourceManager resourceManager, BoardView boardView){
        setSize(width, width);
        setOrigin(width * 0.5f, width * 0.5f);

        this.cellData = cellData;
        // Paylaşılan Color.ROYAL / seviye rengi referansı tutulmaz; alfa
        // animasyonu (fadeInSolvedBg) paylaşılan rengi 0'a çekip oyundan
        // çıkıp girince mavi kutuları şeffaf bırakıyordu.
        this.solvedColor = solvedColor == null ? new Color(Color.ROYAL) : new Color(solvedColor);
        this.solvedColor.a = 1f;
        this.unsolvedBgColor = unsolvedColor == null ? new Color(1f, 1f, 1f, 0.7f) : new Color(unsolvedColor);
        this.resourceManager = resourceManager;
        this.boardView = boardView;
        clearActions();
        super.setColor(Color.WHITE);
        getColor().a = 1f;
        updateStateView();
        ensureSolvedBgOpaque();
    }




    @Override
    public void reset() {

        if(bomb != null){
            bomb.remove();
            bomb = null;
        }
        if(goldPack != null){
            goldPack.remove();
            goldPack = null;
        }
        if(ufo != null){
            ufo.remove();
            ufo = null;
        }

        removeCoin();

        hasMonster = false;
        if(unsolvedImg != null) {
            unsolvedImg.clearActions();
            unsolvedImg.setVisible(false);
        }
        if(solvedImg != null) {
            solvedImg.clearActions();
            solvedImg.getColor().a = 1f;
            solvedImg.setVisible(false);
        }
        if(label != null) {
            label.clearActions();
            label.setVisible(false);
        }
        sparkles.clear();

        if(unsolvedBgAnimColor != null) {
            com.badlogic.gdx.utils.Pools.free(unsolvedBgAnimColor);
            unsolvedBgAnimColor = null;
        }

        clearActions();
        super.setColor(Color.WHITE);
        getColor().a = 1f;
        setRotation(0);
        setScale(1f);

    }




    public void removeCoin(){
        if(coin != null){
            coin.remove();
            coin = null;
        }
    }



    @Override
    public boolean equals(Object o) {
        if(o instanceof CellView){
            CellView that = (CellView)o;
            return this.cellData.getX() == that.cellData.getX() && this.cellData.getY() == that.cellData.getY();
        }
        return false;
    }





    @Override
    public int hashCode() {
        return (cellData.getX() << 8) | cellData.getY();
    }



    public void updateStateView(){
        switch (cellData.getState()){
            case Constants.TILE_STATE_DEFAULT:
                createUnsolvedBg();
                break;
            case Constants.TILE_STATE_SOLVED:
                createUnsolvedBg();
                createSolvedBg();
                createLabel();
                label.setStyle(labelStyleSolved);
                break;
            case Constants.TILE_STATE_REVEALED:
                createUnsolvedBg();
                createLabel();
                Label.LabelStyle style = labelStyleRevealed;
                label.setStyle(style);
                break;
            case Constants.TILE_STATE_COINED:
                createUnsolvedBg();
                revealRocketCoin();
                break;
            case Constants.TILE_STATE_BOMBED:
                createUnsolvedBg();
                setBomb();
                break;
            case Constants.TILE_STATE_GOLD_PACKED:
                createUnsolvedBg();
                setGoldPack();
                break;
            case Constants.TILE_STATE_MONSTER:
                createUnsolvedBg();
                setMonster();
        }
    }




    private void createUnsolvedBg(){
        if(unsolvedImg == null){
            unsolvedImg = new Image(unsolvedBgDrawable);
            addActor(unsolvedImg);
        }else{
            unsolvedImg.setVisible(true);
        }

        if(boardView != null) unsolvedImg.setColor(unsolvedBgColor);

        unsolvedImg.setSize(getWidth(), getHeight());
        unsolvedImg.setOrigin(Align.center);
    }




    private void createSolvedBg() {
        if(solvedImg == null){
            solvedImg = new Image(solvedBgDrawable);
            addActor(solvedImg);
            solvedImg.setZIndex(1);

        }else{
            solvedImg.setVisible(true);
        }
        float size = getWidth() * solvedTileSizeCoef;
        solvedImg.setSize(size, size);
        solvedImg.setX((getWidth() - solvedImg.getWidth()) * 0.5f);
        solvedImg.setY((getHeight() - solvedImg.getHeight()) * 0.5f);
        solvedImg.clearActions();
        Color tint = solvedColor == null ? new Color(Color.ROYAL) : new Color(solvedColor);
        tint.a = 1f;
        solvedImg.setColor(tint);
        solvedImg.setOrigin(Align.center);
        solvedImg.getColor().a = 1f;
        applyWhiteFrame();
    }



    public static float fontScale;


    public void createLabel(){
        if(label == null) {
            label = new Label(String.valueOf(cellData.getLetter()), labelStyleSolved);
            addActor(label);
        }else{
            label.setText(String.valueOf(cellData.getLetter()));
            label.setVisible(true);
        }

        label.setOrigin(Align.center);
        GlyphLayout layout = com.badlogic.gdx.utils.Pools.obtain(GlyphLayout.class);
        layout.setText(label.getStyle().font, label.getText());
        fontScale = getHeight() * solvedTileSizeCoef * UIConfig.TILE_LETTER_FONT_SCALE / layout.height;
        label.setFontScale(fontScale);
        label.setX((getWidth() - layout.width * label.getFontScaleX()) * 0.5f);
        label.setY((getHeight() - label.getHeight()) * 0.5f);
        com.badlogic.gdx.utils.Pools.free(layout);
    }

    private void applyWhiteFrame() {
        if (unsolvedImg == null) return;
        unsolvedImg.setVisible(true);
        unsolvedImg.setSize(getWidth(), getHeight());
        Color frame = unsolvedBgColor == null ? new Color(1f, 1f, 1f, 1f) : new Color(unsolvedBgColor);
        frame.a = 1f;
        unsolvedImg.setColor(frame);
        unsolvedImg.toBack();
    }








    public void revealRocketCoin(){
        if(coin == null){
            coin = new CoinRotateAnim();
            coin.setScale(getWidth() * 0.75f / coin.getWidth());
            coin.setX((getWidth() - coin.getWidth() * coin.getScaleX()) * 0.5f);
            coin.setY((getHeight() - coin.getHeight() * coin.getScaleY()) * 0.5f);
            addActor(coin);
        }
    }




    public void setCoin(Actor coin){
        this.coin = coin;
    }




    public void setBomb(){

        if(bomb == null){
            bomb = new Bomb(boardView.gameScreen);
            float scale = getHeight() * 0.75f / bomb.getHeight();
            bomb.setScale(scale);
            bomb.setSmallScale(scale);
            bomb.setLargeScale(getHeight() * 0.9f / bomb.getHeight());

            bomb.setX((getWidth() - bomb.getWidth()) * 0.5f);
            bomb.setY((getHeight() - bomb.getHeight()) * 0.5f);
            addActor(bomb);
            boardView.bomb = bomb;
            bomb.targetCell = this;
        }
    }




    public void setGoldPack(){
        if(goldPack == null){
            goldPack = new GoldPack(resourceManager);
            float scale = getWidth() * 0.75f / goldPack.getWidth();
            goldPack.setScale(scale);
            goldPack.setX((getWidth() - goldPack.getWidth() * goldPack.getScaleX()) * 0.5f);
            goldPack.setY((getHeight() - goldPack.getHeight() * goldPack.getScaleY()) * 0.5f);
            addActor(goldPack);
            boardView.goldPack = goldPack;
        }
    }






    private void setMonster(){
        if(boardView.monster == null){
            boardView.monster = new Monster();
            boardView.monster.setScale(getWidth() / boardView.monster.getWidth());
            boardView.monster.setX(getX() + (getWidth() * 0.5f - boardView.monster.getWidth() * boardView.monster.getScaleX() * 0.407051f));
            boardView.monster.setY(getY() + getHeight() * 0.1f);
            boardView.addActor(boardView.monster);
            hasMonster = true;
            boardView.monster.targetCell = this;
        }
    }



    public Actor getCoin(){
        return coin;
    }



    public void growAndShrink(final Runnable callback){

        getGsSequence();
        getGsScale1(1.2f, 1.2f, 0.15f, Interpolation.slowFast);
        getGsScale2(1f, 1f, 0.3f, Interpolation.fastSlow);

        gsSequence.addAction(gsScale1);
        gsSequence.addAction(gsScale2);

        if(callback != null){
            getGsRunnable(callback);
            gsSequence.addAction(gsRunnable);
        }

        addAction(gsSequence);
    }



    private DelayAction gsDelay1, gsDelay2;
    private ScaleToAction gsScale1, gsScale2;
    private SequenceAction gsSequence;
    private RunnableAction gsRunnable;




    public DelayAction getGsDelay1(float duration){
        if(gsDelay1 == null) gsDelay1 = new DelayAction();
        else gsDelay1.reset();
        gsDelay1.setDuration(duration);
        return gsDelay1;
    }


    public DelayAction getGsDelay2(float duration){
        if(gsDelay2 == null) gsDelay2 = new DelayAction();
        else gsDelay2.reset();
        gsDelay2.setDuration(duration);
        return gsDelay2;
    }


    public ScaleToAction getGsScale1(float x, float y, float duration, Interpolation interpolation){
        if(gsScale1 == null) gsScale1 = new ScaleToAction();
        else gsScale1.reset();
        gsScale1.setScale(x, y);
        gsScale1.setDuration(duration);
        if(interpolation != null) gsScale1.setInterpolation(interpolation);
        return gsScale1;
    }


    public ScaleToAction getGsScale2(float x, float y, float duration, Interpolation interpolation){
        if(gsScale2 == null) gsScale2 = new ScaleToAction();
        else gsScale2.reset();
        gsScale2.setScale(x, y);
        gsScale2.setDuration(duration);
        if(interpolation != null) gsScale2.setInterpolation(interpolation);
        return gsScale2;
    }


    public void getGsSequence(){
        if(gsSequence == null) gsSequence = new SequenceAction();
        else gsSequence.reset();
    }


    public RunnableAction getGsRunnable(Runnable callback){
        if(gsRunnable == null) gsRunnable = new RunnableAction();
        else gsRunnable.reset();
        gsRunnable.setRunnable(callback);
        return gsRunnable;
    }







    private AlphaAction convertAlphaAction;



    public void convertUnsolvedBgToSolved(){
        solvedImg.getColor().a = 0;

        if(convertAlphaAction == null) convertAlphaAction = new AlphaAction();
        else convertAlphaAction.reset();
        convertAlphaAction.setAlpha(1f);
        convertAlphaAction.setDuration(0.2f);
        solvedImg.addAction(convertAlphaAction);

    }







    public void ensureSolvedBgOpaque() {
        if (cellData == null || cellData.getState() != Constants.TILE_STATE_SOLVED) return;
        if (solvedImg == null || !solvedImg.isVisible()) {
            updateStateView();
        }
        if (solvedImg != null) {
            solvedImg.clearActions();
            Color tint = solvedColor == null ? new Color(Color.ROYAL) : new Color(solvedColor);
            tint.a = 1f;
            solvedImg.setColor(tint);
            solvedImg.setVisible(true);
            solvedImg.getColor().a = 1f;
            applyWhiteFrame();
        }
        if (label != null) {
            label.setVisible(true);
            if (labelStyleSolved != null) label.setStyle(labelStyleSolved);
        }
    }

    @Override
    public void setColor(Color color) {
        super.setColor(color);
    }




    public void starBurst(){

        for(int i = 0; i < UIConfig.TILE_SUCCESS_SPARKLE_PARTICLE_COUNT; i++){
            CellViewParticle p = Pools.cellViewParticlePool.obtain();

            float angle = MathUtils.random() * MathUtils.PI2;
            float radius = getWidth() * (MathUtils.random() * 1.5f);

            p.setColor(UIConfig.TILE_SUCCESS_SPARKLE_PARTICLE_COLOR);
            p.x = getX() + getWidth() * 0.5f - p.getWidth() * 0.5f + radius * MathUtils.cos(angle);
            p.y = getY() + getHeight() * 0.5f - p.getHeight() * 0.5f + radius * MathUtils.sin(angle);
            p.radius = MathUtils.random(0.7f, 1.0f);
            p.rotation = angle;
            p.speed = MathUtils.random(2f, 4f);
            p.friction = 0.97f;
            p.opacity = MathUtils.random(0.7f, 1f);
            sparkles.add(p);
        }
    }





    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);

        for(int i = 0; i < sparkles.size; i++){

            CellViewParticle p = sparkles.get(i);

            p.x += p.speed * MathUtils.cos(p.rotation);
            p.y += p.speed * MathUtils.sin(p.rotation);

            p.speed *= p.friction;
            p.radius *= 0.98f;

            p.setPosition(p.x, p.y);
            p.setScale(p.radius, p.radius);
            p.setRotation(p.getRotation());

            if (p.getScaleX() < 0.3f){
                p.opacity -= 0.01f;
                p.setAlpha(p.opacity);
            }
            p.draw(batch);

            if (p.getColor().a <= 0 || p.getScaleX() <= 0) {
                sparkles.removeIndex(i);
                Pools.cellViewParticlePool.free(p);
            }
        }
    }



    public void setUnsolvedBgColor(Color color) {
        if(unsolvedImg != null) {
            unsolvedImg.setColor(color);
        }
    }






    private Color unsolvedBgAnimColor;

    public void animateUnsolvedBgColor(){
        if(unsolvedImg != null){
            unsolvedBgAnimColor = com.badlogic.gdx.utils.Pools.obtain(Color.class);
            unsolvedBgAnimColor.set(unsolvedBgColor.r, unsolvedBgColor.g, unsolvedBgColor.b, unsolvedBgColor.a);
            unsolvedImg.addAction(Actions.color(unsolvedBgAnimColor, 1.2f));
        }
    }





    public Vector2 getStageCoords(){
        temp.x = 0;
        temp.y = 0;
        return localToStageCoordinates(temp);
    }



    private AlphaAction solvedBgAlphaAction;
    private DelayAction solvedBgDelayAction;
    private SequenceAction solvedBgSequenceAction;

    public void fadeInSolvedBg(float delay){
        if(solvedImg != null && solvedImg.isVisible() && solvedImg.getColor().a == 1f) return;
        updateStateView();
        solvedImg.getColor().a = 0f;

        if(solvedBgAlphaAction == null) solvedBgAlphaAction = new AlphaAction();
        else solvedBgAlphaAction.reset();

        solvedBgAlphaAction.setAlpha(1f);
        solvedBgAlphaAction.setDuration(0.25f);

        if(solvedBgDelayAction == null) solvedBgDelayAction = new DelayAction();
        else solvedBgDelayAction.reset();
        solvedBgDelayAction.setDuration(delay);

        if(solvedBgSequenceAction == null) solvedBgSequenceAction = new SequenceAction();
        else solvedBgSequenceAction.reset();

        solvedBgSequenceAction.addAction(solvedBgDelayAction);
        solvedBgSequenceAction.addAction(solvedBgAlphaAction);
        solvedImg.addAction(solvedBgSequenceAction);
    }



    @Override
    public String toString() {
        return "["+cellData.getLetter()+":"+cellData.getState()+"]";
    }



}
