package ogzapp.wordgame.pool;

import com.badlogic.gdx.utils.Pool;

import ogzapp.wordgame.ui.board.AnimationLabel;

public class AnimationLabelPool extends Pool<AnimationLabel> {


    public AnimationLabelPool(){
        super(6, 50);
    }



    @Override
    protected AnimationLabel newObject() {
        return new AnimationLabel();
    }


}
