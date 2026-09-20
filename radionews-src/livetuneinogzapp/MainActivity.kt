package com.globalradio.livetuneinogzapp

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.globalradio.livetuneinogzapp.ads.AdManager
import com.globalradio.livetuneinogzapp.cast.CastManager
import com.globalradio.livetuneinogzapp.databinding.ActivityMainBinding
import com.globalradio.livetuneinogzapp.model.PlayerState
import com.globalradio.livetuneinogzapp.model.RadioStation
import androidx.media3.common.util.UnstableApi
import com.globalradio.livetuneinogzapp.service.RadioPlayerService
import com.globalradio.livetuneinogzapp.utils.AppSettings
import com.globalradio.livetuneinogzapp.utils.SleepTimerManager
import com.globalradio.livetuneinogzapp.utils.StationImages
import com.globalradio.livetuneinogzapp.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var _viewModel: MainViewModel

    private var radioService: RadioPlayerService? = null
    private var isServiceBound = false
    private var resumedLastStation = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            radioService = (binder as RadioPlayerService.RadioBinder).getService()
            isServiceBound = true
            radioService?.playerState?.observe(this@MainActivity) { state ->
                _viewModel.updatePlayerState(state)
            }
            val sid = radioService?.getAudioSessionId() ?: 0
            if (sid != 0) EqualizerManager.init(this@MainActivity, sid)
            maybeResumeLastStation()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isServiceBound = false; radioService = null
        }
    }

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // ────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tema ayarını uygula
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        _viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        requestNotifPermission()
        setupMiniPlayer()
        observeViewModel()

        AdManager.initialize(this) {
            AdManager.loadBanner(this, binding.bannerContainer)
            AdManager.preloadInterstitial(this)
        }
        // preload artık initialize callback içinde
        startAndBindService()

        // "Diğer cihazlarla Çal" (Google Cast) — bkz. CastOptionsProvider.kt
        // için gerekli build.gradle/AndroidManifest.xml kurulum notları.
        CastManager.init(
            activity = this,
            getCurrentStation = { radioService?.currentStation },
            onCastSessionStarted = { radioService?.pause() }
        )

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment(), TAG_HOME)
                .commit()
        }
    }

    // ── Ses tuşları ───────────────────────────────────────────────────────────

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Radyo çalıyorsa ses tuşları sadece uygulama içi slider'ı değiştirsin
        // Sistem ses panelini gösterme (FLAG=0)
        if (radioService?.isPlaying() == true) {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            return when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        am.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            0 // Sistem panelini gösterme
                        )
                    }
                    true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        am.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            0 // Sistem panelini gösterme
                        )
                    }
                    true
                }

                else -> super.dispatchKeyEvent(event)
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ── Mini Player ───────────────────────────────────────────────────────────

    private var neonAnimator: android.animation.ValueAnimator? = null

    private fun startNeonPulse() {
        val neon = binding.neonBorder
        // Zaten çalışıyorsa yeniden başlatma
        if (neonAnimator?.isRunning == true) return

        neon.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

        // ═══════════════════════════════════════════════════════════════════
        //  NEON RENK PALETİ — 5 renk arasında sürekli geçiş
        //  Alfa değerleri 0xAA–0xFF arasında tutuldu (asla tamamen kaybolmaz,
        //  sadece hafif solup parlar).
        // ═══════════════════════════════════════════════════════════════════
        val neonColors = intArrayOf(
            android.graphics.Color.parseColor("#FF00E5FF"),  // Turkuaz (parlak)
            android.graphics.Color.parseColor("#AA00E5FF"),  // Turkuaz (sönük) → nefes efekti
            android.graphics.Color.parseColor("#FFE91E63"),  // Pembe (parlak)
            android.graphics.Color.parseColor("#AAE91E63"),  // Pembe (sönük)
            android.graphics.Color.parseColor("#FF9D00FF"),  // Mor (parlak)
            android.graphics.Color.parseColor("#AA9D00FF"),  // Mor (sönük)
            android.graphics.Color.parseColor("#FF00FF85"),  // Yeşil (parlak)
            android.graphics.Color.parseColor("#AA00FF85"),  // Yeşil (sönük)
            android.graphics.Color.parseColor("#FFFFB400"),  // Turuncu (parlak)
            android.graphics.Color.parseColor("#AAFFB400"),  // Turuncu (sönük)
            android.graphics.Color.parseColor("#FF00E5FF")   // Turkuaza geri dön (döngü pürüzsüz)
        )

        neonAnimator = android.animation.ValueAnimator.ofArgb(*neonColors).apply {
            duration = 8000    // 5 renk × ~1.6sn/renk — yavaş, akıcı geçiş
            repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.RESTART   // Döngü başa sar
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()

            addUpdateListener { anim ->
                val color = anim.animatedValue as Int
                val drawable = neon.background
                if (drawable is android.graphics.drawable.GradientDrawable) {
                    (drawable.mutate() as android.graphics.drawable.GradientDrawable)
                        .setStroke(
                            (1f * resources.displayMetrics.density).toInt(),
                            color
                        )
                }
                val glowPaint = android.graphics.Paint().apply {
                    setShadowLayer(20f, 0f, 0f, color)
                }
                neon.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, glowPaint)
                neon.invalidate()
            }
        }
        neonAnimator?.start()
    }

    private fun stopNeonPulse() {
        neonAnimator?.cancel()
        neonAnimator = null
        binding.neonBorder.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
    }

    private fun setupMiniPlayer() {
        binding.miniPlayer.setOnClickListener {
            radioService?.currentStation?.let { showPlayerSheet(it) }
        }
        binding.btnMiniPlayPause.setOnClickListener { radioService?.togglePlayPause() }
        binding.btnMiniEqualizer.setOnClickListener {
            EqualizerBottomSheet.newInstance().show(supportFragmentManager, "Equalizer")
        }
        binding.btnMiniSleepTimer.setOnClickListener {
            SleepTimerDialog.newInstance().show(supportFragmentManager, "SleepTimer")
        }
    }

    private fun observeViewModel() {
        _viewModel.error.observe(this) { err ->
            if (!err.isNullOrEmpty()) Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        }
        _viewModel.playerState.observe(this) { state ->
            updateMiniPlayer(state)
            val sid = radioService?.getAudioSessionId() ?: 0
            if (sid != 0) EqualizerManager.init(this, sid)
        }
        _viewModel.favoritePayload.observe(this) { payload ->
            payload ?: return@observe
            radioService?.updateFavorite(payload.first, payload.second)
        }
        SleepTimerManager.remainingMs.observe(this) { ms ->
            // Ay ikonu tint rengi - aktifse kırmızı
            val timerActive = ms > 0
            binding.btnMiniSleepTimer.setColorFilter(
                if (timerActive) getColor(R.color.timer_active)
                else getColor(R.color.btn_secondary_icon)
            )
            // Geri sayım göster/gizle
            binding.sleepTimerBar.visibility = if (timerActive) View.VISIBLE else View.GONE
            binding.tvSleepTimerRemaining.visibility = if (timerActive) View.VISIBLE else View.GONE
            if (timerActive) {
                binding.tvSleepTimerRemaining.text =
                    getString(R.string.timer_remaining, SleepTimerManager.formatRemaining(ms))
            }
        }
    }

    private fun updateMiniPlayer(state: PlayerState) {
        when (state) {
            is PlayerState.Playing -> showMiniPlayer(state.station, true)
            is PlayerState.Paused -> showMiniPlayer(state.station, false)
            is PlayerState.Buffering -> radioService?.currentStation?.let {
                showMiniPlayer(
                    it,
                    false,
                    isBuffering = true
                )
            }

            is PlayerState.Reconnecting -> showMiniPlayer(
                state.station,
                false,
                isReconnecting = true,
                attempt = state.attempt
            )

            is PlayerState.Error -> {
                Toast.makeText(
                    this,
                    getString(R.string.error_with_message, state.message),
                    Toast.LENGTH_SHORT
                )
                    .show()
                stopNeonPulse()
                binding.miniPlayerWrapper.visibility = View.GONE
            }

            is PlayerState.Idle -> {
                stopNeonPulse()
                binding.miniPlayerWrapper.visibility = View.GONE
            }
        }
    }

    private fun showMiniPlayer(
        station: RadioStation,
        isPlaying: Boolean,
        isBuffering: Boolean = false,
        isReconnecting: Boolean = false,
        attempt: Int = 0
    ) {
        binding.miniPlayerWrapper.visibility = View.VISIBLE
        startNeonPulse()
        binding.tvMiniStationName.text = station.name
        binding.tvMiniStationName.isSelected = true
        binding.tvMiniStatus.text = when {
            isReconnecting -> getString(R.string.status_reconnecting, attempt)
            isBuffering -> getString(R.string.status_buffering)
            isPlaying -> getString(R.string.status_live)
            else -> getString(R.string.status_paused)
        }
        binding.btnMiniPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
        )
        binding.miniBuffering.visibility =
            if (isBuffering || isReconnecting) View.VISIBLE else View.GONE
        if (station.hasValidFavicon()) {
            StationImages.loadLogo(binding.ivMiniLogo, station.favicon, circle = true)
        } else {
            binding.ivMiniLogo.setImageResource(R.drawable.ic_radio_placeholder)
        }
    }

    // ── İstasyon çalma ────────────────────────────────────────────────────────

    fun playStation(station: RadioStation) {
        val service = radioService ?: run {
            Toast.makeText(this, getString(R.string.service_starting), Toast.LENGTH_SHORT)
                .show(); return
        }
        val queue = _viewModel.playlistFor(station)
        station.isFavorite = _viewModel.isFavorite(station.id)
        if (CastManager.isCasting()) {
            // Bir Cast cihazına bağlıyken istasyonu oraya gönder, telefonda
            // aynı anda ikinci bir ses kaynağı çalmasın diye yereli durdur.
            CastManager.castStation(station)
            service.pause()
        } else {
            service.playStation(station, queue)
        }
        _viewModel.recordPlay(station)
        AdManager.onStationClicked(this)
        // Büyük player açma - mini player'a tıklayınca açılır
    }

    fun showPlayerSheet(station: RadioStation) {
        if (supportFragmentManager.findFragmentByTag("PlayerSheet") != null) return
        PlayerBottomSheet.newInstance(station).show(supportFragmentManager, "PlayerSheet")
    }

    fun getRadioService(): RadioPlayerService? = radioService
    fun getViewModel(): MainViewModel = _viewModel

    // ── Ayarlar sayfası ───────────────────────────────────────────────────────

    fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun maybeResumeLastStation() {
        if (resumedLastStation) return
        if (!AppSettings(this).resumeLastStation) return
        if (radioService?.currentStation != null) return
        val last = AppSettings(this).lastStation() ?: return
        resumedLastStation = true
        playStation(last)
    }

    // ── Service ───────────────────────────────────────────────────────────────

    private fun startAndBindService() {
        val intent = Intent(this, RadioPlayerService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onResume() {
        super.onResume()
        // Uygulama ön plana gelince boşsa yeniden yükle
        _viewModel.reloadIfEmpty()
    }

    override fun onDestroy() {
        stopNeonPulse()
        if (isServiceBound) {
            unbindService(serviceConnection); isServiceBound = false
        }
        super.onDestroy()
    }

    companion object {
        const val TAG_HOME = "frag_home"
    }
}