package com.globalradio.livetuneinogzapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.globalradio.livetuneinogzapp.adapter.StationAdapter
import com.globalradio.livetuneinogzapp.ads.AdManager
import com.globalradio.livetuneinogzapp.databinding.ActivityCountryStationsBinding
import com.globalradio.livetuneinogzapp.model.PlayerState
import com.globalradio.livetuneinogzapp.model.RadioStation
import com.globalradio.livetuneinogzapp.service.RadioPlayerService
import com.globalradio.livetuneinogzapp.utils.FavoritesManager
import com.globalradio.livetuneinogzapp.utils.SleepTimerManager
import com.globalradio.livetuneinogzapp.viewmodel.CountryStationsViewModel

@UnstableApi
class CountryStationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCountryStationsBinding
    private lateinit var viewModel: CountryStationsViewModel
    private lateinit var adapter: StationAdapter
    private lateinit var favoritesManager: FavoritesManager

    private var radioService: RadioPlayerService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            radioService = (binder as RadioPlayerService.RadioBinder).getService()
            isServiceBound = true
            radioService?.playerState?.observe(this@CountryStationsActivity) { state ->
                val playingId = when (state) {
                    is PlayerState.Playing -> state.station.id
                    is PlayerState.Buffering -> radioService?.currentStation?.id
                    is PlayerState.Paused -> state.station.id
                    is PlayerState.Reconnecting -> state.station.id
                    else -> null
                }
                adapter.updatePlayingStation(playingId)
                updateMiniPlayerState(state)
            }
            val sid = radioService?.getAudioSessionId() ?: 0
            if (sid != 0) EqualizerManager.init(this@CountryStationsActivity, sid)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isServiceBound = false
            radioService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryStationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val country = intent.getStringExtra(EXTRA_COUNTRY) ?: ""
        val countryCode = intent.getStringExtra(EXTRA_COUNTRY_CODE)
        favoritesManager = FavoritesManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = country
            setDisplayHomeAsUpEnabled(true)
        }

        viewModel = ViewModelProvider(this)[CountryStationsViewModel::class.java]

        adapter = StationAdapter(
            onStationClick = { station -> onStationSelected(station) },
            onFavoriteClick = { station ->
                val isNowFav = favoritesManager.toggleFavorite(station.id)
                val updated = viewModel.stations.value?.map { s ->
                    if (s.id == station.id) s.copy(isFavorite = isNowFav) else s
                } ?: emptyList()
                viewModel.updateStations(updated)
            }
        )

        binding.recyclerCountryStations.apply {
            layoutManager = LinearLayoutManager(this@CountryStationsActivity)
            adapter = this@CountryStationsActivity.adapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }

        binding.tvEmptyCountryStations.setOnClickListener {
            viewModel.loadStationsForCountry(country, countryCode)
        }

        observeViewModel()
        viewModel.loadStationsForCountry(country, countryCode)
        setupMiniPlayer()

        val serviceIntent = Intent(this, RadioPlayerService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // ── Ses tuşları (sistem panelini göstermeden) ─────────────────────────────

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (radioService?.isPlaying() == true) {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            return when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        am.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            0
                        )
                    }
                    true
                }

                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        am.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            0
                        )
                    }
                    true
                }

                else -> super.dispatchKeyEvent(event)
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ── ViewModel gözlemcileri ────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.stations.observe(this) { stations ->
            adapter.submitList(stations)
            binding.progressCountry.visibility = View.GONE
            binding.tvEmptyCountryStations.visibility =
                if (stations.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressCountry.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) binding.tvEmptyCountryStations.visibility = View.GONE
        }

        viewModel.error.observe(this) { err ->
            if (!err.isNullOrEmpty()) {
                binding.tvEmptyCountryStations.visibility = View.VISIBLE
                binding.tvEmptyCountryStations.text = getString(R.string.stream_failed)
            }
        }
    }

    // ── İstasyon seçimi ──────────────────────────────────────────────────────

    private fun onStationSelected(station: RadioStation) {
        val service = radioService ?: run {
            Toast.makeText(this, getString(R.string.service_starting), Toast.LENGTH_SHORT).show()
            return
        }
        service.playStation(station, viewModel.stations.value)
        AdManager.onStationClicked(this)
    }

    fun playStation(station: RadioStation) {
        radioService?.playStation(station, viewModel.stations.value)
    }

    // ── Neon pulse animasyonu ─────────────────────────────────────────────────

    private var neonAnimator: android.animation.ValueAnimator? = null

    private fun startNeonPulse() {
        val neon = binding.neonBorder
        if (neonAnimator?.isRunning == true) return

        neon.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

        // ═══════════════════════════════════════════════════════════════════
        //  NEON RENK PALETİ — 5 renk arasında sürekli geçiş
        //  Alfa değerleri 0x99–0xFF arasında tutuldu (asla tamamen kaybolmaz,
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

    // ── Mini Player ──────────────────────────────────────────────────────────

    private fun setupMiniPlayer() {
        AdManager.loadBanner(this, binding.bannerContainer)

        binding.miniPlayer.setOnClickListener {
            radioService?.currentStation?.let { st ->
                val existing = supportFragmentManager.findFragmentByTag("PlayerBS")
                if (existing == null)
                    PlayerBottomSheet.newInstance(st).show(supportFragmentManager, "PlayerBS")
            }
        }
        binding.btnMiniPlayPause.setOnClickListener { radioService?.togglePlayPause() }
        binding.btnMiniEqualizer.setOnClickListener {
            EqualizerBottomSheet.newInstance().show(supportFragmentManager, "Equalizer")
        }
        binding.btnMiniSleepTimer.setOnClickListener {
            SleepTimerDialog.newInstance().show(supportFragmentManager, "SleepTimer")
        }

        // Sleep timer geri sayım gözlemcisi
        SleepTimerManager.remainingMs.observe(this) { ms ->
            val timerActive = ms > 0
            binding.btnMiniSleepTimer.setColorFilter(
                if (timerActive) getColor(R.color.timer_active)
                else getColor(R.color.btn_secondary_icon)
            )
            binding.sleepTimerBar.visibility = if (timerActive) View.VISIBLE else View.GONE
            binding.tvSleepTimerRemaining.visibility = if (timerActive) View.VISIBLE else View.GONE
            if (timerActive) {
                binding.tvSleepTimerRemaining.text =
                    getString(R.string.timer_remaining, SleepTimerManager.formatRemaining(ms))
            }
        }
    }

    private fun updateMiniPlayerState(state: PlayerState) {
        when (state) {
            is PlayerState.Playing -> showMiniPlayer(state.station, true)
            is PlayerState.Paused -> showMiniPlayer(state.station, false)
            is PlayerState.Buffering -> radioService?.currentStation?.let {
                showMiniPlayer(it, false, isBuffering = true)
            }

            is PlayerState.Reconnecting -> showMiniPlayer(
                state.station, false, isReconnecting = true, attempt = state.attempt
            )

            is PlayerState.Error -> {
                Toast.makeText(
                    this,
                    getString(R.string.error_with_message, state.message),
                    Toast.LENGTH_SHORT
                ).show()
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
        binding.tvMiniStationName.isSelected = true  // Marquee başlat
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
            Glide.with(this).load(station.favicon)
                .placeholder(R.drawable.ic_radio_placeholder)
                .error(R.drawable.ic_radio_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(binding.ivMiniLogo)
        } else {
            binding.ivMiniLogo.setImageResource(R.drawable.ic_radio_placeholder)
        }
    }

    private fun showPlayerBottomSheet(station: RadioStation) {
        val existing = supportFragmentManager.findFragmentByTag("PlayerBS")
        if (existing != null) return
        PlayerBottomSheet.newInstance(station).show(supportFragmentManager, "PlayerBS")
    }

    // ── Public erişim (PlayerBottomSheet için) ────────────────────────────────

    fun getRadioService(): RadioPlayerService? = radioService

    fun toggleFavoriteForStation(station: RadioStation) {
        val isNowFav = favoritesManager.toggleFavorite(station.id)
        val updated = viewModel.stations.value?.map { s ->
            if (s.id == station.id) s.copy(isFavorite = isNowFav) else s
        } ?: emptyList()
        viewModel.updateStations(updated)
        adapter.submitList(updated)
    }

    fun getStationList(): List<RadioStation> = viewModel.stations.value ?: emptyList()

    fun getNextStation(current: RadioStation): RadioStation? {
        val list = viewModel.stations.value ?: return null
        val idx = list.indexOfFirst { it.id == current.id }
        return if (idx >= 0 && idx < list.size - 1) list[idx + 1] else list.firstOrNull()
    }

    fun getPreviousStation(current: RadioStation): RadioStation? {
        val list = viewModel.stations.value ?: return null
        val idx = list.indexOfFirst { it.id == current.id }
        return if (idx > 0) list[idx - 1] else list.lastOrNull()
    }

    fun getCurrentStationIndex(current: RadioStation): Int =
        viewModel.stations.value?.indexOfFirst { it.id == current.id } ?: -1

    fun getTotalCount(): Int = viewModel.stations.value?.size ?: 0

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish(); return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        stopNeonPulse()
        if (isServiceBound) {
            unbindService(serviceConnection); isServiceBound = false
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_COUNTRY = "extra_country"
        const val EXTRA_COUNTRY_CODE = "extra_country_code"
    }
}