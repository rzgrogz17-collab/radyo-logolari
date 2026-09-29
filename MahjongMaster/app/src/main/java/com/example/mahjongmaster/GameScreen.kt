package com.example.mahjongmaster

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    soundManager: SoundManager,
    onExitToHome: () -> Unit
) {
    val tiles by viewModel.tiles.collectAsState()
    val score by viewModel.score.collectAsState()
    val level by viewModel.currentLevel.collectAsState()
    val isGameWon by viewModel.isGameWon.collectAsState()
    val isDeadlocked by viewModel.isDeadlocked.collectAsState()
    val remainingPairs by viewModel.remainingPairs.collectAsState()
    val totalPairs by viewModel.totalPairs.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val hintCount by viewModel.hintCount.collectAsState()
    val shuffleCount by viewModel.shuffleCount.collectAsState()
    val showRescueDialog by viewModel.showRescueDialog.collectAsState()
    val matchableTypes by viewModel.matchableTypes.collectAsState()
    val isMatchAnimating by viewModel.isMatchAnimating.collectAsState()
    val combo by viewModel.comboStreak.collectAsState()
    val elapsed by viewModel.elapsedSeconds.collectAsState()
    val winSummary by viewModel.winSummary.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val boardEpoch by viewModel.boardEpoch.collectAsState()
    val shuffleEpoch by viewModel.shuffleEpoch.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val theme = settings.boardTheme

    var showNewGameDialog by remember { mutableStateOf(false) }
    var showNoHintsDialog by remember { mutableStateOf(false) }
    var showNoShufflesDialog by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }

    DisposableEffect(paused, isGameWon) {
        viewModel.setTimerRequested(!paused && !isGameWon)
        onDispose { viewModel.setTimerRequested(false) }
    }

    LaunchedEffect(viewModel, settings.hapticsEnabled) {
        viewModel.events.collect { event ->
            if (!settings.hapticsEnabled) return@collect
            when (event) {
                is GameEvent.Selected -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                is GameEvent.Matched -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                is GameEvent.Blocked -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                is GameEvent.Won -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    BackHandler {
        if (isGameWon) return@BackHandler
        if (paused) paused = false else paused = true
    }

    if (showNewGameDialog) {
        FancyDialog(
            icon = Icons.Default.Refresh,
            iconColor = GameColors.btnNew,
            title = stringResource(R.string.dialog_new_game_title),
            message = stringResource(R.string.dialog_new_game_msg),
            confirmText = stringResource(R.string.dialog_yes),
            confirmColor = GameColors.btnNew,
            dismissText = stringResource(R.string.dialog_no),
            onConfirm = { showNewGameDialog = false; viewModel.startNewGame(1) },
            onDismiss = { showNewGameDialog = false },
            theme = theme
        )
    }
    if (showNoHintsDialog) {
        FancyDialog(
            icon = Icons.Default.Lightbulb,
            iconColor = GameColors.btnHint,
            title = stringResource(R.string.no_hints_title),
            message = stringResource(R.string.no_hints_msg),
            confirmText = stringResource(R.string.watch_ad_btn),
            confirmColor = GameColors.btnHint,
            dismissText = stringResource(R.string.dialog_no),
            onConfirm = {
                showNoHintsDialog = false
                if (context is Activity) {
                    AdManager.showRewarded(context, onRewardEarned = { viewModel.refillHints() })
                } else viewModel.refillHints()
            },
            onDismiss = { showNoHintsDialog = false },
            theme = theme
        )
    }
    if (showNoShufflesDialog) {
        FancyDialog(
            icon = Icons.Default.Shuffle,
            iconColor = GameColors.btnMix,
            title = stringResource(R.string.no_shuffles_title),
            message = stringResource(R.string.no_shuffles_msg),
            confirmText = stringResource(R.string.watch_ad_btn),
            confirmColor = GameColors.btnMix,
            dismissText = stringResource(R.string.dialog_no),
            onConfirm = {
                showNoShufflesDialog = false
                if (context is Activity) {
                    AdManager.showRewarded(
                        context,
                        onRewardEarned = { viewModel.refillShuffles() },
                        onAdNotAvailable = { viewModel.emergencyShuffle() }
                    )
                } else viewModel.emergencyShuffle()
            },
            onDismiss = { showNoShufflesDialog = false },
            theme = theme
        )
    }
    if (showRescueDialog) {
        FancyDialog(
            icon = Icons.Default.Warning,
            iconColor = GameColors.btnNew,
            title = stringResource(R.string.rescue_title),
            message = stringResource(R.string.rescue_msg),
            confirmText = stringResource(R.string.rescue_btn),
            confirmColor = GameColors.btnNew,
            dismissText = stringResource(R.string.rescue_cancel),
            onConfirm = { viewModel.dismissRescueDialog(); viewModel.startNewGame(level) },
            onDismiss = { viewModel.dismissRescueDialog() },
            theme = theme
        )
    }

    val onHint: () -> Unit = {
        soundManager.playButton()
        when (viewModel.useHintIfAvailable()) {
            GameViewModel.HintResult.SHOWN -> Unit
            GameViewModel.HintResult.NO_MATCH ->
                Toast.makeText(context, context.getString(R.string.hint_no_match_msg), Toast.LENGTH_SHORT).show()
            GameViewModel.HintResult.NO_HINTS -> {
                if (NetworkUtils.isOnline(context)) showNoHintsDialog = true
                else Toast.makeText(context, context.getString(R.string.no_internet_msg), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val onShuffle: () -> Unit = {
        soundManager.playButton()
        if (!viewModel.useShuffleIfAvailable()) {
            if (NetworkUtils.isOnline(context)) showNoShufflesDialog = true
            else Toast.makeText(context, context.getString(R.string.no_internet_msg), Toast.LENGTH_SHORT).show()
        }
    }

    SceneBackdrop(theme) {
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
            val landscape = maxWidth > maxHeight && maxWidth > 560.dp
            if (landscape) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                        MahjongBoard(
                            tiles, matchableTypes, settings, boardEpoch, shuffleEpoch,
                            viewModel.events, viewModel::onTileClick, viewModel::onBlockedTileTap,
                            Modifier.fillMaxSize()
                        )
                        ComboBadge(combo, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
                        if (isDeadlocked && !isGameWon) {
                            DeadlockBanner(
                                stringResource(R.string.deadlock_banner),
                                Modifier.align(Alignment.BottomCenter).padding(12.dp)
                            )
                        }
                    }
                    Column(
                        Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                            .navigationBarsPadding()
                            .padding(end = 10.dp, top = 8.dp, bottom = 6.dp)
                    ) {
                        GameHud(level, score, remainingPairs, elapsed, highScore, theme)
                        Spacer(Modifier.height(8.dp))
                        PairProgressBar(remainingPairs, totalPairs, theme.accent)
                        Spacer(Modifier.weight(1f))
                        GameDock(
                            canUndo && !isMatchAnimating, hintCount, shuffleCount,
                            isDeadlocked && !isGameWon, !isMatchAnimating,
                            onPause = { soundManager.playButton(); paused = true },
                            onUndo = { soundManager.playButton(); viewModel.undoLastMove() },
                            onHint = onHint, onShuffle = onShuffle,
                            onNew = { soundManager.playButton(); showNewGameDialog = true }
                        )
                        BannerAdView()
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                    GameHud(
                        level, score, remainingPairs, elapsed, highScore, theme,
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    PairProgressBar(
                        remainingPairs, totalPairs, theme.accent,
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        MahjongBoard(
                            tiles, matchableTypes, settings, boardEpoch, shuffleEpoch,
                            viewModel.events, viewModel::onTileClick, viewModel::onBlockedTileTap,
                            Modifier.fillMaxSize()
                        )
                        ComboBadge(combo, Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                        if (isDeadlocked && !isGameWon) {
                            DeadlockBanner(
                                stringResource(R.string.deadlock_banner),
                                Modifier.align(Alignment.BottomCenter).padding(8.dp)
                            )
                        }
                    }
                    GameDock(
                        canUndo && !isMatchAnimating, hintCount, shuffleCount,
                        isDeadlocked && !isGameWon, !isMatchAnimating,
                        onPause = { soundManager.playButton(); paused = true },
                        onUndo = { soundManager.playButton(); viewModel.undoLastMove() },
                        onHint = onHint, onShuffle = onShuffle,
                        onNew = { soundManager.playButton(); showNewGameDialog = true },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                    BannerAdView()
                }
            }
        }

        AnimatedVisibility(
            paused && !isGameWon,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize().zIndex(8000f)
        ) {
            PauseOverlay(
                theme = theme,
                onResume = { paused = false },
                onHome = { paused = false; onExitToHome() },
                onRestart = {
                    paused = false
                    viewModel.restartLevel()
                }
            )
        }

        AnimatedVisibility(
            isGameWon,
            enter = fadeIn() + scaleIn(),
            modifier = Modifier.fillMaxSize().zIndex(9999f)
        ) {
            WinOverlay(
                summary = winSummary,
                score = score,
                highScore = highScore,
                theme = theme,
                onNextLevel = {
                    if (context is Activity) {
                        AdManager.showInterstitial(context) { viewModel.moveToNextLevel() }
                    } else viewModel.moveToNextLevel()
                }
            )
        }
    }
}

@Composable
private fun GameHud(
    level: Int,
    score: Long,
    remaining: Int,
    elapsed: Int,
    highScore: Long,
    theme: BoardTheme,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MahjongBrandTitle(
                bigFontSize = 20.sp, smallFontSize = 11.sp,
                horizontalAlignment = Alignment.Start,
                bigLineHeight = 22.sp, smallLineHeight = 12.sp
            )
            Text(
                stringResource(Difficulty.forLevel(level).titleRes),
                color = theme.accent.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatChip(stringResource(R.string.level_text), "$level", Modifier.weight(1f), theme.accent)
            StatChip(stringResource(R.string.score_text), formatScore(score), Modifier.weight(1.3f))
            StatChip(stringResource(R.string.remaining_text), "$remaining", Modifier.weight(1f))
            StatChip(stringResource(R.string.time_text), formatTime(elapsed), Modifier.weight(1.1f))
        }
        if (highScore > 0) {
            Text(
                "${stringResource(R.string.highscore_text)}  ${formatScore(highScore)}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
            )
        }
    }
}

@Composable
private fun GameDock(
    undoEnabled: Boolean,
    hintCount: Int,
    shuffleCount: Int,
    deadlock: Boolean,
    shuffleEnabled: Boolean,
    onPause: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onShuffle: () -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockButton(stringResource(R.string.btn_pause), Icons.Default.Pause, GameColors.btnMode, onPause)
        DockButton(stringResource(R.string.btn_undo), Icons.Default.Undo, GameColors.btnUndo, onUndo, enabled = undoEnabled)
        DockButton(stringResource(R.string.btn_hint), Icons.Default.Lightbulb, GameColors.btnHint, onHint, badgeCount = hintCount)
        DockButton(
            stringResource(R.string.btn_mix), Icons.Default.Shuffle, GameColors.btnMix, onShuffle,
            enabled = shuffleEnabled, badgeCount = shuffleCount, shake = deadlock
        )
        DockButton(stringResource(R.string.btn_new), Icons.Default.Refresh, GameColors.btnNew, onNew)
    }
}

@Composable
private fun PauseOverlay(
    theme: BoardTheme,
    onResume: () -> Unit,
    onHome: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(theme = theme, modifier = Modifier.fillMaxWidth().padding(28.dp)) {
            Text(
                stringResource(R.string.pause_title),
                color = GameColors.gold, fontSize = 26.sp, fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(22.dp))
            PrimaryCta(stringResource(R.string.pause_resume), GameColors.btnWin, onResume, icon = Icons.Default.PlayArrow)
            Spacer(Modifier.height(10.dp))
            PrimaryCta(stringResource(R.string.pause_restart), GameColors.btnNew, onRestart, icon = Icons.Default.Refresh)
            Spacer(Modifier.height(10.dp))
            PrimaryCta(stringResource(R.string.pause_home), Color(0xFF546E7A), onHome, icon = Icons.Default.Home)
        }
    }
}
