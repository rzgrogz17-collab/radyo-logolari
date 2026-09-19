package ogzapp.wordgame.pool;

import com.badlogic.gdx.utils.Pool;

import ogzapp.wordgame.model.Word;

public class WordPool extends Pool<Word> {

    public WordPool(){
        super(5, 15);
    }

    @Override
    protected Word newObject() {
        return new Word();
    }
}
