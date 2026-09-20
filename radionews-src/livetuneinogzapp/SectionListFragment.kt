package com.globalradio.livetuneinogzapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.globalradio.livetuneinogzapp.adapter.CountryAdapter
import com.globalradio.livetuneinogzapp.adapter.MostListenedAdapter
import com.globalradio.livetuneinogzapp.adapter.StationAdapter
import com.globalradio.livetuneinogzapp.model.PlayerState
import androidx.media3.common.util.UnstableApi
import com.globalradio.livetuneinogzapp.viewmodel.MainViewModel

@UnstableApi
class SectionListFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val mainActivity get() = activity as? MainActivity

    private var stationAdapter: StationAdapter? = null
    private var topAdapter: MostListenedAdapter? = null
    private var countryAdapter: CountryAdapter? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var userTriggeredRefresh = false

    private val sectionType get() = arguments?.getInt(ARG_SECTION, 0) ?: 0

    private val genres = listOf(
        "Pop", "Rock", "News", "Haber", "Jazz", "Hip-Hop",
        "Electronic", "Dance", "Classical", "House", "Folk", "80s", "90s", "Talk", "İslam"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // SwipeRefreshLayout içine RecyclerView koy
        swipeRefresh = SwipeRefreshLayout(requireContext()).apply {
            setColorSchemeResources(R.color.accent)
            setProgressBackgroundColorSchemeResource(R.color.bg_surface)
            setOnRefreshListener {
                // Kullanıcı pull-to-refresh yaptı
                userTriggeredRefresh = true
                viewModel.loadStations()
            }
        }
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isNestedScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        swipeRefresh.addView(recyclerView)
        return swipeRefresh
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSection()
        observeData()
    }

    private fun setupSection() {
        when (sectionType) {
            HomeFragment.SECTION_ALL -> {
                stationAdapter = StationAdapter(
                    onStationClick = { mainActivity?.playStation(it) },
                    onFavoriteClick = { viewModel.toggleFavorite(it) }
                )
                recyclerView.adapter = stationAdapter
                viewModel.filterByCategory(MainViewModel.CATEGORY_ALL)
            }

            HomeFragment.SECTION_FAVORITES -> {
                stationAdapter = StationAdapter(
                    onStationClick = { mainActivity?.playStation(it) },
                    onFavoriteClick = { viewModel.toggleFavorite(it) }
                )
                recyclerView.adapter = stationAdapter
            }

            HomeFragment.SECTION_TOP -> {
                topAdapter = MostListenedAdapter(
                    onStationClick = { mainActivity?.playStation(it) },
                    onFavoriteClick = { viewModel.toggleFavorite(it) }
                )
                recyclerView.adapter = topAdapter
                viewModel.refreshMostListened()
            }

            HomeFragment.SECTION_GENRES -> {
                stationAdapter = StationAdapter(
                    onStationClick = { mainActivity?.playStation(it) },
                    onFavoriteClick = { viewModel.toggleFavorite(it) }
                )
                recyclerView.adapter = stationAdapter
                // "Tümü" sekmesinden bağımsız kendi filtresini kullanır
                viewModel.filterGenre(genres[0])
            }

            HomeFragment.SECTION_COUNTRIES -> {
                countryAdapter = CountryAdapter { country ->
                    startActivity(
                        Intent(requireContext(), CountryStationsActivity::class.java)
                            .putExtra(CountryStationsActivity.EXTRA_COUNTRY, country.name)
                            .putExtra(CountryStationsActivity.EXTRA_COUNTRY_CODE, country.isoCode)
                    )
                }
                recyclerView.adapter = countryAdapter
                buildCountryList()
            }
        }
    }

    private fun observeData() {
        // Yükleme durumu — sadece kullanıcı pull-to-refresh yaptıysa döngü göster
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (userTriggeredRefresh) {
                swipeRefresh.isRefreshing = loading
            }
            if (!loading) {
                userTriggeredRefresh = false // yükleme bitti, bayrağı sıfırla
            }
        }

        // Hata durumu
        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrEmpty()) {
                swipeRefresh.isRefreshing = false
                userTriggeredRefresh = false
            }
        }

        // Tümü bölümü → allSectionStations (arama filtreli, türden bağımsız TÜM istasyonlar)
        if (sectionType == HomeFragment.SECTION_ALL) {
            viewModel.allSectionStations.observe(viewLifecycleOwner) { stations ->
                stationAdapter?.submitList(stations)
            }
        }

        // Türler bölümü → kendi bağımsız akışı (artık "Tümü" ile paylaşılmıyor)
        if (sectionType == HomeFragment.SECTION_GENRES) {
            viewModel.genreSectionStations.observe(viewLifecycleOwner) { stations ->
                stationAdapter?.submitList(stations)
            }
        }

        // Favoriler bölümü → sadece favorileStations
        if (sectionType == HomeFragment.SECTION_FAVORITES) {
            viewModel.favoriteStations.observe(viewLifecycleOwner) { stations ->
                stationAdapter?.submitList(stations)
            }
        }

        // En Çok bölümü
        if (sectionType == HomeFragment.SECTION_TOP) {
            viewModel.mostListenedStations.observe(viewLifecycleOwner) { stations ->
                topAdapter?.setPlayCounts(viewModel.playCounts.value ?: emptyMap())
                topAdapter?.submitList(stations)
            }
            viewModel.playCounts.observe(viewLifecycleOwner) { counts ->
                topAdapter?.refreshPlayCounts(counts)
            }
        }

        // Ülkeler bölümü → allStations değişince yeniden oluştur
        if (sectionType == HomeFragment.SECTION_COUNTRIES) {
            viewModel.countrySummaries.observe(viewLifecycleOwner) { summaries ->
                countryAdapter?.submitList(summaries)
            }
        }

        // Favori anlık güncelleme (payload)
        viewModel.favoritePayload.observe(viewLifecycleOwner) { payload ->
            payload ?: return@observe
            stationAdapter?.updateFavorite(payload.first, payload.second)
            topAdapter?.updateFavorite(payload.first, payload.second)
        }

        // Oynatıcı durumu (tüm bölümler için)
        viewModel.playerState.observe(viewLifecycleOwner) { state ->
            val playingId = when (state) {
                is PlayerState.Playing -> state.station.id
                is PlayerState.Buffering -> mainActivity?.getRadioService()?.currentStation?.id
                is PlayerState.Paused -> state.station.id
                is PlayerState.Reconnecting -> state.station.id
                else -> null
            }
            stationAdapter?.updatePlayingStation(playingId)
            topAdapter?.updatePlayingStation(playingId)
        }
    }

    /** Tür chip'i seçilince HomeFragment'tan çağrılır */
    fun filterByGenre(genre: String) {
        viewModel.filterGenre(genre)
    }

    /** Arama sorgusu HomeFragment'tan iletilir */
    fun applySearch(query: String) {
        viewModel.search(query)
    }

    private fun buildCountryList() {
        // countrySummaries observer zaten halleder, boş bırakılabilir
    }

    companion object {
        private const val ARG_SECTION = "section"
        fun newInstance(section: Int) = SectionListFragment().apply {
            arguments = Bundle().apply { putInt(ARG_SECTION, section) }
        }
    }
}