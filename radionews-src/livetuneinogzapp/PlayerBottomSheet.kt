package com.globalradio.livetuneinogzapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import android.content.res.ColorStateList
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.globalradio.livetuneinogzapp.adapter.LogoPagerAdapter
import com.globalradio.livetuneinogzapp.databinding.FragmentPlayerBottomSheetBinding
import com.globalradio.livetuneinogzapp.model.PlayerState
import com.globalradio.livetuneinogzapp.model.RadioStation
import androidx.media3.common.util.UnstableApi
import com.globalradio.livetuneinogzapp.service.RadioPlayerService
import com.globalradio.livetuneinogzapp.utils.FavoriteIcon
import com.globalradio.livetuneinogzapp.viewmodel.MainViewModel

@UnstableApi
class PlayerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentPlayerBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var currentStation: RadioStation? = null

    private lateinit var logoPagerAdapter: LogoPagerAdapter
    private var isPagerScrolling = false
    private var userScrubbing = false
    private var recordingObserved = false
    private var liveEdgePosition = 0L
    private var lastProgressStationId: String? = null

    private val writePermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) radioService?.startRecording()
        else if (isAdded) {
            Toast.makeText(requireContext(), R.string.recording_permission_needed, Toast.LENGTH_SHORT).show()
        }
    }

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

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (!isAdded || _binding == null) return
            updateProgressUi()
            volumeHandler.postDelayed(this, 500)
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

        // Tam ekran — sistem çubukları banner'ı kesmesin
        dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )?.let { sheet ->
            sheet.setBackgroundColor(Color.TRANSPARENT)
            if (sheet is ViewGroup) {
                sheet.clipChildren = false
                sheet.clipToPadding = false
            }
            BottomSheetBehavior.from(sheet).apply {
                peekHeight = resources.displayMetrics.heightPixels
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
            val navBottom = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                dialog?.window?.decorView?.rootWindowInsets
                    ?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
            } else {
                @Suppress("DEPRECATION")
                dialog?.window?.decorView?.rootWindowInsets?.systemWindowInsetBottom ?: 0
            }
            sheet.layoutParams.height = resources.displayMetrics.heightPixels
            sheet.requestLayout()
            binding.root.setPadding(
                binding.root.paddingLeft,
                binding.root.paddingTop,
                binding.root.paddingRight,
                navBottom.coerceAtLeast(binding.root.paddingBottom)
            )
        }

        setupSwipeGesture()
        setupVolumeSlider()
        setupProgressControls()
        setupButtons()
        observePlayerState()
        observeFavorites()
        ensureServiceObservers()
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
        binding.logoPager.clipToPadding = false
        binding.logoPager.clipChildren = false

        binding.logoPager.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applyLogoPeekPadding()
        }
        binding.logoPager.post { applyLogoPeekPadding() }

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

    /** Sağ/sol komşu logo kenarları görünsün — padding yalnızca iç RecyclerView'da. */
    private fun applyLogoPeekPadding() {
        if (_binding == null) return
        val pager = binding.logoPager
        val peek = (32f * resources.displayMetrics.density).toInt()
        pager.clipToPadding = false
        pager.clipChildren = false
        if (pager.paddingStart != 0 || pager.paddingEnd != 0) {
            pager.setPadding(0, pager.paddingTop, 0, pager.paddingBottom)
        }
        val rv = pager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView ?: return
        rv.clipToPadding = false
        rv.clipChildren = false
        rv.overScrollMode = View.OVER_SCROLL_NEVER
        if (rv.paddingStart != peek || rv.paddingEnd != peek) {
            rv.setPadding(peek, 0, peek, 0)
        }
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
        binding.logoPager.post { applyLogoPeekPadding() }
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
        volumeHandler.post(progressRunnable)
    }

    // ── İlerleme / geri sar / kaydet ─────────────────────────────────────────

    private fun setupProgressControls() {
        binding.seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (!fromUser) return
                val dur = radioService?.playbackDurationMs() ?: 0L
                if (dur > 0L) binding.tvElapsed.text = formatClock((dur * p) / sb!!.max)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                userScrubbing = true
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                userScrubbing = false
                val bar = sb ?: return
                val service = radioService ?: return
                val dur = service.playbackDurationMs()
                if (dur > 0L) {
                    service.seekToMs((dur * bar.progress) / bar.max)
                    return
                }
                val window = RadioPlayerService.LIVE_REWIND_WINDOW_MS
                val edge = liveEdgePosition.coerceAtLeast(service.playbackPositionMs())
                val start = (edge - window).coerceAtLeast(0L)
                val span = (edge - start).coerceAtLeast(1L)
                service.seekToMs(start + (span * bar.progress) / bar.max)
            }
        })
        binding.btnRewind.setOnClickListener {
            val ok = radioService?.rewind(15_000L) == true
            if (!ok && isAdded) {
                Toast.makeText(requireContext(), R.string.rewind_not_available, Toast.LENGTH_SHORT).show()
            }
            updateProgressUi()
        }
        binding.btnSave.setOnClickListener { onSaveClicked() }
        updateSaveButton(radioService?.isRecording() == true)
        updateProgressUi()
    }

    private fun onSaveClicked() {
        val service = radioService ?: return
        if (service.isRecording()) {
            service.stopRecording()
            return
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(requireContext(), perm)
                != PackageManager.PERMISSION_GRANTED
            ) {
                writePermLauncher.launch(perm)
                return
            }
        }
        service.startRecording()
    }

    private fun updateProgressUi() {
        if (_binding == null) return
        ensureServiceObservers()
        val service = radioService ?: return
        val duration = service.playbackDurationMs()
        val position = service.playbackPositionMs()
        val seekable = service.isPlaybackSeekable()
        val elapsed = when {
            duration > 0L && position <= duration + 1_000L -> position
            position in 1L until (24L * 3600_000L) -> position
            else -> service.sessionElapsedMs()
        }

        binding.tvElapsed.text = formatClock(elapsed)
        if (duration > 0L) {
            val remaining = (duration - position).coerceAtLeast(0L)
            binding.tvRemaining.text = if (seekable) {
                "−${formatClock(remaining)}"
            } else {
                getString(R.string.live_badge)
            }
            binding.tvRemaining.setTextColor(
                Color.parseColor(if (seekable) "#E6FFFFFF" else "#E01B3B")
            )
            binding.seekBarProgress.isEnabled = true
            if (!userScrubbing) {
                val max = binding.seekBarProgress.max.coerceAtLeast(1)
                binding.seekBarProgress.progress =
                    ((position.coerceAtLeast(0L) * max) / duration).toInt().coerceIn(0, max)
            }
        } else {
            if (position > liveEdgePosition) liveEdgePosition = position
            val window = RadioPlayerService.LIVE_REWIND_WINDOW_MS
            val edge = liveEdgePosition.coerceAtLeast(position)
            val start = (edge - window).coerceAtLeast(0L)
            val span = (edge - start).coerceAtLeast(1L)
            binding.tvRemaining.text = getString(R.string.live_badge)
            binding.tvRemaining.setTextColor(Color.parseColor("#E01B3B"))
            binding.seekBarProgress.isEnabled = true
            if (!userScrubbing) {
                val max = binding.seekBarProgress.max.coerceAtLeast(1)
                binding.seekBarProgress.progress =
                    (((position - start) * max) / span).toInt().coerceIn(0, max)
            }
        }
        if (service.isRecording()) {
            binding.tvSaveLabel.text = formatClock(service.recordingElapsedMs())
        }
    }

    private fun formatClock(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun updateSaveButton(recording: Boolean) {
        if (_binding == null) return
        binding.btnSave.setImageResource(
            if (recording) R.drawable.ic_record_stop else R.drawable.ic_record
        )
        ImageViewCompat.setImageTintList(
            binding.btnSave,
            ColorStateList.valueOf(Color.parseColor("#E01B3B"))
        )
        binding.btnSave.clearAnimation()
        if (recording) {
            binding.tvSaveLabel.text = getString(R.string.recording_short)
            val pulse = AlphaAnimation(1f, 0.35f).apply {
                duration = 700
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            binding.btnSave.startAnimation(pulse)
        } else {
            binding.tvSaveLabel.text = getString(R.string.save_short)
        }
    }

    // ── Butonlar ─────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnPlayPause.setOnClickListener { radioService?.togglePlayPause() }
        binding.btnNext.setOnClickListener { radioService?.playNextStation() }
        binding.btnPrevious.setOnClickListener { radioService?.playPreviousStation() }

        binding.btnFavorite.setOnClickListener {
            val st = radioService?.currentStation ?: currentStation ?: return@setOnClickListener
            val nowFav = when {
                countryActivity != null -> countryActivity!!.toggleFavoriteForStation(st)
                activeViewModel != null -> activeViewModel!!.toggleFavorite(st)
                else -> return@setOnClickListener
            }
            st.isFavorite = nowFav
            currentStation?.isFavorite = nowFav
            radioService?.updateFavorite(st.id, nowFav)
            setFavIcon(nowFav)
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

    private fun observeFavorites() {
        activeViewModel?.favoritePayload?.observe(viewLifecycleOwner) { payload ->
            payload ?: return@observe
            refreshFavoriteIcon(payload.first, payload.second)
        }
        activeViewModel?.favoriteStations?.observe(viewLifecycleOwner) {
            refreshFavoriteIcon()
        }
    }

    private fun ensureServiceObservers() {
        if (recordingObserved || !isAdded) return
        val service = radioService ?: return
        recordingObserved = true
        service.recordingState.observe(viewLifecycleOwner) { rec ->
            updateSaveButton(rec == true)
        }
        service.recordingEvent.observe(viewLifecycleOwner) { ev ->
            ev ?: return@observe
            if (!isAdded) return@observe
            if (ev.first) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.recording_saved, ev.second),
                    Toast.LENGTH_LONG
                ).show()
            } else if (ev.second != "short") {
                Toast.makeText(requireContext(), R.string.recording_failed, Toast.LENGTH_SHORT).show()
            }
            if (service.recordingEvent.value != null) {
                service.recordingEvent.value = null
            }
        }
    }

    private fun refreshFavoriteIcon(stationId: String? = null, forced: Boolean? = null) {
        val id = stationId
            ?: radioService?.currentStation?.id
            ?: currentStation?.id
            ?: return
        val playingId = radioService?.currentStation?.id ?: currentStation?.id
        if (playingId != null && playingId != id && forced != null) return
        val fav = forced ?: isFavoriteNow(id)
        if (playingId == null || playingId == id) setFavIcon(fav)
    }

    private fun isFavoriteNow(stationId: String): Boolean {
        activeViewModel?.let { return it.isFavorite(stationId) }
        countryActivity?.let { return it.isStationFavorite(stationId) }
        return radioService?.currentStation?.takeIf { it.id == stationId }?.isFavorite == true
            || currentStation?.takeIf { it.id == stationId }?.isFavorite == true
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
        if (lastProgressStationId != station.id) {
            liveEdgePosition = 0L
            lastProgressStationId = station.id
        }
        binding.tvStationName.text = station.name
        binding.tvStationName.isSelected = true
        binding.tvCountry.text = station.country
        binding.tvTags.text = station.getTagList().take(3).joinToString("  ·  ")
        val fav = isFavoriteNow(station.id)
        station.isFavorite = fav
        radioService?.updateFavorite(station.id, fav)
        setFavIcon(fav)
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
            container.visibility = android.view.View.INVISIBLE
            com.globalradio.livetuneinogzapp.ads.AdManager.loadBanner(
                requireActivity() as android.app.Activity,
                container,
                collapseIfEmpty = false
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
        if (_binding == null) return
        FavoriteIcon.apply(binding.btnFavorite, fav, onLightSurface = true)
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
        recordingObserved = false
        if (_binding != null) {
            binding.btnSave.clearAnimation()
        }
        volumeHandler.removeCallbacks(volumeRunnable)
        volumeHandler.removeCallbacks(progressRunnable)
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