package com.example.mahjongmaster

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameSplashScreen(theme: BoardTheme = BoardTheme.JADE) {
    var dots by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            dots = (dots + 1) % 4
        }
    }
    val progressInf = rememberInfiniteTransition(label = "prg")
    val progress by progressInf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "progress"
    )
    SceneBackdrop(theme) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            TilePreview("Chun", 110.dp, TileStyle.IVORY, theme)
            Spacer(Modifier.height(28.dp))
            MahjongBrandTitle(40.sp, 22.sp)
            Spacer(Modifier.height(36.dp))
            Text(
                stringResource(R.string.loading) + ".".repeat(dots),
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(1f, 1f), 2f))
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(theme.accent, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    soundManager: SoundManager,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    onStats: () -> Unit,
    onTutorial: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val tiles by viewModel.tiles.collectAsState()
    val level by viewModel.currentLevel.collectAsState()
    val score by viewModel.score.collectAsState()
    val remaining by viewModel.remainingPairs.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val inProgress = tiles.any { it.isVisible }
    val theme = settings.boardTheme

    SceneBackdrop(theme) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            TilePreview("Chun", 92.dp, settings.tileStyle, theme)
            Spacer(Modifier.height(18.dp))
            MahjongBrandTitle(36.sp, 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_tagline),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(stringResource(R.string.highscore_text), formatScore(highScore))
                StatChip(stringResource(R.string.stats_highest), "${stats.highestLevel}")
                StatChip(stringResource(R.string.stats_stars), "★ ${stats.totalStars}")
            }
            Spacer(Modifier.weight(1f))
            if (inProgress) {
                Text(
                    stringResource(R.string.home_continue_hint, level, remaining, formatScore(score)),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            PrimaryCta(
                if (inProgress) stringResource(R.string.home_continue) else stringResource(R.string.home_play),
                GameColors.btnWin,
                onClick = { soundManager.playButton(); onPlay() },
                icon = Icons.Default.PlayArrow
            )
            if (inProgress) {
                Spacer(Modifier.height(10.dp))
                PrimaryCta(
                    stringResource(R.string.home_new_game),
                    GameColors.btnNew,
                    onClick = {
                        soundManager.playButton()
                        viewModel.startNewGame(1)
                        onPlay()
                    }
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeMiniButton(stringResource(R.string.home_settings), Icons.Default.Settings, theme) {
                    soundManager.playButton(); onSettings()
                }
                HomeMiniButton(stringResource(R.string.home_stats), Icons.Default.BarChart, theme) {
                    soundManager.playButton(); onStats()
                }
                HomeMiniButton(stringResource(R.string.home_how_to), Icons.Default.HelpOutline, theme) {
                    soundManager.playButton(); onTutorial()
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HomeMiniButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: BoardTheme,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .weight(1f)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(theme.panelTop.copy(alpha = 0.7f), theme.panelBottom.copy(alpha = 0.9f))))
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SettingsScreen(viewModel: GameViewModel, soundManager: SoundManager, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val theme = settings.boardTheme
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    SceneBackdrop(theme) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ScreenHeader(stringResource(R.string.settings_title), onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                GlassPanel(theme = theme, modifier = Modifier.fillMaxWidth()) {
                    SettingRow(stringResource(R.string.settings_music), null, settings.musicEnabled) {
                        viewModel.updateSettings { it.copy(musicEnabled = !it.musicEnabled) }
                        soundManager.playButton()
                    }
                    SettingRow(stringResource(R.string.settings_sfx), null, settings.sfxEnabled) {
                        viewModel.updateSettings { it.copy(sfxEnabled = !it.sfxEnabled) }
                        soundManager.playButton()
                    }
                    SettingRow(stringResource(R.string.settings_haptics), null, settings.hapticsEnabled) {
                        viewModel.updateSettings { it.copy(hapticsEnabled = !it.hapticsEnabled) }
                        soundManager.playButton()
                    }
                    SettingRow(
                        stringResource(R.string.settings_dim),
                        stringResource(R.string.settings_dim_sub),
                        settings.dimBlockedTiles
                    ) {
                        viewModel.updateSettings { it.copy(dimBlockedTiles = !it.dimBlockedTiles) }
                        soundManager.playButton()
                    }
                    SettingRow(
                        stringResource(R.string.settings_highlight),
                        stringResource(R.string.settings_highlight_sub),
                        settings.highlightPairs
                    ) {
                        viewModel.updateSettings { it.copy(highlightPairs = !it.highlightPairs) }
                        soundManager.playButton()
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.settings_theme), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BoardTheme.entries.forEach { t ->
                        ThemeSwatch(t, selected = t == settings.boardTheme, modifier = Modifier.weight(1f)) {
                            viewModel.updateSettings { it.copy(boardTheme = t) }
                            soundManager.playButton()
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.settings_tiles), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TileStyle.entries.forEach { s ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (s == settings.tileStyle) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                                .border(
                                    1.5.dp,
                                    if (s == settings.tileStyle) theme.accent else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.updateSettings { it.copy(tileStyle = s) }
                                    soundManager.playButton()
                                }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TilePreview("Chun", 56.dp, s, theme)
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(s.titleRes), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                if (AdManager.isPrivacyOptionsRequired) {
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = {
                        (context as? Activity)?.let { AdManager.showPrivacyOptions(it) }
                    }) {
                        Text(stringResource(R.string.settings_privacy), color = Color.White)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ThemeSwatch(theme: BoardTheme, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(theme.sceneTop, theme.sceneBottom)))
            .border(if (selected) 2.dp else 1.dp, if (selected) theme.accent else Color.White.copy(alpha = 0.2f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(theme.tileBack))
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(theme.titleRes),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun StatsScreen(viewModel: GameViewModel, onBack: () -> Unit) {
    val stats by viewModel.stats.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val settings by viewModel.settings.collectAsState()
    BackHandler(onBack = onBack)
    SceneBackdrop(settings.boardTheme) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ScreenHeader(stringResource(R.string.stats_title), onBack)
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                GlassPanel(theme = settings.boardTheme, modifier = Modifier.fillMaxWidth()) {
                    StatLine(stringResource(R.string.highscore_text), formatScore(highScore))
                    StatLine(stringResource(R.string.stats_levels), "${stats.levelsWon}")
                    StatLine(stringResource(R.string.stats_matches), "${stats.totalMatches}")
                    StatLine(stringResource(R.string.stats_best_combo), "×${stats.bestCombo}")
                    StatLine(
                        stringResource(R.string.stats_fastest),
                        if (stats.fastestLevelMs > 0) formatTime((stats.fastestLevelMs / 1000L).toInt()) else "—"
                    )
                    StatLine(stringResource(R.string.stats_play_time), formatPlayHours(stats.totalPlayMs))
                    StatLine(stringResource(R.string.stats_stars), "${stats.totalStars}")
                    StatLine(stringResource(R.string.stats_three_star), "${stats.threeStarLevels}")
                    StatLine(stringResource(R.string.stats_hints), "${stats.hintsUsed}")
                    StatLine(stringResource(R.string.stats_shuffles), "${stats.shufflesUsed}")
                    StatLine(stringResource(R.string.stats_highest), "${stats.highestLevel}")
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
    }
}

private fun formatPlayHours(ms: Long): String {
    val totalMin = (ms / 60_000L).toInt()
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "%d h %02d m".format(h, m) else "%d m".format(m)
}

@Composable
fun TutorialScreen(viewModel: GameViewModel, onDone: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        Triple(R.string.tutorial_1_title, R.string.tutorial_1_body, "Man1"),
        Triple(R.string.tutorial_2_title, R.string.tutorial_2_body, "Pin5"),
        Triple(R.string.tutorial_3_title, R.string.tutorial_3_body, "Sou3"),
        Triple(R.string.tutorial_4_title, R.string.tutorial_4_body, "Chun")
    )
    BackHandler {
        if (page > 0) page-- else onDone()
    }
    SceneBackdrop(settings.boardTheme) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(stringResource(R.string.home_how_to), { onDone() })
            Spacer(Modifier.weight(0.4f))
            val (title, body, tile) = pages[page]
            if (page == 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
                    TilePreview("Man3", 64.dp, settings.tileStyle, settings.boardTheme, dimmed = true)
                    TilePreview("Man3", 64.dp, settings.tileStyle, settings.boardTheme, glowing = true)
                    TilePreview("Man5", 64.dp, settings.tileStyle, settings.boardTheme, selected = true)
                }
            } else {
                TilePreview(tile, 88.dp, settings.tileStyle, settings.boardTheme, glowing = page == 0, hinted = page == 2)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(title), color = GameColors.gold, fontSize = 22.sp, fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(body), color = Color.White.copy(alpha = 0.88f), fontSize = 15.sp,
                textAlign = TextAlign.Center, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == page) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (i == page) settings.boardTheme.accent else Color.White.copy(alpha = 0.3f))
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryCta(
                if (page == pages.lastIndex) stringResource(R.string.tutorial_done)
                else stringResource(R.string.tutorial_next),
                GameColors.btnWin,
                onClick = {
                    if (page == pages.lastIndex) {
                        viewModel.markTutorialSeen()
                        onDone()
                    } else page++
                }
            )
            if (page < pages.lastIndex) {
                TextButton(onClick = { viewModel.markTutorialSeen(); onDone() }) {
                    Text(stringResource(R.string.tutorial_skip), color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = title, tint = Color.White)
        }
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun WinOverlay(
    summary: WinSummary?,
    score: Long,
    highScore: Long,
    theme: BoardTheme,
    onNextLevel: () -> Unit
) {
    var advancing by remember { mutableStateOf(false) }
    val displayScore = summary?.totalScore ?: score
    val animatedScore by animateIntAsState(
        displayScore.toInt().coerceAtMost(Int.MAX_VALUE),
        tween(900),
        label = "scoreUp"
    )
    val stars = summary?.stars ?: 3
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        ConfettiLayer(theme)
        GlassPanel(theme = theme, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            StarRow(stars)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.win_congrats),
                fontSize = 24.sp, fontWeight = FontWeight.Black, color = GameColors.gold,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.8f), Offset(2f, 2f), 4f))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${stringResource(R.string.win_score_prefix)} ${formatScore(animatedScore.toLong())}",
                fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFAB40)
            )
            if (summary != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.win_time_line, formatTime(summary.elapsedSeconds), formatScore(summary.timeBonus)),
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, textAlign = TextAlign.Center
                )
                if (summary.bestCombo >= 2) {
                    Text(
                        stringResource(R.string.win_combo_line, summary.bestCombo),
                        color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp
                    )
                }
            }
            val isRecord = summary?.isNewRecord == true || (summary == null && score >= highScore && highScore > 0)
            if (isRecord) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.win_new_record),
                    fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700)
                )
            }
            Spacer(Modifier.height(20.dp))
            PrimaryCta(
                stringResource(R.string.win_next_level),
                GameColors.btnWin,
                enabled = !advancing,
                onClick = {
                    if (advancing) return@PrimaryCta
                    advancing = true
                    onNextLevel()
                }
            )
        }
    }
}

