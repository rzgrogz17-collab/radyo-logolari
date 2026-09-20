package ogzapp.wordgame.controllers;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;

import java.util.HashSet;
import java.util.Set;

import ogzapp.wordgame.actions.Interpolation;
import ogzapp.wordgame.config.ConfigProcessor;
import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.SoundConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.managers.HintManager;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.model.Constants;
import ogzapp.wordgame.model.Direction;
import ogzapp.wordgame.model.GameData;
import ogzapp.wordgame.model.Level;
import ogzapp.wordgame.model.Word;
import ogzapp.wordgame.pool.Pools;
import ogzapp.wordgame.screens.GameScreen;
import ogzapp.wordgame.ui.board.AnimationLabel;
import ogzapp.wordgame.ui.board.BoardView;
import ogzapp.wordgame.ui.board.CellView;
import ogzapp.wordgame.ui.dial.Dial;
import ogzapp.wordgame.ui.dialogs.DictionaryDialog;
import ogzapp.wordgame.ui.preview.Letter;
import ogzapp.wordgame.ui.preview.Preview;
import ogzapp.wordgame.ui.top_panel.CoinView;
import ogzapp.wordgame.ui.tutorial.TutorialDial;
import ogzapp.wordgame.util.UiUtil;

public class GameController {

    public GameScreen gameScreen;
    private Dial dial;
    public Preview preview;
    private BoardView boardView;
    public Level level;
    private boolean stopAnimatingFinalWord;
    private Queue<Word> queue = new Queue<>();
    private Word finalWord;
    public int tempComboCount;
    private HashSet<Word> animatedWords = new HashSet<>();
    private boolean finalWordAnimRunning;
    private boolean queueRunning;
    private int numExtraWordsEarnedThisLevel;

    public void setGameScreen(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
        createLevel();
    }

    public void setDial(Dial dial) {
        this.dial = dial;
    }

    public void setPreview(Preview preview) {
        this.preview = preview;
    }

    private void createLevel() {
        queueRunning = false;
        gameScreen.topPanel.topComboDisplay.setComboCountWithoutAnim(0);
        queue.clear();
        animatedWords.clear();
        numExtraWordsEarnedThisLevel = 0;
        bombDialogWillAppear = false;
        finalWordAnimRunning = false;
        resetTempComboCount();
        int nextLevel = GameData.findFirstIncompleteLevel();
        gameScreen.setBackgroundImage(nextLevel);
        level = GameData.getLevelByIndex(nextLevel);
        gameScreen.createLevelContent(level);
    }

    public void setBoard(BoardView boardView) {
        this.boardView = boardView;
    }

    public void selectingLetters(String text) {
        preview.setAnimatedText(text);
        gameScreen.stopIdleTimer();
    }

    public void selectingLettersFinished(String text) {
        checkWord(text);
    }

    private void checkWord(String text) {
        Word foundWord = evaluateAnswer(text);

        if (foundWord != null) {
            if (foundWord.isSolved) {
                indicateWasSolvedBefore(foundWord);
            } else {
                notifyDialTutorialCorrectAnswerAnimating(true);
                answeredCorrect(foundWord);
            }
        } else {
            notifyDialTutorialCorrectAnswerAnimating(false);
            answeredWrong(text);
        }

        resumeTutorialDialAnimation();

        if (text.length() > 1) handleBoosters(text, foundWord);
        gameScreen.resumeIdleTimer();
    }

    private Word evaluateAnswer(String answer) {
        Array<Word> words = level.getBoardModel().getAllWords(true);
        for (Word word : words) {
            if (word.answer.equals(answer))
                return word;
        }
        return null;
    }

    private void notifyDialTutorialCorrectAnswerAnimating(boolean animating) {
        if (gameScreen.tutorial != null) {
            if (gameScreen.tutorial instanceof TutorialDial) {
                TutorialDial tutorialDial = (TutorialDial) gameScreen.tutorial;
                tutorialDial.correctAnswerAnimating = animating;
            }
        }
    }

    public void resumeTutorialDialAnimation() {
        if (gameScreen.tutorial != null && !level.isSolved) {
            if (gameScreen.tutorial instanceof TutorialDial) {
                TutorialDial tutorialDial = (TutorialDial) gameScreen.tutorial;
                if (!tutorialDial.correctAnswerAnimating) {
                    tutorialDial.resumeAnimation();
                }
            }
        }
    }

