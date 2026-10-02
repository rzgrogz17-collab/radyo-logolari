package com.example.satranc.ui

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.satranc.AdConfig
import com.example.satranc.AdManager
import com.example.satranc.BannerAdView
import com.example.satranc.EndReason
import com.example.satranc.GameModel
import com.example.satranc.PlayMode
import com.example.satranc.R
import com.example.satranc.Screen
import com.example.satranc.chess.Chess
import com.example.satranc.chess.Difficulty
import com.example.satranc.chess.Puzzles

@Composable
fun SalonApp(model: GameModel) {
    val state by model.state.collectAsState()
    val activity = LocalContext.current as Activity
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF10182A), Ink, Color(0xFF07060C))))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        when (state.screen) {
            Screen.MENU -> MenuScreen(model, state)
            Screen.GAME -> GameScreen(model, state, activity)
            Screen.SETTINGS -> SettingsScreen(model, state, activity)
            Screen.STATS -> InfoScreen(
                title = stringResource(R.string.stats_title),
                onBack = { model.open(Screen.MENU) }
            ) { StatsBody(state) }
            Screen.RULES -> InfoScreen(
                title = stringResource(R.string.rules_title),
                onBack = { model.open(Screen.MENU) }
            ) { RulesBody() }
            Screen.PUZZLES -> PuzzleList(model, state)
        }
    }
}

@Composable
private fun MenuScreen(model: GameModel, state: com.example.satranc.GameState) {
    val settings = state.settings
    val theme = boardTheme(settings.themeId)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.menu_kicker), color = Gold, letterSpacing = 3.sp, fontSize = 12.sp)
            Text(stringResource(R.string.menu_title), fontFamily = FontFamily.Serif, fontSize = 46.sp, color = Ivory, lineHeight = 48.sp)
            Text(stringResource(R.string.menu_sub), color = Mist, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            ChessBoard(
                board = Chess.start().board,
                theme = theme,
                style = settings.pieceStyle,
                flipped = false,
                showCoords = false,
                showDots = false,
                selected = null,
                targets = emptySet(),
                lastFrom = -1,
                lastTo = -1,
                hintFrom = -1,
                hintTo = -1,
                checkSquare = -1,
                onTap = {},
                modifier = Modifier.fillMaxWidth(0.62f).align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(18.dp))
            if (state.canContinue) {
                GoldButton(stringResource(R.string.continue_game)) { model.continueGame() }
                Spacer(Modifier.height(10.dp))
            }
            GoldButton(stringResource(R.string.play_ai)) { model.newMatch(PlayMode.VS_AI) }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(stringResource(R.string.play_local), Modifier.weight(1f)) { model.newMatch(PlayMode.LOCAL) }
                GhostButton(stringResource(R.string.puzzles), Modifier.weight(1f)) { model.open(Screen.PUZZLES) }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.difficulty))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { level ->
                    ChoiceChip(
                        text = diffName(level),
                        selected = settings.difficulty == level,
                        modifier = Modifier.weight(1f)
                    ) { model.updateSettings { it.copy(difficulty = level) } }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel(stringResource(R.string.side))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(stringResource(R.string.white), settings.humanWhite, Modifier.weight(1f)) {
                    model.updateSettings { it.copy(humanWhite = true) }
                }
                ChoiceChip(stringResource(R.string.black), !settings.humanWhite, Modifier.weight(1f)) {
                    model.updateSettings { it.copy(humanWhite = false) }
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel(stringResource(R.string.themes))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BoardThemes.forEach { item ->
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(item.dark)
                            .border(
                                width = if (item.id == settings.themeId) 2.dp else 1.dp,
                                color = if (item.id == settings.themeId) Gold else item.light,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { model.updateSettings { it.copy(themeId = item.id) } }
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .size(18.dp)
                                .background(item.light)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniLink(stringResource(R.string.settings), Icons.Filled.Settings, Modifier.weight(1f)) { model.open(Screen.SETTINGS) }
                MiniLink(stringResource(R.string.stats), Icons.Filled.BarChart, Modifier.weight(1f)) { model.open(Screen.STATS) }
                MiniLink(stringResource(R.string.rules), Icons.AutoMirrored.Filled.MenuBook, Modifier.weight(1f)) { model.open(Screen.RULES) }
            }
            Spacer(Modifier.height(16.dp))
        }
        BannerAdView(Modifier.background(Color(0xFF0B0F16)))
    }
}

