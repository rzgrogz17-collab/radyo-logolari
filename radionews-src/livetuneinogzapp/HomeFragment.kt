package com.globalradio.livetuneinogzapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.Chip
import androidx.media3.common.util.UnstableApi
import com.globalradio.livetuneinogzapp.databinding.FragmentHomeBinding
import com.globalradio.livetuneinogzapp.cast.CastManager
import com.globalradio.livetuneinogzapp.viewmodel.MainViewModel

@UnstableApi
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val mainActivity get() = activity as? MainActivity

    private var currentSection = SECTION_ALL
    private var sectionFragments = arrayOfNulls<SectionListFragment>(SECTION_COUNT)

    private val genres = listOf(
        "Pop", "Rock", "news", "Jazz", "Hip-Hop",
        "Electronic", "Dance", "Classical", "House", "Folk", "80s", "90s", "Talk"
    )

    companion object {
        const val SECTION_ALL = 0
        const val SECTION_TOP = 1
        const val SECTION_FAVORITES = 2
        const val SECTION_GENRES = 3
        const val SECTION_COUNTRIES = 4
        const val SECTION_COUNT = 5
    }

    inner class SectionPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount() = SECTION_COUNT
        override fun createFragment(position: Int): SectionListFragment {
            val f = SectionListFragment.newInstance(position)
            sectionFragments[position] = f
            return f
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSettings.setOnClickListener { mainActivity?.openSettings() }
        CastManager.attachButton(requireContext(), binding.btnCast)
        setupViewPager()
        setupTabButtons()
        setupSearch()
        observeLoading()
    }

    // ── ViewPager2 ────────────────────────────────────────────────────────────

    private fun setupViewPager() {
        binding.viewPager.adapter = SectionPagerAdapter()
        binding.viewPager.offscreenPageLimit = SECTION_COUNT - 1

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentSection = position
                updateTabButtons(position)
                scrollTabIntoView(position)

                binding.genreChipScroll.visibility =
                    if (position == SECTION_GENRES) View.VISIBLE else View.GONE

                // Arama aktifken ALL'a geçişte temizleme — diğer sekmelere geçince temizle
                if (position != SECTION_ALL && binding.etSearch.text?.isNotEmpty() == true) {
                    binding.etSearch.text?.clear()
                }

                // Bölüme özel tetiklemeler
                when (position) {
                    SECTION_GENRES -> buildGenreChips()
                    SECTION_TOP -> viewModel.refreshMostListened()
                    SECTION_ALL -> {
                        if (binding.etSearch.text.isNullOrEmpty()) {
                            viewModel.filterByCategory(MainViewModel.CATEGORY_ALL)
                        }
                    }

                    SECTION_FAVORITES -> { /* favoriteStations observer zaten halleder */
                    }
                }
            }
        })
    }

    // ── Tab Butonları ─────────────────────────────────────────────────────────

    private fun setupTabButtons() {
        listOf(
            binding.btnSectionAll to SECTION_ALL,
            binding.btnSectionTopChart to SECTION_TOP,
            binding.btnSectionFavorites to SECTION_FAVORITES,
            binding.btnSectionGenres to SECTION_GENRES,
            binding.btnSectionCountries to SECTION_COUNTRIES
        ).forEach { (btn, section) ->
            btn.setOnClickListener { binding.viewPager.currentItem = section }
        }
    }

    fun selectSection(section: Int) {
        binding.viewPager.currentItem = section
    }

    private fun updateTabButtons(active: Int) {
        listOf(
            binding.btnSectionAll,
            binding.btnSectionTopChart,
            binding.btnSectionFavorites,
            binding.btnSectionGenres,
            binding.btnSectionCountries
        ).forEachIndexed { i, btn ->
            if (i == active) {
                animateTabActive(btn)
            } else {
                animateTabInactive(btn)
            }
        }
    }

    private fun animateTabActive(btn: TextView) {
        btn.setBackgroundResource(R.drawable.bg_section_active)
        btn.setTextColor(Color.WHITE)

        val scaleX = ObjectAnimator.ofFloat(btn, "scaleX", 0.85f, 1.08f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(btn, "scaleY", 0.85f, 1.08f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(btn, "alpha", 0.6f, 1.0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 280
            interpolator = OvershootInterpolator(2.0f)
            start()
        }
    }

    private fun animateTabInactive(btn: TextView) {
        btn.setBackgroundResource(R.drawable.bg_section_inactive)

        val scaleX = ObjectAnimator.ofFloat(btn, "scaleX", btn.scaleX, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(btn, "scaleY", btn.scaleY, 1.0f)
        val alpha = ObjectAnimator.ofFloat(btn, "alpha", btn.alpha, 1.0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 200
            start()
        }

        val colorAnim = ValueAnimator.ofArgb(Color.WHITE, btn.context.getColor(R.color.bone_white))
        colorAnim.duration = 200
        colorAnim.addUpdateListener { btn.setTextColor(it.animatedValue as Int) }
        colorAnim.start()
    }

    private fun scrollTabIntoView(idx: Int) {
        val btns = listOf(
            binding.btnSectionAll, binding.btnSectionTopChart,
            binding.btnSectionFavorites, binding.btnSectionGenres,
            binding.btnSectionCountries
        )
        btns.getOrNull(idx)?.let {
            binding.tabScrollView.smoothScrollTo(it.left - 32, 0)
        }
    }

    // ── Türler Chip'leri ──────────────────────────────────────────────────────

    private fun genreLabel(tag: String): String = when (tag.lowercase()) {
        "pop" -> getString(R.string.genre_pop)
        "rock" -> getString(R.string.genre_rock)
        "news" -> getString(R.string.genre_news)
        "jazz" -> getString(R.string.genre_jazz)
        "hip-hop" -> getString(R.string.genre_hiphop)
        "electronic" -> getString(R.string.genre_electronic)
        "dance" -> getString(R.string.genre_dance)
        "classical" -> getString(R.string.genre_classical)
        "house" -> getString(R.string.genre_house)
        "folk" -> getString(R.string.genre_folk)
        "talk" -> getString(R.string.genre_talk)
        else -> tag
    }

    private fun buildGenreChips() {
        if (binding.chipGroupGenres.childCount > 0) return
        genres.forEach { genre ->
            val chip = Chip(requireContext()).apply {
                text = genreLabel(genre)
                isCheckable = true
                isChecked = false
                setChipBackgroundColorResource(R.color.chip_selector)
                setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
                chipStrokeWidth = 1f
                setChipStrokeColorResource(R.color.accent)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        viewModel.filterGenre(genre)
                    }
                }
            }
            binding.chipGroupGenres.addView(chip)
        }
        (binding.chipGroupGenres.getChildAt(0) as? Chip)?.isChecked = true
        viewModel.filterGenre(genres[0])
    }

    // ── Arama (anında, büyük/küçük harf duyarsız, "başlangıç eşleşmesi") ─────

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim() ?: ""
                binding.ivSearchClear.visibility =
                    if (q.isEmpty()) View.GONE else View.VISIBLE

                // Debounce YOK: her karakterde anında filtrelenir (ör. "a" → a ile
                // başlayanlar, "al" → al ile başlayanlar, "alt" → alt ile başlayanlar).
                if (q.isNotEmpty()) {
                    if (currentSection != SECTION_ALL) {
                        binding.viewPager.setCurrentItem(SECTION_ALL, false)
                    }
                    viewModel.search(q)
                } else {
                    viewModel.filterByCategory(MainViewModel.CATEGORY_ALL)
                }
            }
        })
        binding.ivSearchClear.setOnClickListener {
            binding.etSearch.text?.clear()
        }
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    private fun animateDots(show: Boolean) {
        val dots = listOf(
            binding.loadingIndicator.getChildAt(0),
            binding.loadingIndicator.getChildAt(1),
            binding.loadingIndicator.getChildAt(2)
        )
        if (show) {
            binding.loadingIndicator.visibility = View.VISIBLE
            dots.forEachIndexed { i, dot ->
                dot ?: return@forEachIndexed
                val anim = android.view.animation.AnimationUtils.loadAnimation(
                    requireContext(),
                    R.anim.dot_wave
                )
                anim.startOffset = (i * 200).toLong()
                dot.startAnimation(anim)
            }
        } else {
            binding.loadingIndicator.visibility = View.GONE
            dots.forEach { it?.clearAnimation() }
        }
    }

    private fun observeLoading() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            val hasData = viewModel.allStationsList().isNotEmpty()
            animateDots(loading && !hasData)
        }
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrEmpty()) {
                android.widget.Toast.makeText(
                    requireContext(), err, android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}