package com.example.satranc

import android.app.Activity
import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.satranc.chess.Chess
import com.example.satranc.chess.Difficulty
import com.example.satranc.chess.Engine
import com.example.satranc.chess.Position
import com.example.satranc.chess.Puzzles
import com.example.satranc.chess.Status
import com.example.satranc.chess.Undo
import com.example.satranc.ui.PieceStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class Screen { MENU, GAME, SETTINGS, STATS, RULES, PUZZLES }

enum class PlayMode { VS_AI, LOCAL, PUZZLE }

enum class EndReason { NONE, MATE, DRAW, RESIGN, TIME }

data class Settings(
    val themeId: String = "walnut",
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC,
    val showCoords: Boolean = true,
    val showDots: Boolean = true,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val clockEnabled: Boolean = false,
    val clockMinutes: Int = 10,
    val humanWhite: Boolean = true
)

data class Stats(
    val played: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val puzzles: Int = 0
)

data class GameState(
    val screen: Screen = Screen.MENU,
    val settings: Settings = Settings(),
    val stats: Stats = Stats(),
    val mode: PlayMode = PlayMode.VS_AI,
    val board: IntArray = Chess.start().board.copyOf(),
    val whiteToMove: Boolean = true,
    val status: Status = Status.ONGOING,
    val endReason: EndReason = EndReason.NONE,
    val winnerWhite: Boolean? = null,
    val sans: List<String> = emptyList(),
    val selected: Int? = null,
    val targets: Set<Int> = emptySet(),
    val lastFrom: Int = -1,
    val lastTo: Int = -1,
    val hintFrom: Int = -1,
    val hintTo: Int = -1,
    val checkSquare: Int = -1,
    val flipped: Boolean = false,
    val humanWhite: Boolean = true,
    val hintsLeft: Int = 3,
    val aiThinking: Boolean = false,
    val whiteMs: Long = 0,
    val blackMs: Long = 0,
    val clockOn: Boolean = false,
    val promotion: Boolean = false,
    val confirmNew: Boolean = false,
    val confirmResign: Boolean = false,
    val puzzleIndex: Int = 0,
    val puzzleNote: String? = null,
    val canContinue: Boolean = false,
    val moveSerial: Int = 0,
    val takenByWhite: List<Int> = emptyList(),
    val takenByBlack: List<Int> = emptyList()
)

class GameModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("satranc_salon", Application.MODE_PRIVATE)
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

    private val _state = MutableStateFlow(loadShell())
    val state = _state.asStateFlow()

    private var position: Position = Chess.start()
    private val history = ArrayDeque<Rec>()
    private val redo = ArrayDeque<Int>()
    private val keys = ArrayDeque<String>()
    private var promoMoves: List<Int> = emptyList()
    private var resultRecorded = false
    private var offerAdOnLeave = false
    private var aiToken = 0
    private var aiJob: Job? = null
    private var lastTick = SystemClock.elapsedRealtime()

    init {
        keys.add(Chess.key(position))
        viewModelScope.launch {
            while (isActive) {
                delay(200)
                tick()
            }
        }
    }

    fun open(screen: Screen) {
        if (screen == Screen.MENU) cancelAi()
        publish(screen = screen, confirmNew = false, confirmResign = false)
        if (screen == Screen.GAME) lastTick = SystemClock.elapsedRealtime()
    }

    fun updateSettings(block: (Settings) -> Settings) {
        val next = block(_state.value.settings)
        _state.value = _state.value.copy(settings = next)
        saveSettings(next)
    }

    fun newMatch(mode: PlayMode) {
        cancelAi()
        val settings = _state.value.settings
        position = Chess.start()
        history.clear()
        redo.clear()
        keys.clear()
        keys.add(Chess.key(position))
        promoMoves = emptyList()
        resultRecorded = false
        offerAdOnLeave = false
        val minutes = settings.clockMinutes * 60_000L
        _state.value = _state.value.copy(
            screen = Screen.GAME,
            mode = mode,
            humanWhite = settings.humanWhite,
            flipped = mode == PlayMode.VS_AI && !settings.humanWhite,
            hintsLeft = 3,
            whiteMs = minutes,
            blackMs = minutes,
            clockOn = settings.clockEnabled && mode != PlayMode.PUZZLE,
            puzzleNote = null,
            endReason = EndReason.NONE,
            winnerWhite = null,
            confirmNew = false,
            confirmResign = false,
            promotion = false
        )
        clearSelection()
        publish()
        lastTick = SystemClock.elapsedRealtime()
        saveGame()
        if (mode == PlayMode.VS_AI && !settings.humanWhite) launchAi()
    }

    fun openPuzzle(index: Int) {
        cancelAi()
        val puzzle = Puzzles.all[index.coerceIn(0, Puzzles.all.lastIndex)]
        position = Chess.parseFen(puzzle.fen)
        history.clear()
        redo.clear()
        keys.clear()
        keys.add(Chess.key(position))
        resultRecorded = false
        offerAdOnLeave = false
        _state.value = _state.value.copy(
            screen = Screen.GAME,
            mode = PlayMode.PUZZLE,
            puzzleIndex = index,
            flipped = !position.white,
            clockOn = false,
            hintsLeft = _state.value.hintsLeft.coerceAtLeast(1),
            puzzleNote = null,
            endReason = EndReason.NONE,
            winnerWhite = null,
            humanWhite = position.white
        )
        clearSelection()
        publish()
    }

    fun continueGame() {
        if (!restoreGame()) {
            newMatch(PlayMode.VS_AI)
            return
        }
        publish(screen = Screen.GAME)
        lastTick = SystemClock.elapsedRealtime()
        if (_state.value.mode == PlayMode.VS_AI && !isOver() && position.white != _state.value.humanWhite) {
            launchAi()
        }
    }

    fun onSquare(sq: Int) {
        val snap = _state.value
        if (snap.promotion || snap.aiThinking || isOver()) return
        if (snap.mode == PlayMode.VS_AI && position.white != snap.humanWhite) return
        val legal = Chess.legal(position)
        val selected = snap.selected
        if (selected == null) {
            val piece = position.board[sq]
            if (piece != 0 && Chess.isWhite(piece) == position.white) select(sq, legal)
            return
        }
        val options = legal.filter { Chess.from(it) == selected && Chess.to(it) == sq }
        if (options.isEmpty()) {
            val piece = position.board[sq]
            if (piece != 0 && Chess.isWhite(piece) == position.white) select(sq, legal) else clearSelection()
            publish()
            return
        }
        if (options.any { Chess.promo(it) != 0 }) {
            promoMoves = options
            publish(promotion = true)
            return
        }
        play(options.first())
    }

    fun choosePromotion(kind: Int) {
        val move = promoMoves.firstOrNull { Chess.promo(it) == kind } ?: return
        promoMoves = emptyList()
        publish(promotion = false)
        play(move)
    }

    fun dismissPromotion() {
        promoMoves = emptyList()
        publish(promotion = false)
    }

    fun undo() {
        if (_state.value.mode == PlayMode.PUZZLE) return
        cancelAi()
        if (history.isEmpty()) return
        undoOne()
        if (_state.value.mode == PlayMode.VS_AI && history.isNotEmpty() && position.white != _state.value.humanWhite) {
            undoOne()
        }
        resultRecorded = false
        offerAdOnLeave = false
        clearSelection()
        publish(endReason = EndReason.NONE, winnerWhite = null)
        saveGame()
    }

    fun redo() {
        if (_state.value.mode != PlayMode.LOCAL) return
        val move = redo.removeLastOrNull() ?: return
        commit(move, clearRedo = false)
    }

    fun flip() = publish(flipped = !_state.value.flipped)

    fun askNew(activity: Activity) {
        if (isOver() && _state.value.mode != PlayMode.PUZZLE) {
            rematch(activity)
        } else if (history.isNotEmpty() && !isOver()) {
            publish(confirmNew = true)
        } else {
            startOverSameMode()
        }
    }

    fun rematch(activity: Activity) {
        val mode = _state.value.mode
        if (offerAdOnLeave && mode != PlayMode.PUZZLE) {
            offerAdOnLeave = false
            AdManager.showInterstitial(activity) { newMatch(mode) }
        } else {
            newMatch(mode)
        }
    }

    fun askResign() {
        if (!isOver() && _state.value.mode != PlayMode.PUZZLE) publish(confirmResign = true)
    }

    fun cancelDialogs() = publish(confirmNew = false, confirmResign = false, promotion = false)

    fun confirmNewGame() {
        publish(confirmNew = false)
        startOverSameMode()
    }

    fun confirmResign() {
        publish(confirmResign = false)
        val loserWhite = if (_state.value.mode == PlayMode.VS_AI) _state.value.humanWhite else position.white
        finish(EndReason.RESIGN, winnerWhite = !loserWhite)
    }

    fun hint(activity: Activity) {
        if (isOver() || _state.value.aiThinking) return
        if (_state.value.mode == PlayMode.PUZZLE) {
            val mate = Puzzles.mateMoves(Chess.toFen(position)).firstOrNull() ?: return
            publish(hintFrom = Chess.from(mate), hintTo = Chess.to(mate))
            return
        }
        if (_state.value.hintsLeft <= 0) {
            AdManager.showRewarded(activity, onRewardEarned = {
                _state.value = _state.value.copy(hintsLeft = _state.value.hintsLeft + 3)
                spendHint()
            })
            return
        }
        spendHint()
    }

    fun leaveGame(activity: Activity, going: Screen) {
        cancelAi()
        if (offerAdOnLeave && isOver() && _state.value.mode != PlayMode.PUZZLE) {
            offerAdOnLeave = false
            AdManager.showInterstitial(activity) { open(going) }
        } else {
            open(going)
        }
    }

    fun nextPuzzle(activity: Activity) {
        val next = _state.value.puzzleIndex + 1
        if (next >= Puzzles.all.size) {
            leaveGame(activity, Screen.PUZZLES)
        } else {
            openPuzzle(next)
        }
    }

    private fun spendHint() {
        val snap = position.clone()
        val difficulty = if (_state.value.settings.difficulty == Difficulty.EASY) Difficulty.MEDIUM else Difficulty.HARD
        viewModelScope.launch {
            val move = withContext(Dispatchers.Default) { Engine.choose(snap, difficulty, Random(7)) }
            if (move == 0) return@launch
            _state.value = _state.value.copy(
                hintsLeft = (_state.value.hintsLeft - 1).coerceAtLeast(0),
                hintFrom = Chess.from(move),
                hintTo = Chess.to(move)
            )
        }
    }

    private fun play(move: Int) {
        if (_state.value.mode == PlayMode.PUZZLE) {
            val undo = Chess.make(position, move)
            val mate = Chess.status(position, 1) == Status.CHECKMATE
            if (!mate) {
                Chess.unmake(position, move, undo)
                publish(puzzleNote = "no")
                return
            }
            history.add(Rec(move, undo, Chess.san(Chess.parseFen(Puzzles.all[_state.value.puzzleIndex].fen), move)))
            val stats = _state.value.stats.copy(puzzles = _state.value.stats.puzzles + 1)
            saveStats(stats)
            tone(_state.value.settings.sound, win = true)
            _state.value = _state.value.copy(stats = stats, puzzleNote = "yes")
            finish(EndReason.MATE, winnerWhite = !position.white, countStats = false)
            return
        }
        commit(move, clearRedo = true)
    }

    private fun commit(move: Int, clearRedo: Boolean) {
        val san = Chess.san(position, move)
        val undo = Chess.make(position, move)
        history.addLast(Rec(move, undo, san))
        keys.addLast(Chess.key(position))
        if (clearRedo) redo.clear()
        val status = currentStatus()
        tone(_state.value.settings.sound, capture = undo.captured != 0, check = status == Status.CHECK, win = isTerminal(status))
        clearSelection()
        if (isTerminal(status)) finish(if (status == Status.CHECKMATE) EndReason.MATE else EndReason.DRAW, winnerFromStatus(status))
        else publish()
        saveGame()
        if (_state.value.mode == PlayMode.VS_AI && !isOver() && position.white != _state.value.humanWhite) launchAi()
    }

    private fun finish(reason: EndReason, winnerWhite: Boolean?, countStats: Boolean = true) {
        if (countStats && !resultRecorded && _state.value.mode != PlayMode.PUZZLE) {
            resultRecorded = true
            offerAdOnLeave = true
            if (_state.value.mode == PlayMode.VS_AI) {
                val human = _state.value.humanWhite
                val stats = _state.value.stats.let { s ->
                    val played = s.played + 1
                    when {
                        winnerWhite == null -> s.copy(played = played, draws = s.draws + 1)
                        winnerWhite == human -> s.copy(played = played, wins = s.wins + 1)
                        else -> s.copy(played = played, losses = s.losses + 1)
                    }
                }
                saveStats(stats)
                _state.value = _state.value.copy(stats = stats)
            }
        }
        publish(endReason = reason, winnerWhite = winnerWhite)
        saveGame()
    }

    private fun launchAi() {
        val token = ++aiToken
        val snap = position.clone()
        val difficulty = _state.value.settings.difficulty
        publish(aiThinking = true)
        aiJob = viewModelScope.launch {
            val move = withContext(Dispatchers.Default) { Engine.choose(snap, difficulty) }
            if (token != aiToken || move == 0) {
                if (token == aiToken) publish(aiThinking = false)
                return@launch
            }
            publish(aiThinking = false)
            commit(move, clearRedo = true)
        }
    }

    private fun cancelAi() {
        aiToken++
        aiJob?.cancel()
        aiJob = null
    }

    private fun tick() {
        val snap = _state.value
        if (!snap.clockOn || snap.screen != Screen.GAME || isOver()) {
            lastTick = SystemClock.elapsedRealtime()
            return
        }
        val now = SystemClock.elapsedRealtime()
        val delta = (now - lastTick).coerceAtMost(1000)
        lastTick = now
        if (delta <= 0) return
        val white = if (position.white) (snap.whiteMs - delta).coerceAtLeast(0) else snap.whiteMs
        val black = if (!position.white) (snap.blackMs - delta).coerceAtLeast(0) else snap.blackMs
        if (white == 0L && position.white) finish(EndReason.TIME, winnerWhite = false)
        else if (black == 0L && !position.white) finish(EndReason.TIME, winnerWhite = true)
        else _state.value = _state.value.copy(whiteMs = white, blackMs = black)
    }

    private fun startOverSameMode() {
        when (_state.value.mode) {
            PlayMode.PUZZLE -> openPuzzle(_state.value.puzzleIndex)
            else -> newMatch(_state.value.mode)
        }
    }

    private fun select(sq: Int, legal: List<Int>) {
        _state.value = _state.value.copy(
            selected = sq,
            targets = legal.filter { Chess.from(it) == sq }.map { Chess.to(it) }.toSet(),
            hintFrom = -1,
            hintTo = -1,
            puzzleNote = null
        )
    }

    private fun clearSelection() {
        _state.value = _state.value.copy(selected = null, targets = emptySet(), hintFrom = -1, hintTo = -1)
    }

    private fun undoOne() {
        val rec = history.removeLastOrNull() ?: return
        Chess.unmake(position, rec.move, rec.undo)
        if (keys.size > 1) keys.removeLast()
        redo.addLast(rec.move)
    }

    private fun currentStatus(): Status = Chess.status(position, keys.count { it == Chess.key(position) })

    private fun isOver(): Boolean = isTerminal(_state.value.status) || _state.value.endReason != EndReason.NONE

    private fun winnerFromStatus(status: Status): Boolean? = when (status) {
        Status.CHECKMATE -> !position.white
        else -> null
    }

    private fun publish(
        screen: Screen = _state.value.screen,
        flipped: Boolean = _state.value.flipped,
        promotion: Boolean = _state.value.promotion,
        confirmNew: Boolean = _state.value.confirmNew,
        confirmResign: Boolean = _state.value.confirmResign,
        endReason: EndReason = _state.value.endReason,
        winnerWhite: Boolean? = _state.value.winnerWhite,
        puzzleNote: String? = _state.value.puzzleNote,
        hintFrom: Int = _state.value.hintFrom,
        hintTo: Int = _state.value.hintTo,
        aiThinking: Boolean = _state.value.aiThinking
    ) {
        val status = if (endReason == EndReason.RESIGN || endReason == EndReason.TIME) {
            Status.CHECKMATE
        } else currentStatus()
        val checkSq = if (status == Status.CHECK || (status == Status.CHECKMATE && endReason != EndReason.RESIGN && endReason != EndReason.TIME)) {
            Chess.findKing(position, position.white)
        } else -1
        val takenWhite = history.filter { it.undo.white && it.undo.captured != 0 }.map { it.undo.captured }
        val takenBlack = history.filter { !it.undo.white && it.undo.captured != 0 }.map { it.undo.captured }
        _state.value = _state.value.copy(
            screen = screen,
            board = position.board.copyOf(),
            whiteToMove = position.white,
            status = status,
            endReason = endReason,
            winnerWhite = winnerWhite,
            sans = history.map { it.san },
            lastFrom = history.lastOrNull()?.let { Chess.from(it.move) } ?: -1,
            lastTo = history.lastOrNull()?.let { Chess.to(it.move) } ?: -1,
            checkSquare = checkSq,
            flipped = flipped,
            promotion = promotion,
            confirmNew = confirmNew,
            confirmResign = confirmResign,
            aiThinking = aiThinking,
            puzzleNote = puzzleNote,
            hintFrom = hintFrom,
            hintTo = hintTo,
            moveSerial = history.size,
            takenByWhite = takenWhite,
            takenByBlack = takenBlack,
            canContinue = prefs.contains("uci")
        )
    }

    private fun tone(enabled: Boolean, capture: Boolean = false, check: Boolean = false, win: Boolean = false) {
        if (!enabled) return
        val id = when {
            win -> ToneGenerator.TONE_CDMA_CONFIRM
            check -> ToneGenerator.TONE_SUP_ERROR
            capture -> ToneGenerator.TONE_PROP_ACK
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        tone.startTone(id, if (win) 180 else 60)
    }

    private fun loadShell(): GameState {
        val settings = Settings(
            themeId = prefs.getString("theme", "walnut") ?: "walnut",
            pieceStyle = runCatching { PieceStyle.valueOf(prefs.getString("pieces", "CLASSIC") ?: "CLASSIC") }.getOrDefault(PieceStyle.CLASSIC),
            showCoords = prefs.getBoolean("coords", true),
            showDots = prefs.getBoolean("dots", true),
            sound = prefs.getBoolean("sound", true),
            haptics = prefs.getBoolean("haptics", true),
            difficulty = runCatching { Difficulty.valueOf(prefs.getString("diff", "MEDIUM") ?: "MEDIUM") }.getOrDefault(Difficulty.MEDIUM),
            clockEnabled = prefs.getBoolean("clock", false),
            clockMinutes = prefs.getInt("minutes", 10),
            humanWhite = prefs.getBoolean("humanWhite", true)
        )
        val stats = Stats(
            prefs.getInt("played", 0),
            prefs.getInt("wins", 0),
            prefs.getInt("losses", 0),
            prefs.getInt("draws", 0),
            prefs.getInt("puzzles", 0)
        )
        return GameState(settings = settings, stats = stats, canContinue = prefs.contains("uci"))
    }

    private fun saveSettings(s: Settings) {
        prefs.edit()
            .putString("theme", s.themeId)
            .putString("pieces", s.pieceStyle.name)
            .putBoolean("coords", s.showCoords)
            .putBoolean("dots", s.showDots)
            .putBoolean("sound", s.sound)
            .putBoolean("haptics", s.haptics)
            .putString("diff", s.difficulty.name)
            .putBoolean("clock", s.clockEnabled)
            .putInt("minutes", s.clockMinutes)
            .putBoolean("humanWhite", s.humanWhite)
            .apply()
    }

    private fun saveStats(s: Stats) {
        prefs.edit()
            .putInt("played", s.played)
            .putInt("wins", s.wins)
            .putInt("losses", s.losses)
            .putInt("draws", s.draws)
            .putInt("puzzles", s.puzzles)
            .apply()
    }

    private fun saveGame() {
        if (_state.value.mode == PlayMode.PUZZLE) return
        val uci = history.joinToString(" ") { uciOf(it.move) }
        prefs.edit()
            .putString("uci", uci)
            .putString("mode", _state.value.mode.name)
            .putBoolean("savedHumanWhite", _state.value.humanWhite)
            .putBoolean("savedFlip", _state.value.flipped)
            .putLong("wms", _state.value.whiteMs)
            .putLong("bms", _state.value.blackMs)
            .putBoolean("savedClock", _state.value.clockOn)
            .putInt("savedHints", _state.value.hintsLeft)
            .putBoolean("recorded", resultRecorded)
            .putString("end", _state.value.endReason.name)
            .putString("winner", _state.value.winnerWhite?.toString() ?: "")
            .apply()
        _state.value = _state.value.copy(canContinue = true)
    }

    private fun restoreGame(): Boolean {
        val uci = prefs.getString("uci", null) ?: return false
        val mode = runCatching { PlayMode.valueOf(prefs.getString("mode", "VS_AI") ?: "VS_AI") }.getOrDefault(PlayMode.VS_AI)
        if (mode == PlayMode.PUZZLE) return false
        position = Chess.start()
        history.clear()
        redo.clear()
        keys.clear()
        keys.add(Chess.key(position))
        if (uci.isNotBlank()) {
            for (token in uci.split(' ')) {
                val move = Chess.parseUci(position, token) ?: return false
                val san = Chess.san(position, move)
                val undo = Chess.make(position, move)
                history.add(Rec(move, undo, san))
                keys.add(Chess.key(position))
            }
        }
        resultRecorded = prefs.getBoolean("recorded", false)
        val end = runCatching { EndReason.valueOf(prefs.getString("end", "NONE") ?: "NONE") }.getOrDefault(EndReason.NONE)
        val winnerRaw = prefs.getString("winner", "") ?: ""
        _state.value = _state.value.copy(
            mode = mode,
            humanWhite = prefs.getBoolean("savedHumanWhite", true),
            flipped = prefs.getBoolean("savedFlip", false),
            whiteMs = prefs.getLong("wms", 600_000),
            blackMs = prefs.getLong("bms", 600_000),
            clockOn = prefs.getBoolean("savedClock", false),
            hintsLeft = prefs.getInt("savedHints", 3),
            endReason = end,
            winnerWhite = when (winnerRaw) {
                "true" -> true
                "false" -> false
                else -> null
            }
        )
        return true
    }

    private fun uciOf(move: Int): String {
        val promo = Chess.promo(move)
        val base = Chess.squareName(Chess.from(move)) + Chess.squareName(Chess.to(move))
        return if (promo == 0) base else base + "nbrq"[promo - 2]
    }

    override fun onCleared() {
        tone.release()
        super.onCleared()
    }

    private data class Rec(val move: Int, val undo: Undo, val san: String)
}

private fun isTerminal(status: Status): Boolean =
    status != Status.ONGOING && status != Status.CHECK
