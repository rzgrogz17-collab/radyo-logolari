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
    private var logoPagerUserDrag = false
    private var pendingLogoIndex = -1
    private var logoNeedsRecenter = false
    private var logoCenterAttempts = 0
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

        // Başlangıç UI — serviste çalan istasyon öncelikli
        (radioService?.currentStation ?: currentStation)?.let { updateUI(it) }
    }

    // ── Logo ViewPager2 kurulumu ─────────────────────────────────────────────

    private val logoCenterRetry = Runnable {
        if (_binding == null || isPagerScrolling) return@Runnable
        if (centerCurrentLogo() == LogoCenterResult.CENTERED) logoNeedsRecenter = false
    }

    private val logoCenterPreDraw = android.view.ViewTreeObserver.OnPreDrawListener {
        if (_binding == null || isPagerScrolling || !logoNeedsRecenter) return@OnPreDrawListener true
        when (centerCurrentLogo()) {
            LogoCenterResult.CENTERED -> {
                logoNeedsRecenter = false
                logoCenterAttempts = 0
                true
            }
            LogoCenterResult.ADJUSTED -> {
                logoCenterAttempts++
                if (logoCenterAttempts > 8) {
                    logoNeedsRecenter = false
                    logoCenterAttempts = 0
                    true
                } else {
                    false
                }
            }
            LogoCenterResult.NOT_READY -> true
        }
    }

    private fun setupSwipeGesture() {
        logoPagerAdapter = LogoPagerAdapter { station ->
            // Görünür olan istasyonu takip et
        }
        val pager = binding.logoPager
        pager.offscreenPageLimit = 2
        pager.clipToPadding = false
        pager.clipChildren = false
        applyLogoPeekPadding()
        pager.adapter = logoPagerAdapter
        applyLogoPageGap()
        applyLogoPeekPadding()

        pager.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            applySquareLogo()
            applyLogoPeekPadding()
            if (logoNeedsRecenter && !isPagerScrolling) {
                pager.removeCallbacks(logoCenterRetry)
                pager.post(logoCenterRetry)
            }
        }
        val vto = pager.viewTreeObserver
        if (vto.isAlive) vto.addOnPreDrawListener(logoCenterPreDraw)
        pager.post(logoCenterRetry)

        // Sayfa değişince radyoyu değiştir — yalnızca kullanıcı kaydırınca
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pendingLogoIndex = position
                if (!logoPagerUserDrag) return
                playPagerStation(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        logoPagerUserDrag = true
                        isPagerScrolling = true
                        logoNeedsRecenter = false
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        val dragged = logoPagerUserDrag
                        logoPagerUserDrag = false
                        isPagerScrolling = false
                        if (dragged) playPagerStation(pager.currentItem)
                        if (logoNeedsRecenter && centerCurrentLogo() == LogoCenterResult.CENTERED) {
                            logoNeedsRecenter = false
                        }
                    }
                }
            }
        })
    }

    private fun logoPeekPx(): Int = 0

    private fun logoPageGapPx(): Int = (36f * resources.displayMetrics.density).toInt()

    /** Kaydırırken logolar yapışmasın. Durunca ortadaki logo ölçüsü aynı kalır. */
    private fun applyLogoPageGap() {
        if (_binding == null) return
        val gap = logoPageGapPx().toFloat()
        binding.logoPager.setPageTransformer { page, position ->
            page.translationX = gap * position
        }
    }

    /** Kare logo, kenarlardan içeride. Kaydırma aynı kalır; komşu kart görünmez. */
    private fun applySquareLogo(): Boolean {
        if (_binding == null) return false
        val frame = binding.logoFrame
        val parent = frame.parent as? View ?: return false
        if (parent.width <= 0 || parent.height <= 0) return false
        val lp = frame.layoutParams as? android.widget.LinearLayout.LayoutParams ?: return false
        val density = resources.displayMetrics.density
        // Kırmızı çerçeve: genişliğin yaklaşık %4'ü kadar kenar boşluğu, kare.
        val gap = (parent.width * 0.043f).toInt().coerceAtLeast((12f * density).toInt())
        val maxW = parent.width - gap * 2
        val maxH = parent.height - binding.tvStationIndex.height - lp.topMargin - (12f * density).toInt()
        val side = minOf(maxW, maxH)
        if (side <= 0) return false
        if (lp.width == side && lp.height == side) return false
        lp.width = side
        lp.height = side
        lp.gravity = android.view.Gravity.CENTER_HORIZONTAL
        frame.layoutParams = lp
        return true
    }

    /**
     * Komşu logolar görünmez. Sağa sola kaydırınca istasyon değişimi sürer.
     */
    private fun applyLogoPeekPadding(): Boolean {
        if (_binding == null) return false
        val pager = binding.logoPager
        val peek = logoPeekPx()
        pager.clipToPadding = false
        pager.clipChildren = false
        var changed = false
        if (pager.paddingStart != 0 || pager.paddingEnd != 0) {
            pager.setPaddingRelative(0, pager.paddingTop, 0, pager.paddingBottom)
            changed = true
        }
        val rv = pager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView ?: return changed
        rv.clipToPadding = false
        rv.clipChildren = false
        rv.overScrollMode = View.OVER_SCROLL_NEVER
        if (rv.paddingStart != peek || rv.paddingEnd != peek) {
            rv.setPaddingRelative(peek, 0, peek, 0)
            changed = true
        }
        return changed
    }

    /**
     * Çalan radyo kartını sağ/sol kenara eşit uzaklıkta ortalar.
     * ViewPager2 setCurrentItem aynı index'te no-op olduğu için sayfanın
     * sol kenarını paddingLeft ile hizalar.
     */
    private fun centerCurrentLogo(): LogoCenterResult {
        if (_binding == null || !::logoPagerAdapter.isInitialized) return LogoCenterResult.NOT_READY
        val pager = binding.logoPager
        if (pager.scrollState != ViewPager2.SCROLL_STATE_IDLE) return LogoCenterResult.NOT_READY
        val index = pendingLogoIndex
        if (index < 0 || index >= logoPagerAdapter.itemCount) return LogoCenterResult.NOT_READY
        if (applySquareLogo()) return LogoCenterResult.ADJUSTED
        if (applyLogoPeekPadding()) return LogoCenterResult.ADJUSTED
        val rv = pager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView ?: return LogoCenterResult.NOT_READY
        val peek = logoPeekPx()
        if (pager.width <= 0 || rv.width <= peek * 2) return LogoCenterResult.NOT_READY
        val lm = rv.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
            ?: return LogoCenterResult.NOT_READY
        val page = lm.findViewByPosition(index)
        if (page == null) {
            lm.scrollToPositionWithOffset(index, 0)
            if (pager.currentItem != index) pager.setCurrentItem(index, false)
            return LogoCenterResult.ADJUSTED
        }
        val marginLeft = (page.layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
        val desiredLeft = rv.paddingLeft + marginLeft
        val dx = page.left - desiredLeft
        if (kotlin.math.abs(dx) > 1) rv.scrollBy(dx, 0)
        if (pager.currentItem != index) pager.setCurrentItem(index, false)
        val aligned = lm.findViewByPosition(index)
        val alignedLeft = aligned?.left ?: return LogoCenterResult.ADJUSTED
        val alignedMargin = (aligned.layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
        return if (kotlin.math.abs(alignedLeft - (rv.paddingLeft + alignedMargin)) <= 1) {
            LogoCenterResult.CENTERED
        } else {
            LogoCenterResult.ADJUSTED
        }
    }

    private fun playPagerStation(position: Int) {
        val station = logoPagerAdapter.getStation(position) ?: return
        val playingId = radioService?.currentStation?.id ?: currentStation?.id
        if (station.id == playingId) return
        play(station)
        currentStation = station
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
                val fromVm = activeViewModel?.playlistFor(cur).orEmpty()
                if (fromVm.isNotEmpty()) fromVm
                else {
                    val active = activeViewModel?.getActiveSectionList() ?: emptyList()
                    val fav = activeViewModel?.favoriteStations?.value ?: emptyList()
                    if (active.any { it.id == cur.id }) active
                    else if (fav.any { it.id == cur.id }) fav
                    else activeViewModel?.allStationsList() ?: emptyList()
                }
            }
        }

        val list = if (fullList.isNotEmpty() && fullList.any { it.id == cur.id }) fullList
        else if (fullList.isNotEmpty()) (listOf(cur) + fullList).distinctBy { it.id }
        else listOf(cur) // fallback

        // Listeyi sadece gerektiğinde güncelle
        if (logoPagerAdapter.indexOf(cur) < 0 || logoPagerAdapter.itemCount != list.size) {
            logoPagerAdapter.setStations(list)
        }
        val idx = logoPagerAdapter.indexOf(cur)
        if (idx < 0) return
        pendingLogoIndex = idx
        if (binding.logoPager.currentItem != idx) {
            binding.logoPager.setCurrentItem(idx, false)
        }
        logoNeedsRecenter = true
        logoCenterAttempts = 0
        applyLogoPeekPadding()
        binding.logoPager.removeCallbacks(logoCenterRetry)
        binding.logoPager.post(logoCenterRetry)
        binding.logoPager.postDelayed(logoCenterRetry, 32)
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

    // ── İlerleme / geri sar / kaydet ─────────────────────────────────────────

    private fun setupProgressControls() {
        binding.btnSave.setOnClickListener { onSaveClicked() }
        updateSaveButton(radioService?.isRecording() == true)
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
            val pulse = AlphaAnimation(1f, 0.35f).apply {
                duration = 700
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }
            binding.btnSave.startAnimation(pulse)
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

        binding.btnSettings.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.btnShare.setOnClickListener { shareCurrentStation() }

        binding.btnEqualizerPlayer.setOnClickListener {
            EqualizerBottomSheet.newInstance().show(parentFragmentManager, "Equalizer")
        }
    }

    // ── Oynat yardımcısı ─────────────────────────────────────────────────────

    private fun shareCurrentStation() {
        val st = radioService?.currentStation ?: currentStation ?: return
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

    private fun play(st: RadioStation) {
        val queue = radioService?.getPlaylist()?.takeIf { it.isNotEmpty() }
            ?: countryActivity?.getStationList()?.takeIf { it.isNotEmpty() }
            ?: activeViewModel?.playlistFor(st)
        radioService?.playStation(st, queue)
        activeViewModel?.recordPlay(st)
        currentStation = st
    }

    // ── State gözlemle ────────────────────────────────────────────────────────

    private var trackObserved = false

    private fun observeTrackTitle() {
        if (trackObserved || !isAdded) return
        val service = radioService ?: return
        trackObserved = true
        service.nowPlayingTitle.observe(viewLifecycleOwner) { title ->
            renderTrackLine(title)
        }
        renderTrackLine(service.nowPlayingTitle.value)
    }

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
        observeTrackTitle()
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
        observeTrackTitle()
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
            }

            is PlayerState.Paused -> {
                currentStation = state.station
                updateUI(state.station)
                setPlayIcon(false)
                setBusy(false)
                stopEq()
            }

            is PlayerState.Buffering -> {
                setBusy(true)
                stopEq()
            }

            is PlayerState.Reconnecting -> {
                setBusy(true)
                stopEq()
            }

            is PlayerState.Error -> {
                setPlayIcon(false)
                setBusy(false)
                stopEq()
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
        bindScrollingLabel(binding.tvStationName, station.name)
        renderTrackLine(radioService?.nowPlayingTitle?.value)
        val fav = isFavoriteNow(station.id)
        station.isFavorite = fav
        radioService?.updateFavorite(station.id, fav)
        setFavIcon(fav)
        updateLogoPager(station)
        applyFrostedBackground()
    }

    private fun renderTrackLine(song: String?) {
        if (_binding == null) return
        val station = radioService?.currentStation ?: currentStation
        val name = station?.name?.trim().orEmpty()
        val track = song?.trim().orEmpty().takeIf { it.isNotEmpty() && !it.equals(name, true) }.orEmpty()
        if (track.isEmpty()) {
            binding.tvTags.visibility = View.GONE
            binding.tvTags.text = ""
            return
        }
        binding.tvTags.visibility = View.VISIBLE
        bindScrollingLabel(binding.tvTags, track)
    }

    private fun bindScrollingLabel(view: android.widget.TextView, text: String) {
        view.text = text
        view.isSelected = true
        view.post {
            if (_binding == null || view.text?.toString() != text) return@post
            val available = (view.width - view.paddingLeft - view.paddingRight).coerceAtLeast(0)
            val fits = available > 0 && view.paint.measureText(text) <= available
            view.gravity = if (fits) {
                android.view.Gravity.CENTER
            } else {
                android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            }
            view.ellipsize = if (fits) null else android.text.TextUtils.TruncateAt.MARQUEE
            view.isSelected = true
        }
    }

    private fun applyGlassmorphism(bmp: Bitmap) {
        if (!isAdded || _binding == null) return
        binding.ivBlurBackground.setImageDrawable(null)
        binding.ivBlurBackground.visibility = android.view.View.GONE
        applyFrostedBackground()
    }

    private fun applyFrostedBackground() {
        if (!isAdded || _binding == null) return
        binding.root.setBackgroundColor(Color.parseColor("#EBF0FA"))
        binding.tvStationName.setTextColor(Color.parseColor("#1A1A2E"))
        binding.tvTags.setTextColor(Color.parseColor("#1A1A2E"))
        binding.tvStationIndex.setTextColor(Color.WHITE)
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
        binding.btnPlayPause.visibility = View.VISIBLE
    }

    private fun setFavIcon(fav: Boolean) {
        if (_binding == null) return
        FavoriteIcon.apply(binding.btnFavorite, fav, emptyColor = Color.parseColor("#b2b5c0"))
    }

    private fun startEq() {}

    private fun stopEq() {}

    override fun onDestroyView() {
        recordingObserved = false
        if (_binding != null) {
            val pager = binding.logoPager
            pager.removeCallbacks(logoCenterRetry)
            val vto = pager.viewTreeObserver
            if (vto.isAlive) vto.removeOnPreDrawListener(logoCenterPreDraw)
            binding.btnSave.clearAnimation()
        }
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

    private enum class LogoCenterResult { NOT_READY, ADJUSTED, CENTERED }
}