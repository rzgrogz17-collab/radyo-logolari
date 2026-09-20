package com.globalradio.livetuneinogzapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.globalradio.livetuneinogzapp.MainActivity
import com.globalradio.livetuneinogzapp.R
import com.globalradio.livetuneinogzapp.RadioWidget
import com.globalradio.livetuneinogzapp.model.PlayerState
import com.globalradio.livetuneinogzapp.model.RadioStation
import com.globalradio.livetuneinogzapp.utils.AppSettings
import com.globalradio.livetuneinogzapp.utils.SleepTimerManager
import com.globalradio.livetuneinogzapp.utils.StreamRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@androidx.media3.common.util.UnstableApi
class RadioPlayerService : Service() {

    inner class RadioBinder : Binder() {
        fun getService(): RadioPlayerService = this@RadioPlayerService
    }

    private val binder = RadioBinder()
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    val playerState = MutableLiveData<PlayerState>(PlayerState.Idle)
    var currentStation: RadioStation? = null
        private set

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var currentAlbumArt: Bitmap? = null
    private var artLoadJob: Job? = null

    private var playlist: MutableList<RadioStation> = mutableListOf()
    private var currentIndex: Int = -1
    private var lastSkipElapsed = 0L
    private var sessionStartedAt = 0L

    private var recorder: StreamRecorder? = null
    val recordingState = MutableLiveData(false)
    val recordingEvent = MutableLiveData<Pair<Boolean, String>?>()
    private var fadeJob: Job? = null
    private var targetVolume = 1f

    // ── Yeniden bağlanma ─────────────────────────────────────────────────────
    private var retryJob: Job? = null
    private var retryCount = 0
    private val maxRetry = 5
    private var isIntentionallyStopped = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initPlayer()
        initMediaSession()
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_STOP -> {
                isIntentionallyStopped = true; stopAndRelease()
            }

            ACTION_NEXT -> playNextStation()
            ACTION_PREV -> playPreviousStation()
            ACTION_TIMER_STOP -> fadeThenStop()
            ACTION_TIMER_MUTE -> muteForTimer()
            ACTION_FADE_VOLUME -> beginFade(20_000L, stopWhenDone = false)
            ACTION_RESTORE_VOLUME -> restoreVolume()
            ACTION_TIMER_CANCEL -> SleepTimerManager.cancel()
            ACTION_TIMER_PLUS_15 -> SleepTimerManager.extend(15 * 60 * 1000L)
            ACTION_REFRESH_NOTIF -> {
                if (currentStation != null) updateNotification()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        retryJob?.cancel()
        artLoadJob?.cancel()
        fadeJob?.cancel()
        stopRecording()
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    // ── MediaSession (Media3) — kilit ekranı + Bluetooth ─────────────────────

    private fun initMediaSession() {
        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun seekToNext() {
                playNextStation()
            }

            override fun seekToPrevious() {
                playPreviousStation()
            }

            override fun seekToNextMediaItem() {
                playNextStation()
            }

            override fun seekToPreviousMediaItem() {
                playPreviousStation()
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                if (mediaItemIndex != currentMediaItemIndex) {
                    if (mediaItemIndex > currentMediaItemIndex) playNextStation()
                    else playPreviousStation()
                } else {
                    super.seekTo(mediaItemIndex, positionMs)
                }
            }

            override fun hasNextMediaItem(): Boolean =
                AppSettings(this@RadioPlayerService).lockScreenControls

            override fun hasPreviousMediaItem(): Boolean =
                AppSettings(this@RadioPlayerService).lockScreenControls

            override fun isCommandAvailable(command: Int): Boolean {
                val lock = AppSettings(this@RadioPlayerService).lockScreenControls
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> lock
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                val builder = super.getAvailableCommands().buildUpon()
                if (AppSettings(this@RadioPlayerService).lockScreenControls) {
                    builder
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                } else {
                    builder
                        .remove(Player.COMMAND_SEEK_TO_NEXT)
                        .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                }
                return builder.build()
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivity)
            .build()
    }

    private fun updateMediaMetadata(station: RadioStation, art: Bitmap? = null) {
        val metaBuilder = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.country)
            .setGenre(station.tags)
            .setIsBrowsable(false)
            .setIsPlayable(true)
        if (art != null) {
            metaBuilder.setArtworkData(
                bitmapToBytes(art),
                MediaMetadata.PICTURE_TYPE_FRONT_COVER
            )
        }
        val item = player.currentMediaItem?.buildUpon()
            ?.setMediaMetadata(metaBuilder.build())
            ?.build()
        if (item != null) player.replaceMediaItem(0, item)
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }

    // ── Çalma listesi (kilit ekranı next/prev) ────────────────────────────────

    fun getPlaylist(): List<RadioStation> = playlist.toList()

    fun playNextStation() = skipBy(1)

    fun playPreviousStation() = skipBy(-1)

    private fun skipBy(delta: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSkipElapsed < 350L) return
        lastSkipElapsed = now

        currentStation?.let { current ->
            if (playlist.none { it.id == current.id }) {
                playlist.add(current)
                currentIndex = playlist.lastIndex
            }
        }
        if (playlist.size < 2) {
            Log.w(TAG, "Skip ignored: playlist size=${playlist.size}")
            return
        }
        currentIndex = Math.floorMod(currentIndex + delta, playlist.size)
        playStationInternal(playlist[currentIndex], updateQueue = false)
    }

    // ── ExoPlayer ─────────────────────────────────────────────────────────────

    private fun initPlayer() {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 1_500, 2_500)
            .setBackBuffer(60_000, true)
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(15_000)
            .setSeekForwardIncrementMs(15_000)
            .build()

        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {
                val st = currentStation ?: return
                when (state) {
                    Player.STATE_BUFFERING -> {
                        playerState.postValue(PlayerState.Buffering)
                    }

                    Player.STATE_READY -> {
                        retryCount = 0
                        retryJob?.cancel()
                        if (player.isPlaying) {
                            playerState.postValue(PlayerState.Playing(st))
                        } else {
                            playerState.postValue(PlayerState.Paused(st))
                        }
                    }

                    Player.STATE_ENDED -> {
                        if (!isIntentionallyStopped) scheduleRetry()
                    }

                    Player.STATE_IDLE -> {
                        if (!isIntentionallyStopped) {
                            playerState.postValue(PlayerState.Paused(st))
                        }
                    }
                }
                updateNotification()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val st = currentStation ?: return
                playerState.postValue(
                    if (isPlaying) PlayerState.Playing(st) else PlayerState.Paused(st)
                )
                updateNotification()
                RadioWidget.notifyWidgetUpdate(applicationContext, st.name, isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error [${error.errorCode}]: ${error.message}")
                val st = currentStation ?: return

                if (!isIntentionallyStopped) {
                    playerState.postValue(PlayerState.Buffering)
                    scheduleRetry()
                } else {
                    val friendlyMsg = when {
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            getString(R.string.error_connection)

                        error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                            getString(R.string.error_stream_unavailable)

                        else -> getString(R.string.error_connection)
                    }
                    playerState.postValue(PlayerState.Error(friendlyMsg))
                }
                updateNotification()
            }
        })
    }

    // ── Otomatik Yeniden Bağlanma ─────────────────────────────────────────────

    private fun scheduleRetry() {
        retryJob?.cancel()

        if (retryCount >= maxRetry) {
            Log.w(TAG, "Maksimum yeniden deneme aşıldı ($maxRetry)")
            currentStation?.let {
                playerState.postValue(PlayerState.Error(getString(R.string.error_connection)))
            }
            retryCount = 0
            return
        }

        val delayMs = (2000L * Math.pow(2.0, retryCount.toDouble())).toLong()
            .coerceAtMost(32_000L)

        retryCount++
        Log.d(TAG, "Yeniden bağlanılıyor... Deneme $retryCount/$maxRetry (${delayMs}ms sonra)")

        currentStation?.let {
            playerState.postValue(PlayerState.Reconnecting(it, retryCount))
        }

        retryJob = serviceScope.launch {
            delay(delayMs)
            val st = currentStation ?: return@launch
            Log.d(TAG, "Yeniden bağlanma başladı: ${st.name}")

            player.stop()
            player.clearMediaItems()
            player.setMediaItem(buildMediaItem(st))
            player.prepare()
            player.playWhenReady = true
        }
    }

    // ── Oynatma Komutları ─────────────────────────────────────────────────────

    fun playStation(station: RadioStation, queue: List<RadioStation>? = null) {
        playStationInternal(station, queue = queue, updateQueue = true)
    }

    private fun playStationInternal(
        station: RadioStation,
        queue: List<RadioStation>? = null,
        updateQueue: Boolean = true
    ) {
        if (updateQueue) {
            if (queue != null && queue.isNotEmpty()) {
                playlist = queue.toMutableList()
            }
            val idx = playlist.indexOfFirst { it.id == station.id }
            if (idx >= 0) {
                currentIndex = idx
            } else {
                playlist.add(station)
                currentIndex = playlist.lastIndex
            }
        }

        if (currentStation?.id != station.id) {
            stopRecording()
        }

        isIntentionallyStopped = false
        retryJob?.cancel()
        retryCount = 0
        artLoadJob?.cancel()

        currentStation = station
        currentAlbumArt = null
        sessionStartedAt = SystemClock.elapsedRealtime()
        restoreVolume()
        AppSettings(this).applyDefaultVolumeIfEnabled()
        AppSettings(this).saveLastStation(station)
        playerState.postValue(PlayerState.Buffering)

        player.stop()
        player.clearMediaItems()
        player.setMediaItem(buildMediaItem(station))
        player.prepare()
        player.playWhenReady = true

        if (station.hasValidFavicon()) {
            artLoadJob = serviceScope.launch {
                val bmp = withContext(Dispatchers.IO) { loadAlbumArt(station.favicon) }
                if (bmp != null && currentStation?.id == station.id) {
                    currentAlbumArt = bmp
                    updateMediaMetadata(station, bmp)
                    updateNotification(bmp)
                }
            }
        }

        updateMediaMetadata(station)
        startForeground(NOTIF_ID, buildNotification(currentAlbumArt))
        RadioWidget.notifyWidgetUpdate(applicationContext, station.name, true)
    }

    private fun loadAlbumArt(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
                instanceFollowRedirects = true
            }
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Album art failed: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun buildMediaItem(station: RadioStation): MediaItem {
        return MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.getStreamUrl())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.country)
                    .setGenre(station.tags)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else resume()
    }

    fun pause() {
        isIntentionallyStopped = false
        retryJob?.cancel()
        player.pause()
    }

    fun resume() {
        isIntentionallyStopped = false
        if (player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_ENDED
        ) {
            currentStation?.let { playStationInternal(it, updateQueue = false) }
        } else {
            player.play()
        }
    }

    fun stopAndRelease() {
        isIntentionallyStopped = true
        retryJob?.cancel()
        artLoadJob?.cancel()
        fadeJob?.cancel()
        stopRecording()
        retryCount = 0
        player.stop()
        currentStation = null
        currentAlbumArt = null
        playerState.postValue(PlayerState.Idle)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun isPlaying(): Boolean = ::player.isInitialized && player.isPlaying

    fun restoreVolume() {
        fadeJob?.cancel()
        targetVolume = 1f
        if (::player.isInitialized) player.volume = 1f
    }

    fun muteForTimer() {
        fadeJob?.cancel()
        if (::player.isInitialized) {
            player.volume = 0f
            pause()
        }
        updateNotification()
    }

    fun fadeThenStop() {
        beginFade(1_500L, stopWhenDone = true)
    }

    fun beginFade(durationMs: Long, stopWhenDone: Boolean) {
        if (!::player.isInitialized) {
            if (stopWhenDone) stopAndRelease()
            return
        }
        fadeJob?.cancel()
        val startVol = player.volume.coerceIn(0.05f, 1f)
        val steps = 20
        val stepMs = (durationMs / steps).coerceAtLeast(50L)
        fadeJob = serviceScope.launch {
            for (i in 1..steps) {
                player.volume = startVol * (1f - i / steps.toFloat())
                delay(stepMs)
            }
            player.volume = 0f
            if (stopWhenDone) stopAndRelease()
        }
    }

    fun updateFavorite(stationId: String, isFav: Boolean) {
        currentStation?.takeIf { it.id == stationId }?.isFavorite = isFav
        val idx = playlist.indexOfFirst { it.id == stationId }
        if (idx >= 0) playlist[idx] = playlist[idx].copy(isFavorite = isFav)
    }

    fun sessionElapsedMs(): Long {
        if (sessionStartedAt == 0L) return 0L
        return SystemClock.elapsedRealtime() - sessionStartedAt
    }

    fun playbackPositionMs(): Long {
        if (!::player.isInitialized) return 0L
        val p = player.currentPosition
        return if (p == C.TIME_UNSET || p < 0L) 0L else p
    }

    fun playbackDurationMs(): Long {
        if (!::player.isInitialized) return 0L
        val d = player.duration
        return if (d == C.TIME_UNSET || d <= 0L) 0L else d
    }

    fun isPlaybackSeekable(): Boolean {
        if (!::player.isInitialized) return false
        if (player.isCurrentMediaItemSeekable && playbackDurationMs() > 0L) return true
        return player.isCurrentMediaItemLive || playbackPositionMs() > 0L
    }

    fun isLiveStream(): Boolean =
        ::player.isInitialized && player.isCurrentMediaItemLive

    fun rewind(deltaMs: Long = 15_000L): Boolean {
        if (!::player.isInitialized) return false
        return try {
            val pos = player.currentPosition
            if (pos != C.TIME_UNSET && pos > 0L) {
                val minPos = if (player.isCurrentMediaItemLive) {
                    (pos - LIVE_REWIND_WINDOW_MS).coerceAtLeast(0L)
                } else {
                    0L
                }
                player.seekTo((pos - deltaMs).coerceAtLeast(minPos))
                true
            } else {
                player.seekBack()
                true
            }
        } catch (_: Exception) {
            try {
                player.seekBack()
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun seekToMs(positionMs: Long): Boolean {
        if (!::player.isInitialized) return false
        return try {
            player.seekTo(positionMs.coerceAtLeast(0L))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isRecording(): Boolean = recorder?.isRunning() == true

    fun recordingElapsedMs(): Long {
        val rec = recorder ?: return 0L
        if (!rec.isRunning() && rec.startedAtElapsed == 0L) return 0L
        return SystemClock.elapsedRealtime() - rec.startedAtElapsed
    }

    fun toggleRecording() {
        if (isRecording()) stopRecording() else startRecording()
    }

    fun startRecording() {
        val st = currentStation ?: return
        if (recorder?.isRunning() == true) return
        val rec = StreamRecorder(applicationContext)
        recorder = rec
        rec.start(st, object : StreamRecorder.Listener {
            override fun onStarted() {
                recordingState.postValue(true)
            }

            override fun onSaved(displayName: String) {
                if (recorder === rec) recorder = null
                recordingState.postValue(false)
                recordingEvent.postValue(true to displayName)
            }

            override fun onError(message: String) {
                if (recorder === rec) recorder = null
                recordingState.postValue(false)
                recordingEvent.postValue(false to message)
            }
        })
        if (rec.isRunning()) {
            recordingState.postValue(true)
        } else if (recorder === rec) {
            recorder = null
            recordingState.postValue(false)
        }
    }

    fun stopRecording() {
        recorder?.stop()
    }

    /** Equalizer için audioSessionId */
    fun getAudioSessionId(): Int =
        if (::player.isInitialized) player.audioSessionId else 0

    // ── Bildirim ──────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
    }

    private fun pi(reqCode: Int, action: String): PendingIntent =
        PendingIntent.getService(
            this, reqCode,
            Intent(this, RadioPlayerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildNotification(art: Bitmap? = null): Notification {
        val station = currentStation
        val isPlaying = player.isPlaying
        val isRetrying = retryCount > 0 && !isIntentionallyStopped

        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = when {
            SleepTimerManager.isActive.value == true ->
                getString(
                    R.string.timer_remaining,
                    SleepTimerManager.formatRemaining(SleepTimerManager.remainingMs.value ?: 0L)
                )
            isRetrying -> getString(R.string.notif_reconnecting)
            player.playbackState == Player.STATE_BUFFERING -> getString(R.string.notif_loading)
            isPlaying -> getString(R.string.notif_live)
            else -> getString(R.string.notif_paused)
        }

        val lock = AppSettings(this).lockScreenControls
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_radio_notif)
            .setContentTitle(station?.name ?: getString(R.string.app_name))
            .setContentText(contentText)
            .setContentIntent(openPi)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying || isRetrying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (art != null) builder.setLargeIcon(art)

        if (lock) {
            builder.addAction(
                R.drawable.ic_skip_previous,
                getString(R.string.previous),
                pi(3, ACTION_PREV)
            )
        }
        builder.addAction(
            if (isPlaying) R.drawable.ic_pause_notif else R.drawable.ic_play_notif,
            if (isPlaying) getString(R.string.pause) else getString(R.string.play),
            pi(1, ACTION_PLAY_PAUSE)
        )
        if (lock) {
            builder.addAction(R.drawable.ic_skip_next, getString(R.string.next), pi(4, ACTION_NEXT))
        }
        builder.addAction(R.drawable.ic_stop_notif, getString(R.string.stop), pi(2, ACTION_STOP))
        if (SleepTimerManager.isActive.value == true) {
            builder.addAction(
                R.drawable.ic_sleep_timer,
                getString(R.string.timer_plus_15),
                pi(5, ACTION_TIMER_PLUS_15)
            )
            builder.addAction(
                R.drawable.ic_close,
                getString(R.string.timer_cancel),
                pi(6, ACTION_TIMER_CANCEL)
            )
        }
        if (lock) {
            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionCompatToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }
        return builder.build()
    }

    private fun updateNotification(art: Bitmap? = currentAlbumArt) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification(art))
    }

    companion object {
        private const val TAG = "RadioPlayerService"
        const val CHANNEL_ID = "radio_playback_channel"
        const val NOTIF_ID = 1001
        const val ACTION_PLAY_PAUSE = "com.globalradio.livetuneinogzapp.ACTION_PLAY_PAUSE"
        const val ACTION_STOP = "com.globalradio.livetuneinogzapp.ACTION_STOP"
        const val ACTION_NEXT = "com.globalradio.livetuneinogzapp.ACTION_NEXT"
        const val ACTION_PREV = "com.globalradio.livetuneinogzapp.ACTION_PREV"
        const val ACTION_TIMER_STOP = "com.globalradio.livetuneinogzapp.ACTION_TIMER_STOP"
        const val ACTION_TIMER_MUTE = "com.globalradio.livetuneinogzapp.ACTION_TIMER_MUTE"
        const val ACTION_FADE_VOLUME = "com.globalradio.livetuneinogzapp.ACTION_FADE_VOLUME"
        const val ACTION_RESTORE_VOLUME = "com.globalradio.livetuneinogzapp.ACTION_RESTORE_VOLUME"
        const val ACTION_TIMER_CANCEL = "com.globalradio.livetuneinogzapp.ACTION_TIMER_CANCEL"
        const val ACTION_TIMER_PLUS_15 = "com.globalradio.livetuneinogzapp.ACTION_TIMER_PLUS_15"
        const val ACTION_REFRESH_NOTIF = "com.globalradio.livetuneinogzapp.ACTION_REFRESH_NOTIF"
        const val BROADCAST_NEXT = "com.globalradio.livetuneinogzapp.NEXT_STATION"
        const val BROADCAST_PREV = "com.globalradio.livetuneinogzapp.PREV_STATION"
        const val LIVE_REWIND_WINDOW_MS = 60_000L
    }
}