    private void indicateWasSolvedBefore(Word word) {
        if (!ConfigProcessor.muted) {
            Sound sound = gameScreen.wordConnectGame.resourceManager.get(ResourceManager.SFX_FOUND_BEFORE, Sound.class);
            sound.play(SoundConfig.SFX_WORD_WAS_SOLVED_BEFORE_VOLUME);
        }

        Array<CellView> cellViewsToAnimate = boardView.findWordCellViews(word);
        float cellWidth = cellViewsToAnimate.get(0).getWidth();
        Direction dir = word.direction;
        float x, y;

        if (dir == Direction.ACROSS) {
            x = cellWidth * 0.1f;
            y = 0;
        } else {
            x = 0;
            y = cellWidth * 0.1f;
        }

        for (int i = 0; i < cellViewsToAnimate.size; i++) {
            CellView cellView = cellViewsToAnimate.get(i);
            Action a = Actions.moveBy(x, y, 0.05f);
            Action b = Actions.moveBy(-x * 2f, -y * 2f, 0.1f);
            Action c = Actions.moveBy(x * 2f, y * 2f, 0.1f);
            Action d = Actions.moveBy(-x, -y, 0.05f);
            SequenceAction sequenceAction = new SequenceAction(a, b, c, d);
            if (i == cellViewsToAnimate.size - 1) {
                preview.fadeOut();
                dial.clearSelection();
            }
            cellView.addAction(sequenceAction);
        }
    }

    private void answeredCorrect(Word foundWord) {
        if (animatedWords.size() == 0)
            animatedWords.add(foundWord);
        gameScreen.stopIdleTimer();
        saveDataBeforeLetterAnimation(foundWord);
        gameScreen.runSmoke(level.comboCount);
        checkCorrectWordCellsForBoostersBeforeAnimation(foundWord);
        checkIfLevelEnded();
        animateCorrectAnswer(foundWord, level.index);
        dial.clearSelection();
        preview.setDirty();
        preview.fadeOut();
        stopAnimatingFinalWord = true;
    }

    private void saveDataBeforeLetterAnimation(Word foundWord) {
        foundWord.isSolved = true;
        GameData.saveSolvedWord(foundWord.id);
        if (foundWord.hasRocket) {
            GameData.deleteWordWithRocket(foundWord.id);
        }
        markWordCellsAsSolved(foundWord);
    }

    private void checkCorrectWordCellsForBoostersBeforeAnimation(Word foundWord) {
        Array<CellView> cellViewsToAnimate = boardView.findWordCellViews(foundWord);
        for (CellView target : cellViewsToAnimate) {
            if (target.hasMonster) boardView.monster.hit = true;
            if (target.ufo != null) target.ufo.stopChrono();
            if (target.bomb != null) target.bomb.hit = true;
            if (target.goldPack != null) {
                target.goldPack.hit = true;
                GameData.setGoldPackHasBeenConsumedInThisLevel();
                GameData.removeNumberOfGoldPackMoves();
            }
        }
        if (boardView.monster != null && !boardView.monster.hit) {
            gameScreen.monsterJump();
        }
    }

    private void checkIfLevelEnded() {
        level.isSolved = boardView.isBoardSolved(false);
        if (level.isSolved) {
            gameScreen.stage.getRoot().setTouchable(Touchable.disabled);
            terminateDialTutorial();
            saveLevelEndData();
        }
    }

    private void saveLevelEndData() {
        DictionaryDialog.words = level.getWordsAsString();
        GameData.updateFirstIncompleteLevelIndex(++level.index);
        // Bonus kelimeler 50/50 dolup coin aktarılana kadar silinmez.
        clearLevelRelatedData(false, true);
    }

