package ogzapp.wordgame.pool;

import com.badlogic.gdx.utils.Pool;

import ogzapp.wordgame.model.Constants;
import ogzapp.wordgame.ui.preview.Letter;

public class LetterPool extends Pool<Letter>{


    public LetterPool(){
        super(Constants.MAX_LETTERS, Constants.MAX_LETTERS * 4);
    }


    @Override
    protected Letter newObject() {
        return new Letter();
    }
}
