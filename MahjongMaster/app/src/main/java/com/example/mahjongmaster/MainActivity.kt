package com.example.mahjongmaster

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.delay

private const val SCREEN_HOME = "home"
private const val SCREEN_GAME = "game"
private const val SCREEN_SETTINGS = "settings"
private const val SCREEN_STATS = "stats"
private const val SCREEN_TUTORIAL = "tutorial"

class MainActivity : ComponentActivity() {
    private var soundManager: SoundManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        try {
            AdManager.initialize(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Ad init failed", e)
        }
        soundManager = SoundManager(this)
        val viewModel = ViewModelProvider(
            this,
            GameViewModelFactory(applicationContext)
        )[GameViewModel::class.java]

        setContent {
            val sm = soundManager
            Surface(Modifier.fillMaxSize(), color = Color(0xFF0E4438)) {
                if (sm != null) {
                    MahjongApp(viewModel, sm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        soundManager?.startMusic()
    }

    override fun onPause() {
        super.onPause()
        soundManager?.stopMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            soundManager?.release()
            soundManager = null
        }
    }
}

@Composable
private fun MahjongApp(viewModel: GameViewModel, soundManager: SoundManager) {
    var showSplash by remember { mutableStateOf(true) }
    var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }
    var playAfterTutorial by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.soundManager = soundManager
        soundManager.applySettings(viewModel.settings.value)
        soundManager.startMusic()
        delay(2200)
        showSplash = false
    }
    DisposableEffect(Unit) {
        onDispose { /* Activity.onDestroy releases SoundManager */ }
    }

    if (showSplash) {
        GameSplashScreen(viewModel.settings.value.boardTheme)
        return
    }

    when (screen) {
        SCREEN_HOME -> HomeScreen(
            viewModel, soundManager,
            onPlay = {
                if (!viewModel.isTutorialSeen()) {
                    playAfterTutorial = true
                    screen = SCREEN_TUTORIAL
                } else {
                    screen = SCREEN_GAME
                }
            },
            onSettings = { screen = SCREEN_SETTINGS },
            onStats = { screen = SCREEN_STATS },
            onTutorial = {
                playAfterTutorial = false
                screen = SCREEN_TUTORIAL
            }
        )
        SCREEN_GAME -> GameScreen(viewModel, soundManager, onExitToHome = { screen = SCREEN_HOME })
        SCREEN_SETTINGS -> SettingsScreen(viewModel, soundManager, onBack = { screen = SCREEN_HOME })
        SCREEN_STATS -> StatsScreen(viewModel, onBack = { screen = SCREEN_HOME })
        SCREEN_TUTORIAL -> TutorialScreen(viewModel, onDone = {
            viewModel.markTutorialSeen()
            val goToGame = playAfterTutorial
            playAfterTutorial = false
            screen = if (goToGame) SCREEN_GAME else SCREEN_HOME
        })
    }
}
