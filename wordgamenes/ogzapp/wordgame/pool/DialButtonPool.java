package ogzapp.wordgame.pool;

import com.badlogic.gdx.utils.Pool;

import ogzapp.wordgame.model.Constants;
import ogzapp.wordgame.ui.dial.DialButton;

public class DialButtonPool extends Pool<DialButton> {

    public DialButtonPool(){
            super(5, Constants.MAX_LETTERS);
    }


    @Override
    protected DialButton newObject() {
        return new DialButton();
    }
}
