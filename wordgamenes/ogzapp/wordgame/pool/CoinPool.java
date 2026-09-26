package ogzapp.wordgame.pool;

import com.badlogic.gdx.utils.Pool;

import ogzapp.wordgame.ui.top_panel.Coin;

public class CoinPool extends Pool<Coin> {


    @Override
    protected Coin newObject() {
        return new Coin();
    }
}