@Composable
private fun GameScreen(model: GameModel, state: com.example.satranc.GameState, activity: Activity) {
    val view = LocalView.current
    var seen by remember { mutableIntStateOf(state.moveSerial) }
    LaunchedEffect(state.moveSerial) {
        if (state.moveSerial > seen && state.settings.haptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        seen = state.moveSerial
    }
    val theme = boardTheme(state.settings.themeId)
    val over = state.endReason != EndReason.NONE
    var reviewing by remember(state.moveSerial, state.endReason) { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIcon(Icons.AutoMirrored.Filled.ArrowBack) { model.leaveGame(activity, Screen.MENU) }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        when (state.mode) {
                            PlayMode.VS_AI -> stringResource(R.string.play_ai)
                            PlayMode.LOCAL -> stringResource(R.string.play_local)
                            PlayMode.PUZZLE -> Puzzles.all[state.puzzleIndex].title
                        },
                        color = Ivory,
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp
                    )
                    Text(statusLine(state), color = if (state.status == com.example.satranc.chess.Status.CHECK) Danger else Mist, fontSize = 12.sp)
                }
                if (state.mode == PlayMode.LOCAL && state.sans.isNotEmpty()) {
                    RoundIcon(Icons.Filled.Refresh) { model.redo() }
                }
                RoundIcon(if (state.settings.sound) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff) {
                    model.updateSettings { it.copy(sound = !it.sound) }
                }
            }
            PlayerBar(
                name = opponentName(state),
                white = !bottomIsWhite(state),
                active = state.whiteToMove != bottomIsWhite(state) && !over,
                ms = if (bottomIsWhite(state)) state.blackMs else state.whiteMs,
                clock = state.clockOn,
                captured = if (bottomIsWhite(state)) state.takenByBlack else state.takenByWhite,
                style = state.settings.pieceStyle,
                thinking = state.aiThinking && state.mode == PlayMode.VS_AI && state.whiteToMove != state.humanWhite
            )
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val side = if (maxWidth < maxHeight) maxWidth else maxHeight
                ChessBoard(
                    board = state.board,
                    theme = theme,
                    style = state.settings.pieceStyle,
                    flipped = state.flipped,
                    showCoords = state.settings.showCoords,
                    showDots = state.settings.showDots,
                    selected = state.selected,
                    targets = state.targets,
                    lastFrom = state.lastFrom,
                    lastTo = state.lastTo,
                    hintFrom = state.hintFrom,
                    hintTo = state.hintTo,
                    checkSquare = state.checkSquare,
                    onTap = model::onSquare,
                    modifier = Modifier.size(side)
                )
            }
            PlayerBar(
                name = youName(state),
                white = bottomIsWhite(state),
                active = state.whiteToMove == bottomIsWhite(state) && !over,
                ms = if (bottomIsWhite(state)) state.whiteMs else state.blackMs,
                clock = state.clockOn,
                captured = if (bottomIsWhite(state)) state.takenByWhite else state.takenByBlack,
                style = state.settings.pieceStyle,
                thinking = false
            )
            if (state.puzzleNote == "no") {
                Text(
                    stringResource(R.string.puzzle_no),
                    color = Danger,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.sans.forEachIndexed { index, san ->
                    val mark = if (index % 2 == 0) "${index / 2 + 1}. $san" else san
                    Text(
                        mark,
                        color = if (index == state.sans.lastIndex) Ink else Mist,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (index == state.sans.lastIndex) Gold else InkRaised)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(InkRaised)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Dock(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo)) { model.undo() }
                Dock(Icons.Filled.Lightbulb, stringResource(R.string.hint) + " ${state.hintsLeft}") { model.hint(activity) }
                Dock(Icons.Filled.SwapVert, stringResource(R.string.flip)) { model.flip() }
                Dock(Icons.Filled.Flag, stringResource(R.string.resign)) { model.askResign() }
                Dock(Icons.Filled.Add, stringResource(R.string.new_game)) { model.askNew(activity) }
            }
            BannerAdView(Modifier.background(Color(0xFF0B0F16)))
        }
        if (state.promotion) PromoSheet(state.whiteToMove, state.settings.pieceStyle, model::choosePromotion, model::dismissPromotion)
        if (state.confirmNew) ConfirmSheet(stringResource(R.string.confirm_new), stringResource(R.string.confirm_new_body), model::confirmNewGame, model::cancelDialogs)
        if (state.confirmResign) ConfirmSheet(stringResource(R.string.confirm_resign), stringResource(R.string.confirm_resign_body), model::confirmResign, model::cancelDialogs)
        if (over && reviewing == 0) ResultSheet(state, activity, model) { reviewing = 1 }
    }
}

