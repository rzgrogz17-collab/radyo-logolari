package ogzapp.wordgame.pool;

import com.badlogic.gdx.utils.Pool;

import ogzapp.wordgame.model.CellModel;

public class CellModelPool extends Pool<CellModel> {

    public CellModelPool(){
        super(9, 50);
    }


    @Override
    protected CellModel newObject() {
        return new CellModel();
    }
}