@Composable
private fun StarRow(stars: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..3).forEach { i ->
            val filled = i <= stars
            val appear = remember { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(stars) {
                delay(i * 180L)
                appear.animateTo(1f, tween(420))
            }
            Text(
                "★",
                fontSize = 40.sp,
                color = if (filled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.22f),
                modifier = Modifier.scale(if (filled) 0.6f + 0.4f * appear.value else 1f),
                style = TextStyle(shadow = Shadow(Color(0xFFFFD700).copy(alpha = 0.7f), Offset.Zero, 12f))
            )
        }
    }
}

@Composable
private fun ConfettiLayer(theme: BoardTheme) {
    val bits = remember {
        List(42) {
            ConfettiBit(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.4f,
                size = 6f + Random.nextFloat() * 8f,
                speed = 0.08f + Random.nextFloat() * 0.18f,
                spin = (Random.nextFloat() - 0.5f) * 140f,
                color = listOf(theme.accent, theme.glowA, theme.glowC, Color(0xFFFFF6D0), Color(0xFFFF8A80))[it % 5]
            )
        }
    }
    val tInf = rememberInfiniteTransition(label = "confetti")
    val t by tInf.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "c")
    Canvas(Modifier.fillMaxSize()) {
        bits.forEachIndexed { i, b ->
            val y = ((b.y + (t + i * 0.017f) * b.speed * 8f) % 1.25f) * size.height
            val x = b.x * size.width + sin((t * 6f + i) * PI.toFloat()) * 18f
            rotate(t * b.spin + i * 20f, Offset(x, y)) {
                drawRoundRect(
                    b.color,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(b.size, b.size * 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                )
            }
        }
    }
}

private data class ConfettiBit(
    val x: Float, val y: Float, val size: Float, val speed: Float, val spin: Float, val color: Color
)