@Composable
private fun PlayerBar(
    name: String,
    white: Boolean,
    active: Boolean,
    ms: Long,
    clock: Boolean,
    captured: List<Int>,
    style: PieceStyle,
    thinking: Boolean
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) Color(0xFF1A2233) else Color(0xFF10151F))
            .border(1.dp, if (active) GoldDeep else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (white) Ivory else Color(0xFF1A1D27))
                .border(1.dp, GoldDeep, CircleShape)
        )
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Text(if (thinking) stringResource(R.string.thinking) else name, color = Ivory, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row {
                captured.take(8).forEach { piece ->
                    ChessGlyph(piece, style, Modifier.size(16.dp))
                }
            }
        }
        if (clock) {
            val low = ms < 30_000
            Text(formatClock(ms), color = if (low) Danger else Gold, fontFamily = FontFamily.Serif, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ResultSheet(
    state: com.example.satranc.GameState,
    activity: Activity,
    model: GameModel,
    onReview: () -> Unit
) {
    val title = when (state.endReason) {
        EndReason.MATE -> if (state.mode == PlayMode.PUZZLE) stringResource(R.string.puzzle_ok) else stringResource(R.string.mate_title)
        EndReason.DRAW -> stringResource(R.string.draw_title)
        EndReason.RESIGN -> stringResource(R.string.resign_title)
        EndReason.TIME -> stringResource(R.string.time_title)
        EndReason.NONE -> ""
    }
    val body = when {
        state.mode == PlayMode.PUZZLE -> stringResource(R.string.puzzle_ok_body)
        state.endReason == EndReason.DRAW -> stringResource(R.string.draw_body)
        state.mode == PlayMode.LOCAL && state.winnerWhite == true -> stringResource(R.string.white_wins)
        state.mode == PlayMode.LOCAL && state.winnerWhite == false -> stringResource(R.string.black_wins)
        state.winnerWhite == state.humanWhite -> stringResource(R.string.you_win)
        state.winnerWhite != null -> stringResource(R.string.you_lose)
        else -> stringResource(R.string.draw_body)
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(28.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(InkRaised)
                .border(1.dp, GoldDeep, RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Gold, fontFamily = FontFamily.Serif, fontSize = 28.sp, textAlign = TextAlign.Center)
            Text(body, color = Mist, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 10.dp))
            if (state.mode == PlayMode.PUZZLE) {
                GoldButton(stringResource(R.string.puzzle_next)) { model.nextPuzzle(activity) }
            } else {
                GoldButton(stringResource(R.string.new_game)) { model.rematch(activity) }
                Spacer(Modifier.height(8.dp))
                GhostButton(stringResource(R.string.review), Modifier.fillMaxWidth(), onReview)
                Spacer(Modifier.height(8.dp))
                GhostButton(stringResource(R.string.menu), Modifier.fillMaxWidth()) { model.leaveGame(activity, Screen.MENU) }
            }
        }
    }
}

@Composable
private fun PromoSheet(white: Boolean, style: PieceStyle, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val shift = if (white) 0 else 8
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.45f)).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(InkRaised)
                .padding(16.dp)
                .clickable(enabled = false) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.promotion), color = Gold, fontFamily = FontFamily.Serif, fontSize = 20.sp)
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Chess.WQ, Chess.WR, Chess.WB, Chess.WN).forEach { kind ->
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF243044))
                            .clickable { onPick(kind) }
                    ) { ChessGlyph(kind + shift, style, Modifier.fillMaxSize().padding(6.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ConfirmSheet(title: String, body: String, onYes: () -> Unit, onNo: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(28.dp).clip(RoundedCornerShape(22.dp)).background(InkRaised).padding(20.dp)
        ) {
            Text(title, color = Ivory, fontFamily = FontFamily.Serif, fontSize = 22.sp)
            Text(body, color = Mist, modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(stringResource(R.string.no), Modifier.weight(1f), onNo)
                GoldButton(stringResource(R.string.yes), Modifier.weight(1f), onYes)
            }
        }
    }
}

@Composable
private fun SettingsScreen(model: GameModel, state: com.example.satranc.GameState, activity: Activity) {
    val ready by AdManager.adsReady.collectAsState()
    val network by AdManager.activeNetwork.collectAsState()
    val s = state.settings
    InfoScreen(stringResource(R.string.settings_title), { model.open(Screen.MENU) }) {
        ToggleRow(stringResource(R.string.sound_label), s.sound) { model.updateSettings { it.copy(sound = !it.sound) } }
        ToggleRow(stringResource(R.string.haptics), s.haptics) { model.updateSettings { it.copy(haptics = !it.haptics) } }
        ToggleRow(stringResource(R.string.coords), s.showCoords) { model.updateSettings { it.copy(showCoords = !it.showCoords) } }
        ToggleRow(stringResource(R.string.dots), s.showDots) { model.updateSettings { it.copy(showDots = !it.showDots) } }
        ToggleRow(stringResource(R.string.clock), s.clockEnabled) { model.updateSettings { it.copy(clockEnabled = !it.clockEnabled) } }
        SectionLabel(stringResource(R.string.minutes))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15).forEach { minutes ->
                ChoiceChip("$minutes", s.clockMinutes == minutes, Modifier.weight(1f)) {
                    model.updateSettings { it.copy(clockMinutes = minutes) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionLabel(stringResource(R.string.piece_style))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PieceStyle.entries.forEach { style ->
                ChoiceChip(pieceName(style), s.pieceStyle == style, Modifier.weight(1f)) {
                    model.updateSettings { it.copy(pieceStyle = style) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val mode = if (AdConfig.USE_YANDEX_AND_HUAWEI) stringResource(R.string.ad_switch_on) else stringResource(R.string.ad_switch_off)
        Text(
            stringResource(R.string.ad_line, mode, AdManager.detectedCountry.ifBlank { "…" }, network?.name ?: if (ready) "…" else "…"),
            color = Mist,
            fontSize = 12.sp
        )
        if (AdManager.isPrivacyOptionsRequired) {
            Spacer(Modifier.height(10.dp))
            GhostButton(stringResource(R.string.privacy), Modifier.fillMaxWidth()) { AdManager.showPrivacyOptions(activity) }
        }
    }
}

@Composable
private fun PuzzleList(model: GameModel, state: com.example.satranc.GameState) {
    InfoScreen(stringResource(R.string.puzzles), { model.open(Screen.MENU) }) {
        Text(stringResource(R.string.puzzle_help), color = Mist)
        Spacer(Modifier.height(10.dp))
        Puzzles.all.forEachIndexed { index, puzzle ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(InkRaised)
                    .clickable { model.openPuzzle(index) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${index + 1}", color = Gold, fontFamily = FontFamily.Serif, fontSize = 20.sp, modifier = Modifier.width(32.dp))
                Column {
                    Text(puzzle.title, color = Ivory, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.mate_in_one), color = Mist, fontSize = 12.sp)
                }
            }
        }
        Text(
            stringResource(R.string.puzzles_solved, state.stats.puzzles),
            color = Gold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun StatsBody(state: com.example.satranc.GameState) {
    val s = state.stats
    StatLine(stringResource(R.string.played), s.played)
    StatLine(stringResource(R.string.wins), s.wins)
    StatLine(stringResource(R.string.losses), s.losses)
    StatLine(stringResource(R.string.draws), s.draws)
    StatLine(stringResource(R.string.puzzles_solved_label), s.puzzles)
}

@Composable
private fun RulesBody() {
    RuleCard(stringResource(R.string.rule_1_title), stringResource(R.string.rule_1_body))
    RuleCard(stringResource(R.string.rule_2_title), stringResource(R.string.rule_2_body))
    RuleCard(stringResource(R.string.rule_3_title), stringResource(R.string.rule_3_body))
    RuleCard(stringResource(R.string.rule_4_title), stringResource(R.string.rule_4_body))
}

@Composable
private fun InfoScreen(title: String, onBack: () -> Unit, body: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Filled.ArrowBack, onBack)
            Text(title, color = Ivory, fontFamily = FontFamily.Serif, fontSize = 26.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) { body() }
    }
}

@Composable
private fun GoldButton(text: String, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFF0D7A2), Gold, Color(0xFFC99645))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF1A1408), fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(InkRaised)
            .border(1.dp, InkLine, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Ivory, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Gold else InkRaised)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color(0xFF1A1408) else Ivory, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniLink(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(InkRaised)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Gold)
        Text(text, color = Mist, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(InkRaised)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = Gold) }
}

@Composable
private fun Dock(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp)) {
        Icon(icon, null, tint = Gold)
        Text(label, color = Mist, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Mist, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ivory, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = { onChange() })
    }
}

