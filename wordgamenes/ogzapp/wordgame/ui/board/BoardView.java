package ogzapp.wordgame.ui.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.utils.Array;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.model.BoardModel;
import ogzapp.wordgame.model.CellModel;
import ogzapp.wordgame.model.Constants;
import ogzapp.wordgame.model.Direction;
import ogzapp.wordgame.model.Word;
import ogzapp.wordgame.model.GameData;
import ogzapp.wordgame.pool.Pools;
import ogzapp.wordgame.screens.GameScreen;

public class BoardView extends Group {

    private BoardModel boardModel;
    public GameScreen gameScreen;
    private float width, height;
    private Map<Integer, CellView> cellViews = new HashMap<>();
    private Array<CellView> cellsToAnimate = new Array<>();

    private Random random = new Random();
    private Array<CellView> cellsToSelectRandomHint = new Array<>();

    private boolean fingerHintSelectionModeActive;
    private AlreadySolvedIndicator alreadySolvedIndicator;
    private Color solvedTileColor;
    private Color unsolvedTileColor;
    public Ufo ufo;
    public Bomb bomb;
    public GoldPack goldPack;
    public Monster monster;



    public void init(BoardModel boardModel, GameScreen gameScreen, float width, float height){
        this.boardModel = boardModel;
        this.gameScreen = gameScreen;
        this.width = width;
        this.height = height;
        layout();
    }






    public void setThemeColor(Color color) {
        solvedTileColor = color;
        super.setColor(Color.WHITE);
    }



    public void setTileBackgroundColor(Color color){
        unsolvedTileColor = color;
    }



    private void layout(){
        // KÖK NEDEN DÜZELTMESİ ("ekranda fazladan/hayalet kelime kutuları
        // açılıyor" hatası): Bu metot ÖNCEDEN sadece "cellViews.clear()"
        // yapıyordu. Bu satır SADECE pozisyon->CellView eşlemesini tutan
        // Java Map'ini temizliyordu; o an ekranda (bu BoardView grubunun
        // çocuğu olarak) duran ESKİ CellView AKTÖRLERİNİ sahneden HİÇ
        // KALDIRMIYORDU. layout() (dolayısıyla init()) tek bir seviye
        // boyunca BİRDEN FAZLA kez çağrılırsa - ör. GameScreen.resize()
        // içinden, bir banner reklam gösterilip kaybolunca tahtayı yeni
        // ekran boyutuna göre yeniden konumlamak için - her çağrıda YENİ
        // bir kutu seti ESKİLERİNİN ÜSTÜNE/YANINA ekleniyordu; eskiler asla
        // kaldırılmadığı için ekranda kalıcı, boş görünen "hayalet"
        // fazladan kutular birikiyordu.
        //
        // Düzeltme: yeni kutuları oluşturmadan ÖNCE, varsa önceki
        // çağrıdan kalan CellView aktörleri sahneden kaldırılıp havuza
        // iade ediliyor. cellView.cellData BİLEREK sıfırlanmıyor/kendi
        // havuzuna DÖNDÜRÜLMÜYOR - o veri boardModel'e, yani oyuncunun O
        // ANKİ gerçek ilerlemesine (hangi hücre çözülmüş/açılmış) ait;
        // burada sadece GÖRSEL aktörü temizliyoruz. CellView.construct()
        // zaten updateStateView() ile cellData'nın GÜNCEL durumunu okuyup
        // doğru görünümü yeniden kurduğu için ilerleme kaybolmuyor.
        for (CellView oldCellView : cellViews.values()) {
            oldCellView.remove();
            oldCellView.reset();
            Pools.cellViewPool.free(oldCellView);
        }
        cellViews.clear();

        CellModel[][] solvedState = boardModel.getSolvedState();

        float cellWidth = computeCellSize();

        for(int x = 0; x < solvedState.length; x++){
            for(int y = 0; y < solvedState[x].length; y++){
                CellModel cellData = solvedState[x][y];

                if(cellData != null){
                    CellView cellView = Pools.cellViewPool.obtain();
                    cellView.construct(cellWidth, cellData, solvedTileColor, unsolvedTileColor, gameScreen.wordConnectGame.resourceManager, this);
                    cellView.setX(x * cellWidth);
                    cellView.setY(y * cellWidth);
                    cellViews.put(mergeInts(cellData.getX(),cellData.getY()), cellView);
                    setTouchListener(cellView);
                    addActor(cellView);
                }
            }
        }

        restoreSolvedAppearance();

        setWidth(cellWidth * boardModel.width);
        setHeight(cellWidth* boardModel.height);
    }