    // ========== ANA ANİMASYON METODU (SIRALI + BOOSTER) ==========
    private void animateCorrectAnswer(final Word foundWord, final int levelIndex) {
        if (!ConfigProcessor.muted) {
            Sound successSfx = gameScreen.wordConnectGame.resourceManager.get(ResourceManager.SFX_SUCCESS, Sound.class);
            successSfx.play(SoundConfig.SFX_SUCCESS_VOLUME);
        }

        if (UIConfig.ENABLE_CAMERA_SHAKE && this.level.comboCount > 0) {
            gameScreen.cameraShaker.startShaking(this.level.comboCount * UIConfig.getComboShakeAmount(this.level.comboCount) * gameScreen.stage.getWidth());
        }

        final Array<CellView> cellViewsToAnimate = boardView.findWordCellViews(foundWord);
        markAdjacentWordsAsCompletedAfterCorrectAnswer(cellViewsToAnimate, foundWord);

        // Harfler daireden değil, üstteki bulunan kelime (önizleme) kutusundan ilgili tahta kutularına doğru uçar
        for (int i = 0; i < cellViewsToAnimate.size; i++) {
            final CellView cellView = cellViewsToAnimate.get(i);
            Letter source = preview.letters.get(i);

            final AnimationLabel label = Pools.animationLetterPool.obtain();
            label.cellView = cellView;

            label.setText(String.valueOf(source.letter), ConfigProcessor.getLevelColor(levelIndex), source.getHeight(), gameScreen.wordConnectGame.resourceManager);

            Vector2 sourceVec2 = source.label.localToStageCoordinates(source.getVec2Zero());
            label.setPosition(sourceVec2.x, sourceVec2.y);
            gameScreen.stage.addActor(label);

            float margin = cellView.getWidth() * (1f - CellView.solvedTileSizeCoef) * 0.5f;
            boolean last = i == cellViewsToAnimate.size - 1;

            label.animateCorrectAnswer(this, i * 0.1f, margin, last ? correctAnswerLettersFinished : null);
        }

        queue.addFirst(foundWord);

        preview.setDirty();
        preview.fadeOut();
    }

    private Runnable correctAnswerLettersFinished = new Runnable() {
        @Override
        public void run() {
            if (!queueRunning && queue.size > 0) {
                queueRunning = true;
                processQueue();
            }
        }
    };
    // ===============================================================

    private void processQueue() {
        if (!queue.isEmpty()) {
            processNextItemInQueue(queue.last());
        }
    }

    private void processNextItemInQueue(Word next) {
        if (next.error) {
            gameScreen.resetCombo(queueAnimationFinished);
            if (!ConfigProcessor.muted) {
                Sound wrong = gameScreen.wordConnectGame.resourceManager.get(ResourceManager.SFX_WRONG, Sound.class);
                wrong.play(SoundConfig.SFX_WRONG_VOLUME);
            }
        } else {
            level.comboCount++;
            GameData.saveComboCount(level.comboCount);

            if (level.comboCount > 1) {
                int n = level.comboCount - 1;
                // Yan panel gizli (setComboState yorumda)
                // if(gameScreen.sideComboDisplay != null) gameScreen.sideComboDisplay.setComboState(n);
                gameScreen.comboShaderAnim(queueAnimationFinished, n);
                // if(!bombDialogWillAppear) gameScreen.showComboFeedback();
                if (gameScreen.dialAnimationContainer != null)
                    gameScreen.dialAnimationContainer.setStage(n);
            } else {
                boardView.addAction(new SequenceAction(Actions.delay(0.5f), Actions.run(queueAnimationFinished)));
            }
        }
    }

    private Runnable queueAnimationFinished = new Runnable() {
        @Override
        public void run() {
            Word word = null;
            if (!queue.isEmpty()) {
                word = queue.removeLast();
                if (!word.error) {
                    animatedWords.add(word);
                }
            }

            boolean allAnimated = animatedWords.size() == level.getWordCount();
            if (queue.size == 0) queueRunning = false;

            if (level.isSolved && allAnimated) {
                if (boardView.getRevealedCells().size > 0) {
                    boardView.convertRevealedCellToSolved();
                }
                stopAnimatingFinalWord = true;
                GameData.saveComboCount(0);
                GameData.saveComboReward(0);
                gameScreen.levelFinished();
                terminateDialTutorial();
                levelEndSfx();
            } else {
                finalWordAnimationChecker.run();
                gameScreen.resumeIdleTimer();
                if (word != null && !word.error) checkSolvedWordForDialTutorial();
                if (!queue.isEmpty()) {
                    processQueue();
                }
            }
        }
    };

    public void animateBoostersAfterLetterAnimation(CellView cellView) {
        checkUfo(cellView);
        checkBomb(cellView);
        checkGoldPack(cellView);
        checkMonster(cellView);
        checkGoldCoin(cellView);
    }

    public void resetTempComboCount() {
        tempComboCount = 0;
    }

    public void setComboAnimatedWordCount(Set<Integer> ids) {
        for (Integer id : ids) {
            Word word = new Word();
            word.id = id;
            word.isSolved = true;
            animatedWords.add(word);
        }
    }

