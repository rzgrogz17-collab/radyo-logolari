package ogzapp.wordgame.pool;

import com.badlogic.gdx.utils.Pool;

import ogzapp.wordgame.ui.board.CellViewParticle;

public class CellViewParticlePool extends Pool<CellViewParticle> {


    public CellViewParticlePool(){
        super(24, 64);
    }



    @Override
    protected CellViewParticle newObject() {
        return new CellViewParticle();
    }
}