    // Çıkış/giriş sonrası çözülmüş kutuların mavi rengi ve harfleri
    // oyun içindeki anlık görünümle aynı kalır.
    private void restoreSolvedAppearance() {
        super.setColor(Color.WHITE);
        getColor().a = 1f;
        clearActions();
        for (CellView cellView : cellViews.values()) {
            if (cellView == null || cellView.cellData == null) continue;
            Word across = cellView.cellData.acrossWord;
            Word down = cellView.cellData.downWord;
            boolean wordSolved = (across != null && across.isSolved) || (down != null && down.isSolved);
            int state = cellView.cellData.getState();
            if (wordSolved && (state == Constants.TILE_STATE_DEFAULT
                    || state == Constants.TILE_STATE_REVEALED
                    || state == Constants.TILE_STATE_SOLVED)) {
                if (state != Constants.TILE_STATE_SOLVED) {
                    cellView.cellData.setState(Constants.TILE_STATE_SOLVED);
                    GameData.saveTileState(
                            cellView.cellData.getX(), cellView.cellData.getY(), Constants.TILE_STATE_SOLVED);
                }
                cellView.updateStateView();
                cellView.ensureSolvedBgOpaque();
            } else if (state == Constants.TILE_STATE_SOLVED) {
                cellView.ensureSolvedBgOpaque();
            }
        }
    }






    private float computeCellSize(){

        int rows = boardModel.height;
        int columns = boardModel.width;

        float boxWidth = (width / columns);
        float boxHeight = (height / rows);

        float boxSize;

        if (boxWidth >= boxHeight) {
            boxSize = height / rows;
        } else {
            boxSize = width / columns;
        }

        return Math.min(boxSize, width * 0.15f);
    }





    public CellView getCellViewByXY(int x, int y){
        int key = mergeInts(x, y);
        return cellViews.get(key);
    }






    private int mergeInts(int a, int b)
    {
        return (a << 8) | b;
    }




    InputListener cellViewInputListener = new InputListener(){

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            return true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {

            Actor eventTarget = event.getTarget();
            final CellView cellView = (CellView)eventTarget.getParent();

            if(fingerHintSelectionModeActive){

                int state = cellView.cellData.getState();

                if(state == Constants.TILE_STATE_SOLVED || state == Constants.TILE_STATE_REVEALED) {
                    indicateLetterWasSolvedBefore(cellView);
                    return;
                }

                if(state != Constants.TILE_STATE_DEFAULT)
                    return;

                cellsToAnimate.clear();
                cellsToAnimate.add(cellView);
                fingerHintSelectionModeActive = false;

                UIConfig.FINGER_SELECTED_TILE_COLOR.a = UIConfig.getTileBackgroundUnsolvedColorByLevelIndex(gameScreen.gameController.level.index).a;

                cellView.setUnsolvedBgColor(UIConfig.FINGER_SELECTED_TILE_COLOR);

                Runnable callback = new Runnable() {
                    @Override
                    public void run() {
                        getStage().getRoot().setTouchable(Touchable.disabled);
                        gameScreen.animateHint(cellsToAnimate, cellView);
                    }
                };
                gameScreen.closeBoardOverlayAndAnimateHint(callback);

            }else{
                if(LanguageManager.wordMeaningProviderMap.containsKey(LanguageManager.locale.code)) showMeaning(cellView.cellData);
            }
        }
    };





    private void setTouchListener(CellView cellView){
        // KÖK NEDEN DÜZELTMESİ: "cellView.inputListener" alanı hiçbir yerde
        // ATANMIYORDU, bu yüzden bu koşul HER ZAMAN true dönüyor ve
        // "cellViewInputListener" (BoardView'da TEK bir paylaşılan
        // dinleyici nesnesi) her çağrıda tekrar tekrar EKLENİYORDU. Havuzdan
        // (Pools.cellViewPool) yeniden kullanılan bir CellView, önceki
        // kullanımından kalma dinleyicileri hâlâ üzerinde taşıdığı için
        // (reset() dinleyicileri temizlemiyor), bu durum aynı dokunuşun
        // birden fazla kez işlenmesine (ör. ipucu animasyonunun/kelime
        // anlamı gösteriminin birkaç kez tetiklenmesi) yol açabiliyordu.
        if(cellView.inputListener == null) {
            cellView.inputListener = cellViewInputListener;
            cellView.addListener(cellViewInputListener);
        }
    }






    public CellView revealSingleRandomHint(Actor button){
        CellView randomCell = getSingleRandomCell(null);
        if(randomCell != null){
            cellsToAnimate.clear();
            cellsToAnimate.add(randomCell);
            gameScreen.animateHint(cellsToAnimate, button);
        }
        return randomCell;
    }





    public CellView getSingleRandomCell(CellView identity){
        Array<CellView> cells = cellViewsToArray();

        if(cells.size > 0) {
            if(identity == null){
                return cells.random();
            }else{//for monster
                cells.shuffle();

                for(CellView cellView : cells){
                    if(!cellView.equals(identity)){
                        return cellView;
                    }
                }
            }

        }
        return null;
    }






    public int revealMultiRandomHint(Actor button, int levelIndex){
        selectMultipleCellsForHint(GameConfig.getNumberOfTilesToRevealForMultiRandomHint(levelIndex));
        if(cellsToAnimate.size > 0){
            gameScreen.animateHint(cellsToAnimate, button);
        }
        return cellsToAnimate.size;
    }






