package com.globalradio.livetuneinogzapp

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.globalradio.livetuneinogzapp.adapter.LogoPagerAdapter
import com.globalradio.livetuneinogzapp.databinding.FragmentPlayerBottomSheetBinding
import com.globalradio.livetuneinogzapp.model.PlayerState
import com.globalradio.livetuneinogzapp.model.RadioStation
import androidx.media3.common.util.UnstableApi
import com.globalradio.livetuneinogzapp.service.RadioPlayerService
import com.globalradio.livetuneinogzapp.viewmodel.MainViewModel
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import kotlin.math.abs

@UnstableApi
class PlayerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentPlayerBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var currentStation: RadioStation? = null
    private lateinit var gestureDetector: GestureDetector

    private lateinit var logoPagerAdapter: LogoPagerAdapter
    private var isPagerScrolling = false

    private val mainActivity get() = activity as? MainActivity
    private val countryActivity get() = activity as? CountryStationsActivity

    private val radioService: RadioPlayerService?
        get() = mainActivity?.getRadioService() ?: countryActivity?.getRadioService()

    private val activeViewModel: MainViewModel?
        get() = mainActivity?.getViewModel()

    // Ses sync handler
    private val volumeHandler = Handler(Looper.getMainLooper())
    private val volumeRunnable = object : Runnable {
        override fun run() {
            if (!isAdded || _binding == null) return
            try {
                val am =
                    requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
                val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (binding.seekBarVolume.progress != cur) binding.seekBarVolume.progress = cur
            } catch (_: Exception) {
            }
            volumeHandler.postDelayed(this, 300)
        }
    }

    // ── Smart next/prev (servis çalma listesi öncelikli) ──

    private fun stationIndex(cur: RadioStation): Int {
        val queue = radioService?.getPlaylist().orEmpty()
        val qi = queue.indexOfFirst { it.id == cur.id }
        if (qi >= 0) return qi
        countryActivity?.let { return it.getCurrentStationIndex(cur) }
        return activeViewModel?.getCurrentStationIndex(cur) ?: -1
    }

    private fun stationTotal(): Int {
        val queue = radioService?.getPlaylist().orEmpty()
        if (queue.isNotEmpty()) return queue.size
        countryActivity?.let { return it.getTotalCount() }
        return activeViewModel?.getTotalCount() ?: 0
    }

    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenPlayerTheme)
        currentStation = arguments?.getParcelable(ARG_STATION)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tam ekran
        dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.let { sheet ->
            sheet.setBackgroundColor(Color.TRANSPARENT)
            BottomSheetBehavior.from(sheet).apply {
                peekHeight = resources.displayMetrics.heightPixels
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
            sheet.layoutParams.height = resources.displayMetrics.heightPixels
            sheet.requestLayout()
        }

        setupSwipeGesture()
        setupVolumeSlider()
        setupButtons()
        observePlayerState()
        loadBannerAd()

        // Başlangıç UI
        currentStation?.let { updateUI(it) }
    }

    // ── Logo ViewPager2 kurulumu ─────────────────────────────────────────────

    private fun setupSwipeGesture() {
        logoPagerAdapter = LogoPagerAdapter { station ->
            // Görünür olan istasyonu takip et
        }
        binding.logoPager.adapter = logoPagerAdapter
        binding.logoPager.offscreenPageLimit = 2

        // Peek efekti — yan logoların görünmesi için
        val pageMarginPx = resources.displayMetrics.density * 8
        binding.logoPager.setPageTransformer { page, position ->
            val absPos = kotlin.math.abs(position).coerceAtMost(1f)
            // Ortadaki tam büyük+parlak, kenardakiler %90 boyut %65 alpha
            page.scaleX = 1f - 0.10f * absPos
            page.scaleY = 1f - 0.10f * absPos
            page.alpha = 1f - 0.35f * absPos
        }
        binding.logoPager.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                androidx.recyclerview.widget.DividerItemDecoration.HORIZONTAL
            ).also {
                // sadece spacing için
            }
        )

        // Sayfa değişince radyoyu değiştir
        binding.logoPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val station = logoPagerAdapter.getStation(position) ?: return
                if (station.id != radioService?.currentStation?.id) {
                    isPagerScrolling = true
                    play(station)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    isPagerScrolling = false
                }
            }
        })
    }

    /** Pager'ı aktif bölümün tüm listesiyle güncelle */
    private fun updateLogoPager(currentSt: RadioStation) {
        if (isPagerScrolling) return
        val cur = radioService?.currentStation ?: currentSt

        // Hangi ekrandaysak o ekranın tüm listesini al
        val fullList: List<RadioStation> = when {
            radioService?.getPlaylist()?.isNotEmpty() == true -> radioService!!.getPlaylist()
            countryActivity != null -> countryActivity!!.getStationList()
            else -> {
                val active = activeViewModel?.getActiveSectionList() ?: emptyList()
                val fav = activeViewModel?.favoriteStations?.value ?: emptyList()
                if (active.any { it.id == cur.id }) active
                else if (fav.any { it.id == cur.id }) fav
                else activeViewModel?.allStationsList() ?: emptyList()
            }
        }

        val list = if (fullList.isNotEmpty() && fullList.any { it.id == cur.id }) fullList
        else if (fullList.isNotEmpty()) (listOf(cur) + fullList).distinctBy { it.id }
        else listOf(cur) // fallback

        // Listeyi sadece gerektiğinde güncelle
        val currentIdx = list.indexOfFirst { it.id == cur.id }
        if (logoPagerAdapter.indexOf(cur) < 0 || logoPagerAdapter.itemCount != list.size) {
            logoPagerAdapter.setStations(list)
        }
        val idx = logoPagerAdapter.indexOf(cur)
        if (idx >= 0) binding.logoPager.setCurrentItem(idx, false)
    }


    // ── Ses slider ────────────────────────────────────────────────────────────

    private fun setupVolumeSlider() {
        val am =
            requireContext().getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
        binding.seekBarVolume.max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.seekBarVolume.progress = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.seekBarVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        volumeHandler.post(volumeRunnable)
    }

    // ── Butonlar ─────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnPlayPause.setOnClickListener { radioService?.togglePlayPause() }
        binding.btnNext.setOnClickListener { radioService?.playNextStation() }
        binding.btnPrevious.setOnClickListener { radioService?.playPreviousStation() }

        binding.btnFavorite.setOnClickListener {
            val st = radioService?.currentStation ?: currentStation ?: return@setOnClickListener
            if (countryActivity != null) {
                countryActivity?.toggleFavoriteForStation(st)
            } else {
                activeViewModel?.toggleFavorite(st)
            }
            // Anlık UI güncellemesi
            val newFav = !st.isFavorite
            st.isFavorite = newFav
            setFavIcon(newFav)
        }

        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnShare.setOnClickListener {
            val st = radioService?.currentStation ?: currentStation ?: return@setOnClickListener
            val shareVia = getString(R.string.share_via)
            val txt = buildString {
                append("📻 ${st.name}\n")
                if (st.country.isNotBlank()) append("🌍 ${st.country}\n")
                if (st.getTagList().isNotEmpty())
                    append("🎵 ${st.getTagList().take(3).joinToString(", ")}\n")
                append("\n$shareVia")
            }
            startActivity(
                android.content.Intent.createChooser(
                    android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, txt)
                    }, getString(R.string.share)
                )
            )
        }

        binding.btnEqualizerPlayer.setOnClickListener {
            EqualizerBottomSheet.newInstance().show(parentFragmentManager, "Equalizer")
        }
    }

    // ── Oynat yardımcısı ─────────────────────────────────────────────────────

    private fun play(st: RadioStation) {
        val queue = radioService?.getPlaylist()?.takeIf { it.isNotEmpty() }
            ?: countryActivity?.getStationList()?.takeIf { it.isNotEmpty() }
            ?: activeViewModel?.playlistFor(st)
        radioService?.playStation(st, queue)
        activeViewModel?.recordPlay(st)
        currentStation = st
    }

    // ── State gözlemle ────────────────────────────────────────────────────────

    private fun observePlayerState() {
        // HER İKİ kaynağı dinle - hangisi önce gelirse
        // ViewModel state her zaman servisin state'ini yansıtır (MainActivity'de observe edilip set ediliyor)
        activeViewModel?.playerState?.observe(viewLifecycleOwner) { state ->
            applyState(state)
        }
        // Servis direkt (CountryStationsActivity ve güvenlik için)
        radioService?.playerState?.observe(viewLifecycleOwner) { state ->
            applyState(state)
        }
    }

    private fun applyState(state: PlayerState) {
        when (state) {
            is PlayerState.Playing -> {
                currentStation = state.station
                updateUI(state.station)
                setPlayIcon(true)
                setBusy(false)
                startEq()
                binding.tvStatus.text = getString(R.string.status_live)
            }

            is PlayerState.Paused -> {
                currentStation = state.station
                updateUI(state.station)
                setPlayIcon(false)
                setBusy(false)
                stopEq()
                binding.tvStatus.text = getString(R.string.status_paused)
            }

            is PlayerState.Buffering -> {
                setPlayIcon(false)
                setBusy(true)
                stopEq()
                binding.tvStatus.text = getString(R.string.status_buffering)
            }

            is PlayerState.Reconnecting -> {
                setPlayIcon(false)
                setBusy(true)
                stopEq()
                binding.tvStatus.text = getString(R.string.status_reconnecting, state.attempt)
            }

            is PlayerState.Error -> {
                setPlayIcon(false)
                setBusy(false)
                stopEq()
                binding.tvStatus.text = "⚠ ${state.message}"
            }

            is PlayerState.Idle -> {
                setPlayIcon(false)
                setBusy(false)
                stopEq()
            }
        }
        // Sıra bilgisi
        radioService?.currentStation?.let { cur ->
            val idx = stationIndex(cur)
            val tot = stationTotal()
            binding.tvStationIndex.text = if (idx >= 0) "${idx + 1} / $tot" else ""
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private fun updateUI(station: RadioStation) {
        if (_binding == null) return
        binding.tvStationName.text = station.name
        binding.tvStationName.isSelected = true
        binding.tvCountry.text = station.country
        binding.tvTags.text = station.getTagList().take(3).joinToString("  ·  ")
        setFavIcon(station.isFavorite)
        updateLogoPager(station)
        applyFrostedBackground()
    }

    private fun applyGlassmorphism(bmp: Bitmap) {
        if (!isAdded || _binding == null) return
        // Blur arka planı gizle - renk değiştirmesin
        binding.ivBlurBackground.setImageDrawable(null)
        binding.ivBlurBackground.visibility = android.view.View.GONE
        applyFrostedBackground()
    }

    private fun applyFrostedBackground() {
        if (!isAdded || _binding == null) return
        // Sabit buzlu beyaz - logo rengi ne olursa değişmez
        binding.root.setBackgroundColor(Color.parseColor("#EBF0FA"))
        binding.tvStationName.setTextColor(Color.parseColor("#1A1A2E"))
        binding.tvCountry.setTextColor(Color.parseColor("#2E3A5C"))
        binding.tvTags.setTextColor(Color.parseColor("#3A4A6E"))
        binding.tvStatus.setTextColor(Color.parseColor("#1A1A2E"))
        binding.tvStationIndex.setTextColor(Color.parseColor("#3A4A6E"))
    }

    private fun loadBannerAd() {
        try {
            val container = binding.playerBannerContainer
            container.visibility = android.view.View.GONE // yüklenene kadar gizli
            com.globalradio.livetuneinogzapp.ads.AdManager.loadBanner(
                requireActivity() as android.app.Activity,
                container
            )
        } catch (_: Exception) {
        }
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private fun setPlayIcon(playing: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (playing) R.drawable.ic_pause_circle_large
            else R.drawable.ic_play_circle_large
        )
    }

    private fun setBusy(busy: Boolean) {
        binding.progressBuffering.visibility = if (busy) View.VISIBLE else View.GONE
        binding.btnPlayPause.visibility = if (busy) View.INVISIBLE else View.VISIBLE
    }

    private fun setFavIcon(fav: Boolean) {
        binding.btnFavorite.setImageResource(
            if (fav) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        )
        binding.btnFavorite.setColorFilter(
            if (fav) Color.parseColor("#e01b3b") else Color.WHITE
        )
    }

    private fun startEq() {
        binding.equalizerView.visibility = View.VISIBLE
        binding.equalizerView.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.equalizer_anim)
        )
    }

    private fun stopEq() {
        binding.equalizerView.clearAnimation()
        binding.equalizerView.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        volumeHandler.removeCallbacks(volumeRunnable)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_STATION = "station"
        fun newInstance(st: RadioStation) = PlayerBottomSheet().apply {
            arguments = Bundle().apply { putParcelable(ARG_STATION, st) }
        }
    }
}