    public void setTempComboCount(int n) {
        tempComboCount = n;
    }

    protected void levelEndSfx() {
        if (!ConfigProcessor.muted) {
            Sound sound = gameScreen.wordConnectGame.resourceManager.get(ResourceManager.SFX_LEVEL_END, Sound.class);
            sound.play(SoundConfig.SFX_LEVEL_END_VOLUME);
        }
    }

    private void terminateDialTutorial() {
        if (gameScreen.tutorial != null && gameScreen.tutorial instanceof TutorialDial) {
            gameScreen.tutorial.remove();
            gameScreen.tutorial = null;
            GameData.saveTutorialStep(Constants.TUTORIAL_DIAL);
        }
    }

    private void checkSolvedWordForDialTutorial() {
        if (gameScreen.tutorial != null && !level.isSolved && gameScreen.tutorial instanceof TutorialDial) {
            ((TutorialDial) gameScreen.tutorial).nextWord();
        }
    }

    private void answeredWrong(String answer) {
        if (answer.length() > 1) {
            if (!isExtraWord(answer) || answer.length() < Constants.MIN_LETTERS) {
                wrongAnswer();
            } else {
                preview.fadeOut();
            }
        } else {
            preview.fadeOut();
        }
        dial.clearSelection();
    }

    private void wrongAnswer() {
        preview.shake();
        Word error = new Word();
        error.error = true;
        queue.addFirst(error);
        if (!queueRunning && queue.size == 1 && queue.last().error) {
            queueRunning = true;
            processQueue();
        }
    }

    private void handleBoosters(String text, Word foundWord) {
        if (text.length() > 1 && boardView.bomb != null)
            handleBomb(foundWord);
        if (text.length() > 1 && boardView.goldPack != null)
            handleGoldPack();
        if (foundWord != null && foundWord.answer.length() > 1 && boardView.monster != null)
            handleMonster(foundWord);
    }

    public boolean bombDialogWillAppear;

