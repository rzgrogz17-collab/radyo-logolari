package tv.garden.global.webapp

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.yandex.mobile.ads.common.MobileAds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#121212"))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(AppConfig.PLAYBACK_USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(httpFactory))
            .build()
            .also {
                it.playWhenReady = true
                it.setHandleAudioBecomingNoisy(true)
            }
        setContent { DiamondTheme { MainAppLogic(this, player!!) } }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                am.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                am.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player?.isPlaying == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(
                    PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                )
            } catch (_: Exception) {
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPiP: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPiP, newConfig)
    }

    fun initializeAds() {
        try {
            MobileAds.initialize(this) {}
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}

@Composable
fun DiamondTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DiamondBlack,
            surface = DiamondPanel,
            primary = DiamondRed
        ),
        content = content
    )
}

@OptIn(UnstableApi::class)
@Composable
fun MainAppLogic(
    activity: MainActivity,
    player: ExoPlayer,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentChannel by remember { mutableStateOf<Channel?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    var retryCount by remember { mutableIntStateOf(0) }
    var didResumeLast by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val inPip = remember(configuration) { activity.isInPictureInPictureMode }
    val currentLocaleTag = remember(configuration) {
        configuration.locales.get(0)?.toLanguageTag() ?: ""
    }
    LaunchedEffect(currentLocaleTag) {
        if (currentLocaleTag.isNotEmpty()) viewModel.reorderByLocale()
    }

    if (AppConfig.ADS_CONSENT_ENABLED) {
        LaunchedEffect(Unit) { activity.initializeAds() }
    }

    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds == 0L) {
                player.pause()
                showRewardedAd(activity) { activity.finish() }
            }
        }
    }

    LaunchedEffect(viewModel.displayedChannels, didResumeLast) {
        if (didResumeLast) return@LaunchedEffect
        if (viewModel.displayedChannels.isEmpty()) return@LaunchedEffect
        val last = viewModel.channelById(viewModel.lastChannelId)
        if (last != null) {
            val idx = viewModel.displayedChannels.indexOfFirst { it.id == last.id }
            if (idx >= 0) {
                listState.scrollToItem(idx)
            }
        } else if (viewModel.lastListIndex > 0) {
            val idx = viewModel.lastListIndex.coerceAtMost(viewModel.displayedChannels.lastIndex)
            listState.scrollToItem(idx)
        }
        didResumeLast = true
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { viewModel.saveListIndex(it) }
    }

    LaunchedEffect(currentChannel?.id, currentChannel?.url) {
        val ch = currentChannel
        retryCount = 0
        if (ch == null) {
            try {
                player.stop()
                player.clearMediaItems()
            } catch (_: Exception) {
            }
            return@LaunchedEffect
        }
        hasError = false
        try {
            player.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.parse(ch.url))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(ch.name)
                            .build()
                    )
                    .build()
            )
            player.prepare()
            player.play()
            viewModel.addToHistory(ch)
            viewModel.saveLastChannel(ch.id)
        } catch (_: Exception) {
            hasError = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    try {
                        player.seekToDefaultPosition()
                        player.prepare()
                        player.play()
                    } catch (_: Exception) {
                    }
                    return
                }
                val failed = currentChannel ?: run { hasError = true; return }
                val playingUrl = player.currentMediaItem?.localConfiguration?.uri?.toString()
                if (!playingUrl.isNullOrEmpty() && playingUrl != failed.url) return
                if (retryCount < 1) {
                    retryCount += 1
                    try {
                        player.prepare()
                        player.play()
                    } catch (_: Exception) {
                    }
                    return
                }
                retryCount = 0
                val result = viewModel.hideBrokenKeepPlace(failed.id)
                Toast.makeText(context, LanguageManager.errorMsg, Toast.LENGTH_SHORT).show()
                val neighbor = result.neighbor
                if (neighbor != null) {
                    currentChannel = neighbor
                    hasError = false
                    val idx = result.neighborIndex
                    if (idx >= 0) {
                        scope.launch {
                            try {
                                listState.animateScrollToItem(idx)
                            } catch (_: Exception) {
                                listState.scrollToItem(idx)
                            }
                        }
                    }
                } else {
                    hasError = true
                    try {
                        player.stop()
                        player.clearMediaItems()
                    } catch (_: Exception) {
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    hasError = false
                    retryCount = 0
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    fun playRelative(delta: Int) {
        val list = viewModel.displayedChannels
        val current = currentChannel ?: return
        val idx = list.indexOfFirst { it.id == current.id }
        if (idx < 0) return
        val nextIdx = (idx + delta).coerceIn(0, list.lastIndex)
        if (nextIdx == idx) return
        currentChannel = list[nextIdx]
        scope.launch {
            try {
                listState.animateScrollToItem(nextIdx)
            } catch (_: Exception) {
            }
        }
    }

    BackHandler(isFullScreen) { isFullScreen = false }

    if (inPip) {
        AndroidView(
            { ctx -> PlayerView(ctx).apply { this.player = player; useController = false } },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    LaunchedEffect(isFullScreen) {
        val win = activity.window
        val ctrl = WindowCompat.getInsetsController(win, win.decorView)
        ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isFullScreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ctrl.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ctrl.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DiamondBlack)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(if (isFullScreen) 1f else 0.35f)
                .background(Color.Black)
        ) {
            if (currentChannel != null) {
                val playingChannel = currentChannel
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            layoutParams = FrameLayout.LayoutParams(-1, -1)
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            subtitleView?.setStyle(
                                CaptionStyleCompat(
                                    Color.White.toArgb(),
                                    Color.Black.copy(alpha = 0.7f).toArgb(),
                                    Color.Transparent.toArgb(),
                                    CaptionStyleCompat.EDGE_TYPE_NONE,
                                    Color.Black.toArgb(), null
                                )
                            )
                            subtitleView?.setBottomPaddingFraction(0.05f)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isFullScreen) GestureOverlay(context as Activity)

                val quality = if (playingChannel != null) detectQuality(playingChannel) else ""
                if (quality.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                            .background(
                                when (quality) {
                                    "4K" -> Color(0xFFFFD700); "FHD" -> Color(0xFF4CAF50)
                                    "HD" -> PastelBlue; else -> Color.Gray
                                }.copy(alpha = 0.85f), RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            quality,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(DiamondRed.copy(0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "● ${LanguageManager.qualityLive}",
                        color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }

                if (isFullScreen) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    ) {
                        IconButton(
                            onClick = { playRelative(-1) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(0.35f), CircleShape)
                        ) {
                            Icon(Icons.Default.SkipPrevious, null, tint = Color.White)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = { playRelative(1) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(0.35f), CircleShape)
                        ) {
                            Icon(Icons.Default.SkipNext, null, tint = Color.White)
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Tv,
                            null,
                            tint = DiamondRed.copy(0.5f),
                            modifier = Modifier.size(50.dp)
                        )
                        Text(LanguageManager.selectChannel, color = Color.Gray)
                        val last = viewModel.channelById(viewModel.lastChannelId)
                        if (last != null) {
                            Spacer(Modifier.height(10.dp))
                            NeonGlassButton(LanguageManager.continueWatching, true) {
                                currentChannel = last
                                val idx = viewModel.displayedChannels.indexOfFirst { it.id == last.id }
                                if (idx >= 0) {
                                    scope.launch { listState.scrollToItem(idx) }
                                }
                            }
                        }
                    }
                }
            }

            if (hasError) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.8f)), Alignment.Center
                ) {
                    Text(LanguageManager.errorMsg, color = DiamondRed, fontWeight = FontWeight.Bold)
                }
            }

            if (remainingSeconds > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .statusBarsPadding()
                        .background(Color.Black.copy(0.3f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        null,
                        tint = Color.White.copy(0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${remainingSeconds / 60}m ${remainingSeconds % 60}s",
                        color = Color.White.copy(0.7f), fontSize = 12.sp
                    )
                }
            }

            IconButton(
                onClick = { isFullScreen = !isFullScreen },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 8.dp)
                    .size(34.dp)
                    .background(Color.Black.copy(0.3f), CircleShape)
            ) {
                Icon(
                    if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    null, tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .alpha(0.7f)
                )
            }
        }

        if (!isFullScreen) {
            Column(Modifier.weight(0.65f)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(DiamondPanel)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactSearchField(
                            value = viewModel.searchQuery,
                            onValueChange = {
                                viewModel.applyFilter(
                                    viewModel.activeFilterMode,
                                    viewModel.selectedFilterName,
                                    it,
                                    showSpinner = false
                                )
                            },
                            hint = LanguageManager.searchHint,
                            modifier = Modifier.fillMaxWidth(0.33f)
                        )
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TvFocusableIconButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent("android.settings.CAST_SETTINGS"))
                                    } catch (_: Exception) {
                                        try {
                                            context.startActivity(Intent("android.settings.WIFI_DISPLAY_SETTINGS"))
                                        } catch (_: Exception) {
                                            Toast.makeText(
                                                context,
                                                LanguageManager.castUnavailable,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                icon = Icons.Rounded.Cast
                            )
                            TvFocusableIconButton(
                                onClick = { showSettingsDialog = true },
                                icon = Icons.Rounded.Settings
                            )
                            TvFocusableIconButton(
                                onClick = {
                                    try {
                                        val shareText =
                                            LanguageManager.shareApp +
                                                "https://play.google.com/store/apps/details?id=${context.packageName}"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(sendIntent, LanguageManager.shareApp.trim())
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            LanguageManager.shareUnavailable,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                icon = Icons.Rounded.Share
                            )
                        }
                    }

                    Spacer(Modifier.height(5.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NeonGlassButton(
                            LanguageManager.menuAll,
                            viewModel.activeFilterMode == "ALL",
                            Modifier.weight(1f)
                        ) { viewModel.applyFilter("ALL", "") }
                        NeonGlassButton(
                            LanguageManager.menuGenres,
                            viewModel.activeFilterMode == "COUNTRY",
                            Modifier.weight(1f)
                        ) { showCountryDialog = true }
                        NeonGlassButton(
                            LanguageManager.menuFavs,
                            viewModel.activeFilterMode == "FAV",
                            Modifier.weight(1f)
                        ) { viewModel.applyFilter("FAV", "") }
                        NeonGlassButton(
                            LanguageManager.menuHistory,
                            viewModel.activeFilterMode == "HISTORY",
                            Modifier.weight(1f)
                        ) { viewModel.applyFilter("HISTORY", "") }
                    }
                }

                when {
                    viewModel.hasNetworkError -> ErrorStateView(LanguageManager.networkError) { viewModel.fetchChannels() }
                    viewModel.hasFetchError -> ErrorStateView(LanguageManager.fetchError) { viewModel.fetchChannels() }
                    viewModel.isLoading && viewModel.displayedChannels.isEmpty() -> {
                        var showSlowHint by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(4000)
                            showSlowHint = true
                        }
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PastelPurple)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    String.format(
                                        LanguageManager.loading,
                                        viewModel.allChannels.size
                                    ), color = Color.Gray, fontSize = 12.sp
                                )
                                if (showSlowHint) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        LanguageManager.slowNetwork,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 40.dp)
                                    )
                                }
                            }
                        }
                    }

                    viewModel.displayedChannels.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(LanguageManager.emptyList, color = Color.Gray)
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 6.dp,
                                end = 6.dp,
                                bottom = if (AppConfig.ADS_CONSENT_ENABLED) 60.dp else 8.dp
                            )
                        ) {
                            itemsIndexed(
                                viewModel.displayedChannels,
                                key = { _, item -> item.id.ifBlank { item.url } }
                            ) { index, item ->
                                ChannelRow(
                                    channel = item,
                                    isFav = viewModel.favSet.contains(item.url),
                                    isPlaying = currentChannel?.id == item.id,
                                    indexLabel = "${index + 1}",
                                    onPlay = {
                                        currentChannel = item
                                        hasError = false
                                        showInterstitialAd(activity)
                                    },
                                    onFav = { viewModel.toggleFavorite(item.url) }
                                )
                            }
                        }
                    }
                }
            }

            if (AppConfig.ADS_CONSENT_ENABLED) {
                YandexBannerAdView()
            }
        }
    }

    if (showCountryDialog) {
        SelectionDialog(
            LanguageManager.menuGenres,
            viewModel.availableCountries,
            viewModel.selectedFilterName,
            viewModel.countryCounts
        ) { country ->
            showCountryDialog = false
            if (country.isNotEmpty()) viewModel.applyFilter("COUNTRY", country)
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            hiddenCount = viewModel.blacklistedIds.size,
            onDismiss = { showSettingsDialog = false },
            onClearCache = {
                val freed = clearAppCache(context)
                Toast.makeText(
                    context,
                    String.format(LanguageManager.cacheCleared, freed),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSetTimer = { minutes ->
                remainingSeconds = minutes * 60L
                showSettingsDialog = false
                if (minutes > 0) Toast.makeText(context, "Timer: $minutes min", Toast.LENGTH_SHORT)
                    .show()
            },
            onOpenPrivacyPolicy = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRIVACY_POLICY_URL))
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                }
            },
            onRestoreHidden = {
                viewModel.restoreHiddenChannels()
                Toast.makeText(context, LanguageManager.restoreBroken, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