    public Array<CellView> selectMultipleCellsForHint(int num){
        cellsToAnimate.clear();
        Array<CellView> cells = cellViewsToArray();

        if(cells.size > 0){
            cells.shuffle();
            int count = Math.min(num, cells.size);
            for(int i = 0; i < count; i++){
                CellView randomCell = cells.get(i);
                cellsToAnimate.add(randomCell);
            }
        }
        return cellsToAnimate;
    }






    private Array<CellView> cellViewsToArray(){
        cellsToSelectRandomHint.clear();

        for(int key : cellViews.keySet()){
            CellView cellView = cellViews.get(key);
            if(cellView.cellData.getState() == Constants.TILE_STATE_DEFAULT)
                cellsToSelectRandomHint.add(cellView);
        }
        return cellsToSelectRandomHint;
    }





    public void setFingerHintSelectionModeActive(boolean flag){
        fingerHintSelectionModeActive = flag;
    }




    private void indicateLetterWasSolvedBefore(CellView cellView){
        if(alreadySolvedIndicator == null)
            alreadySolvedIndicator = new AlreadySolvedIndicator();

        alreadySolvedIndicator.reset();

        //cellView.clearActions();
        cellView.setRotation(0);

        cellView.addAction(alreadySolvedIndicator);
    }





    public boolean isBoardSolved(boolean completely){
        for(Integer key : cellViews.keySet()){
            CellView cellView = cellViews.get(key);
            int state = cellView.cellData.getState();

            if(completely){
                if(state != Constants.TILE_STATE_SOLVED) return false;
            }else{
                if(state != Constants.TILE_STATE_SOLVED && state != Constants.TILE_STATE_REVEALED) return false;
            }


        }
        return true;
    }




    public Array<CellView> getRevealedCells(){
        cellsToAnimate.clear();

        for(Integer key : cellViews.keySet()) {
            CellView cellView = cellViews.get(key);
            int state = cellView.cellData.getState();
            if(state == Constants.TILE_STATE_REVEALED){
                cellsToAnimate.add(cellView);
            }
        }
        return cellsToAnimate;
    }







    public void convertRevealedCellToSolved(){
        for(int i = 0; i < cellsToAnimate.size; i++){
            CellView cellView = cellsToAnimate.get(i);

            cellView.cellData.setState(Constants.TILE_STATE_SOLVED);
            GameData.saveTileState(
                    cellView.cellData.getX(), cellView.cellData.getY(), Constants.TILE_STATE_SOLVED);
            cellView.updateStateView();
            cellView.convertUnsolvedBgToSolved();
        }
    }




    private void showMeaning(CellModel cellModel){
        Word downWord = cellModel.downWord;

        if(downWord != null && downWord.isSolved){
            gameScreen.showDictionary(new String[]{downWord.answer});
            return;
        }

        Word acrossWord = cellModel.acrossWord;

        if(acrossWord != null && acrossWord.isSolved){
            gameScreen.showDictionary(new String[]{acrossWord.answer});
        }

    }




    static class AlreadySolvedIndicator extends SequenceAction {

        private Action a = Actions.rotateBy(-10, 0.1f);
        private Action b = Actions.rotateBy(20, 0.2f);
        private Action c = Actions.rotateBy(-20, 0.2f);
        private Action d = Actions.rotateBy(20, 0.2f);
        private Action e = Actions.rotateBy(-10, 0.1f);


        @Override
        public void reset() {
            super.reset();
            a.reset();
            b.reset();
            c.reset();
            d.reset();
            e.reset();

            addAction(a);
            addAction(b);
            addAction(c);
            addAction(d);
            addAction(e);
        }
    }






    public Array<CellView> findWordCellViews(Word word){

        Array<CellView> cells = new Array<>();

        for(int i = 0; i < word.answer.length(); i++){
            int x = 0;
            int y = 0;

            if(word.direction == Direction.ACROSS){
                y = word.y;
                x = word.x + i;
            }

            if(word.direction == Direction.DOWN){
                x = word.x;
                y = word.y - i;
            }

            CellView cellView = getCellViewByXY(x, y);
            cells.add(cellView);
        }
        return cells;
    }





    public void clearContent(){
        freeCellViews();
        freeWords();
        if(ufo != null) {
            ufo.remove();
            ufo = null;
        }

        if(bomb != null){
            bomb.remove();
            bomb = null;
        }

        if(goldPack != null) {
            goldPack.remove();
            goldPack = null;
        }


        if(monster != null) {
            monster.remove();
            monster = null;
        }

        fingerHintSelectionModeActive = false;
    }




    public void freeCellViews(){
        for(Integer key : cellViews.keySet()) {
            CellView cellView = cellViews.get(key);
            cellView.remove();

            cellView.cellData.reset();
            cellView.reset();

            Pools.cellModelPool.free(cellView.cellData);
            Pools.cellViewPool.free(cellView);
        }
        cellViews.clear();
    }





    public void freeWords(){
        Array<Word> words = boardModel.getAllWords(true);

        for(Word word : words){
            word.reset();
            Pools.wordPool.free(word);
        }
    }


}