    private void handleBomb(final Word word) {
        if (boardView.bomb != null) {
            if (!level.isSolved && !boardView.bomb.hit)
                boardView.bomb.setCount(boardView.bomb.getCount() - 1);
            int movesLeft = boardView.bomb.getCount();
            if (movesLeft == 0) {
                gameScreen.stage.getRoot().setTouchable(Touchable.disabled);
                gameScreen.stage.getRoot().addAction(
                        Actions.sequence(
                                Actions.delay(0.5f),
                                Actions.run(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (gameScreen.wordConnectGame.adManager == null) {
                                            gameScreen.explodeBomb();
                                        } else {
                                            if (gameScreen.wordConnectGame.adManager.isRewardedAdEnabledToEarnMoves())
                                                gameScreen.showBombDialog();
                                            else
                                                gameScreen.explodeBomb();
                                        }
                                    }
                                })
                        )
                );
                bombDialogWillAppear = true;
            }
        }
    }

    public void revertGameDataToPreBombState() {
        int count = GameData.getExtraWordsCount();
        int oldCount = count - numExtraWordsEarnedThisLevel;
        GameData.saveExtraWordsCount(oldCount);
        GameData.removeLastExtraWords(numExtraWordsEarnedThisLevel);
    }

    private void handleGoldPack() {
        int movesLeft = boardView.goldPack.getCount();
        if (movesLeft > 1) {
            if (!level.isSolved && !boardView.goldPack.hit) {
                boardView.goldPack.setCount(boardView.goldPack.getCount() - 1);
            }
        }
    }

    private void handleMonster(Word word) {
        if (!level.isSolved && word == null)
            gameScreen.monsterJump();
    }

    private void markAdjacentWordsAsCompletedAfterCorrectAnswer(Array<CellView> cellViews, Word solvedWord) {
        for (CellView cellView : cellViews) {
            Word across = cellView.cellData.acrossWord;
            Word down = cellView.cellData.downWord;
            if (across != null && !across.isSolved && !across.equals(solvedWord))
                checkAdjacentWordSolved(across);
            if (down != null && !down.isSolved && !down.equals(solvedWord))
                checkAdjacentWordSolved(down);
        }
    }

    private void checkAdjacentWordSolved(Word word) {
        final Array<CellView> cellViewsToAnimate = boardView.findWordCellViews(word);
        for (CellView cellView : cellViewsToAnimate) {
            if (cellView.cellData.getState() == Constants.TILE_STATE_DEFAULT ||
                    cellView.cellData.getState() == Constants.TILE_STATE_COINED ||
                    cellView.cellData.getState() == Constants.TILE_STATE_BOMBED ||
                    cellView.cellData.getState() == Constants.TILE_STATE_GOLD_PACKED ||
                    cellView.cellData.getState() == Constants.TILE_STATE_UFO ||
                    cellView.cellData.getState() == Constants.TILE_STATE_MONSTER) {
                return;
            }
        }
        word.isSolved = true;
        animatedWords.add(word);
        for (CellView cellView : cellViewsToAnimate) {
            cellView.cellData.setState(Constants.TILE_STATE_SOLVED);
            // HATA DÜZELTMESİ: bu satır eksikti - kesişen bir kelime
            // dolaylı olarak (başka bir kelime çözülürken) tamamlandığında
            // hücre durumu sadece HAFIZADA "SOLVED" oluyordu, kalıcı olarak
            // KAYDEDİLMİYORDU (markWordCellsAsSolved() metodundaki gibi).
            // Sonuç: oyunu kapatıp yeniden açınca bu hücreler mavi "solved"
            // kutusunu kaybediyor, sadece harf kalıyordu. Artık diğer
            // metotla AYNI şekilde kalıcı olarak da kaydediliyor.
            GameData.saveTileState(cellView.cellData.getX(), cellView.cellData.getY(), Constants.TILE_STATE_SOLVED);
        }
        cellViewsToAnimate.get(0).addAction(Actions.sequence(
                Actions.delay(AnimationLabel.LETTER_ANIM_SPEED * 3),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        revealWithDelay(cellViewsToAnimate);
                    }
                })
        ));
    }

    private void revealWithDelay(Array<CellView> list) {
        for (int i = 0; i < list.size; i++) {
            list.get(i).fadeInSolvedBg(i * 0.1f);
        }
    }

    public void findAndCompleteInCompleteCellViewsAfterGivingHint(Array<CellView> cellViews) {
        Array<CellView> revealedCellVies = new Array<>();
        int count = 0;
        for (CellView cellView : cellViews) {
            count += findAndCompleteInCompleteWordsOfCellView(cellView, revealedCellVies);
        }
        revealWithDelay(revealedCellVies);
        checkIfLevelEnded();
        if (count > 0) {
            boardView.addAction(new SequenceAction(Actions.delay(revealedCellVies.size * 0.2f), Actions.run(queueAnimationFinished)));
        }
    }

    public int findAndCompleteInCompleteWordsOfCellView(CellView cellView, Array<CellView> cellViews) {
        Word across = cellView.cellData.acrossWord;
        Word down = cellView.cellData.downWord;
        int count = 0;
        if (across != null && !across.isSolved) {
            count += completeWord(across, cellViews);
        }
        if (down != null && !down.isSolved) {
            count += completeWord(down, cellViews);
        }
        return count;
    }

    private int completeWord(Word word, Array<CellView> list) {
        Array<CellView> cellViewsToAnimate = boardView.findWordCellViews(word);
        int count = 0;
        for (CellView cellView : cellViewsToAnimate) {
            if (cellView.cellData.getState() == Constants.TILE_STATE_REVEALED ||
                    cellView.cellData.getState() == Constants.TILE_STATE_SOLVED) {
                count++;
            }
        }
        if (count != cellViewsToAnimate.size) return 0;
        word.isSolved = true;
        markWordCellsAsSolved(word);
        animatedWords.add(word);
        GameData.saveSolvedWord(word.id);
        list.addAll(cellViewsToAnimate);
        return 1;
    }

    private void boosterSfx() {
        if (!ConfigProcessor.muted) {
            Sound sound = gameScreen.wordConnectGame.resourceManager.get(ResourceManager.SFX_HIT_BOOSTER, Sound.class);
            sound.play(SoundConfig.SFX_HIT_BOOSTER_VOLUME);
        }
    }

    private void checkUfo(CellView cellView) {
        if (cellView.ufo != null) {
            gameScreen.setUfoSuccess(cellView);
            boosterSfx();
        }
    }

    private void checkBomb(CellView cellView) {
        if (cellView.bomb != null) {
            gameScreen.setBombSuccess(cellView);
            boosterSfx();
        }
    }

    private void checkGoldPack(CellView cellView) {
        if (cellView.goldPack != null) {
            gameScreen.setGoldPackSuccess(cellView);
            boosterSfx();
        }
    }

    private void checkMonster(CellView cellView) {
        if (cellView.hasMonster && boardView.monster.hit) {
            gameScreen.setMonsterSuccess(cellView);
            boosterSfx();
        }
    }

    private void checkGoldCoin(final CellView cellView) {
        final Actor rocketCoin = cellView.getCoin();
        if (rocketCoin != null) {
            final CoinView coinView = gameScreen.topPanel.coinView;
            Vector2 newPos = rocketCoin.localToActorCoordinates(coinView, new Vector2());
            rocketCoin.remove();
            rocketCoin.setPosition(newPos.x, newPos.y);
            coinView.addActor(rocketCoin);
            final Actor coin = gameScreen.topPanel.coinView.coin;
            Action moveTo = Actions.moveTo(coin.getX(), coin.getY(), 0.6f, Interpolation.cubicInOut);
            rocketCoin.addAction(new SequenceAction(Actions.delay(0.4f), Actions.scaleTo(0, 0, 0.2f)));
            RunnableAction runnableAction = new RunnableAction();
            runnableAction.setRunnable(new Runnable() {
                @Override
                public void run() {
                    coinView.coinPulseAnimation(null);
                    int startFrom = HintManager.getRemainingCoins();
                    gameScreen.topPanel.coinView.incrementCoinLabelWithAnimationAndDeleteCoinImages(startFrom, GameConfig.NUMBER_OF_COINS_EARNED_FOR_TAKING_1_COIN, null);
                    HintManager.setCoinCount(startFrom + GameConfig.NUMBER_OF_COINS_EARNED_FOR_TAKING_1_COIN);
                    cellView.removeCoin();
                }
            });
            SequenceAction sequenceAction = new SequenceAction(moveTo, runnableAction);
            rocketCoin.addAction(sequenceAction);
        }
    }

    public Runnable finalWordAnimationChecker = new Runnable() {
        @Override
        public void run() {
            if (finalWordAnimRunning) return;
            Array<Word> words = level.getBoardModel().getAllWords(false);
            if (words.size == 1) {
                stopAnimatingFinalWord = false;
                finalWord = words.get(0);
                finalWordAnimRunning = true;
                animateFinalWord();
            }
        }
    };

    private void animateFinalWord() {
        if (stopAnimatingFinalWord) return;
        Array<CellView> cellViewsToAnimate = boardView.findWordCellViews(finalWord);
        for (int i = 0; i < cellViewsToAnimate.size; i++) {
            CellView cellView = cellViewsToAnimate.get(i);
            cellView.clearActions();
            DelayAction delay = cellView.getGsDelay1(i * 0.12f);
            Action scaleUp = cellView.getGsScale1(1.15f, 1.15f, 0.1f, null);
            Action scaleDown = cellView.getGsScale2(1, 1, 0.2f, null);
            SequenceAction sequenceAction = new SequenceAction(delay, scaleUp, scaleDown);
            if (i == cellViewsToAnimate.size - 1) {
                sequenceAction.addAction(cellView.getGsDelay2(1f));
                sequenceAction.addAction(cellView.getGsRunnable(endOfFinalWordAnimation));
            }
            cellView.addAction(sequenceAction);
        }
    }

    private Runnable endOfFinalWordAnimation = new Runnable() {
        @Override
        public void run() {
            animateFinalWord();
        }
    };

    public void clearLevelRelatedData(boolean clearExtraWords, boolean updateLastBooster) {
        GameData.clearTileStates();
        GameData.clearSavedSolvedWordsJson();
        GameData.clearWordsWithRocket();
        if (gameScreen.offeredBoosterInThisLevel) {
            GameData.removeUfoHasBeenConsumedInThisLevel();
            GameData.removeBombHasBeenConsumedInThisLevel();
            GameData.removeGoldPackHasBeenConsumedInThisLevel();
            GameData.removeMonsterHasBeenConsumedInThisLevel();
            if (updateLastBooster) GameData.saveLastBoosterType(gameScreen.nextBoosterType);
        }
        if (clearExtraWords) GameData.clearExtraWords();
        GameData.saveComboCount(0);
        GameData.saveComboReward(0);
    }

    public void prepareNextLevel() {
        stopAnimatingFinalWord = true;
        gameScreen.destroyLevel();
        createLevel();
    }

    public void markWordCellsAsSolved(Word word) {
        Array<CellView> cellViews = boardView.findWordCellViews(word);
        for (CellView cellView : cellViews) {
            cellView.cellData.setState(Constants.TILE_STATE_SOLVED);
            GameData.saveTileState(cellView.cellData.getX(), cellView.cellData.getY(), Constants.TILE_STATE_SOLVED);
            if (word.direction == Direction.ACROSS)
                cellView.cellData.acrossWord.isSolved = true;
            else
                cellView.cellData.downWord.isSolved = true;
        }
    }

    public boolean isExtraWord(String answer) {
        if (answer.length() > 1 && answer.length() < Constants.MIN_LETTERS) return false;
        if (GameData.isVulgarWord(answer)) {
            gameScreen.showToast(LanguageManager.get("not_extra_word"));
            if (!ConfigProcessor.muted) {
                Sound wrong = gameScreen.wordConnectGame.resourceManager.get(ResourceManager.SFX_WRONG, Sound.class);
                wrong.play(SoundConfig.SFX_WRONG_VOLUME);
            }
            return true;
        }
        int result = GameData.insertWordToExtraJson(answer);
        int a = (result >> 8) & 0xFF;
        int b = result & 0xFF;
        if (a == 1) {
            if (b == 1) {
                GameData.incrementFoundBonusWordCount();
                animateNewExtraWord();
                if (boardView.bomb != null) numExtraWordsEarnedThisLevel++;
            } else {
                if (gameScreen.extraWordsButton.animating) return true;
                gameScreen.extraWordsButton.animating = true;
                gameScreen.extraWordsButton.clearActions();
                UiUtil.shake(gameScreen.extraWordsButton, false, gameScreen.extraWordsButton.getHeight() * 0.5f, extraWordsShakeFinished);
            }
            if (!ConfigProcessor.muted) {
                Sound wrong = gameScreen.wordConnectGame.resourceManager.get(ResourceManager.SFX_BONUS_WORD, Sound.class);
                wrong.play(SoundConfig.SFX_BONUS_VOLUME);
            }
            return true;
        }
        return false;
    }

    private Runnable extraWordsShakeFinished = new Runnable() {
        @Override
        public void run() {
            gameScreen.extraWordsButton.animating = false;
            gameScreen.positionExtraWordButton();
        }
    };

    private void animateNewExtraWord() {
        if (isExtraWordTutorialAhead()) {
            gameScreen.stage.getRoot().setTouchable(Touchable.disabled);
        }
        for (int i = 0; i < preview.letters.size; i++) {
            Letter source = preview.letters.get(i);
            final AnimationLabel label = Pools.animationLetterPool.obtain();
            label.setText(String.valueOf(source.letter), ConfigProcessor.getLevelColor(level.index), source.getHeight(), gameScreen.wordConnectGame.resourceManager);
            Vector2 sourceVec2 = source.label.localToStageCoordinates(source.getVec2Zero());
            label.setPosition(sourceVec2.x, sourceVec2.y);
            gameScreen.stage.addActor(label);
            float scaleTo = 0.2f;
            float targetX = gameScreen.extraWordsButton.getX() + (gameScreen.extraWordsButton.getWidth() - label.getWidth() * scaleTo) * 0.5f;
            float targetY = gameScreen.extraWordsButton.getY() + (gameScreen.extraWordsButton.getHeight() * 0.5f);
            boolean lastIteration = i == preview.letters.size - 1;
            label.animateExtraWord(this, i * 0.1f, sourceVec2, targetX, targetY, scaleTo, lastIteration);
        }
    }

    private boolean isExtraWordTutorialAhead() {
        return UIConfig.INTERACTIVE_TUTORIAL_ENABLED && !GameData.isExtraWordsTutorialDisplayed1() && gameScreen.tutorial == null;
    }

    public Runnable extraWordsGrowAndShrinkFinished() {
        return new Runnable() {
            @Override
            public void run() {
                if (isExtraWordTutorialAhead()) {
                    gameScreen.tutorialExtraWords();
                }
                if (GameData.getExtraWordsCount() == GameConfig.NUMBER_OF_BONUS_WORDS_TO_FIND_FOR_REWARD) {
                    gameScreen.awardBonusWordsRewardAutomatically();
                }
            }
        };
    }
}