@Composable
private fun StatLine(label: String, value: Int) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(InkRaised).padding(14.dp)
    ) {
        Text(label, color = Mist, modifier = Modifier.weight(1f))
        Text("$value", color = Gold, fontFamily = FontFamily.Serif, fontSize = 20.sp)
    }
}

@Composable
private fun RuleCard(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp)).background(InkRaised).padding(14.dp)) {
        Text(title, color = Gold, fontWeight = FontWeight.SemiBold)
        Text(body, color = Mist, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun diffName(level: Difficulty): String = when (level) {
    Difficulty.EASY -> stringResource(R.string.easy)
    Difficulty.MEDIUM -> stringResource(R.string.medium)
    Difficulty.HARD -> stringResource(R.string.hard)
}

@Composable
private fun pieceName(style: PieceStyle): String = when (style) {
    PieceStyle.CLASSIC -> stringResource(R.string.pieces_classic)
    PieceStyle.GOLD -> stringResource(R.string.pieces_gold)
    PieceStyle.NIGHT -> stringResource(R.string.pieces_night)
}

@Composable
private fun opponentName(state: com.example.satranc.GameState): String = when (state.mode) {
    PlayMode.VS_AI -> stringResource(R.string.salon)
    PlayMode.LOCAL -> if (bottomIsWhite(state)) stringResource(R.string.black) else stringResource(R.string.white)
    PlayMode.PUZZLE -> stringResource(R.string.puzzle_side)
}

@Composable
private fun youName(state: com.example.satranc.GameState): String = when (state.mode) {
    PlayMode.VS_AI -> stringResource(R.string.you)
    PlayMode.LOCAL -> if (bottomIsWhite(state)) stringResource(R.string.white) else stringResource(R.string.black)
    PlayMode.PUZZLE -> stringResource(R.string.you)
}

@Composable
private fun statusLine(state: com.example.satranc.GameState): String {
    if (state.aiThinking) return stringResource(R.string.thinking)
    if (state.endReason == EndReason.RESIGN) return stringResource(R.string.resign_title)
    if (state.endReason == EndReason.TIME) return stringResource(R.string.time_title)
    return when (state.status) {
        com.example.satranc.chess.Status.CHECK -> stringResource(R.string.check)
        com.example.satranc.chess.Status.CHECKMATE -> stringResource(R.string.mate_title)
        com.example.satranc.chess.Status.STALEMATE,
        com.example.satranc.chess.Status.FIFTY,
        com.example.satranc.chess.Status.REPETITION,
        com.example.satranc.chess.Status.MATERIAL -> stringResource(R.string.draw_title)
        else -> if (state.whiteToMove) stringResource(R.string.white_turn) else stringResource(R.string.black_turn)
    }
}

private fun bottomIsWhite(state: com.example.satranc.GameState): Boolean = !state.flipped

private fun formatClock(ms: Long): String {
    val total = (ms / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}
