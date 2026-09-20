package com.globalradio.livetuneinogzapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.globalradio.livetuneinogzapp.R
import com.globalradio.livetuneinogzapp.model.CountrySummary
import com.globalradio.livetuneinogzapp.model.PlayerState
import com.globalradio.livetuneinogzapp.model.RadioStation
import com.globalradio.livetuneinogzapp.repository.StationRepository
import com.globalradio.livetuneinogzapp.utils.ListenHistoryManager
import com.globalradio.livetuneinogzapp.utils.LocaleCountryMapper
import com.globalradio.livetuneinogzapp.utils.StationListOrganizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val repository: StationRepository
) : AndroidViewModel(application) {

    private val historyManager = ListenHistoryManager(application)

    private val _allStations = MutableLiveData<List<RadioStation>>(emptyList())

    // Arama sırasında sadece isim başlangıcı değil, isim/etiket/ülke içindeki
    // her "kelime"nin başlangıcı da eşleşsin diye kelime bazlı önbellek tutulur.
    private data class StationSearchCache(
        val id: String,
        val nameLower: String,
        val nameTokens: List<String>,
        val tagTokens: List<String>,
        val countryTokens: List<String>
    )

    private var searchCache: List<StationSearchCache> = emptyList()

    private val _allSectionStations = MutableLiveData<List<RadioStation>>(emptyList())
    val allSectionStations: LiveData<List<RadioStation>> = _allSectionStations

    // "Türler" (Genres) sekmesi ARTIK "Tümü" sekmesiyle aynı LiveData/kategori
    // durumunu PAYLAŞMIYOR. Önceden ikisi de aynı currentCategory + aynı
    // _allSectionStations akışını kullandığı için, Genres sekmesi bir tür
    // seçtiğinde (ör. "Pop") bu durum "Tümü" sekmesini de kirletiyor ve
    // "Tümü" listesi tüm istasyonlar yerine sadece o türle sınırlı kalıyordu.
    // Bu da cihaz diline göre öncelik sıralamasının tüm listede değil,
    // yanlışlıkla küçük bir alt kümede uygulanmış gibi görünmesine yol açıyordu.
    private var currentGenre: String = ""
    private val _genreSectionStations = MutableLiveData<List<RadioStation>>(emptyList())
    val genreSectionStations: LiveData<List<RadioStation>> = _genreSectionStations

    private val _favoriteStations = MutableLiveData<List<RadioStation>>(emptyList())
    val favoriteStations: LiveData<List<RadioStation>> = _favoriteStations

    private val _mostListenedStations = MutableLiveData<List<RadioStation>>(emptyList())
    val mostListenedStations: LiveData<List<RadioStation>> = _mostListenedStations

    private val _playCounts = MutableLiveData<Map<String, Int>>(emptyMap())
    val playCounts: LiveData<Map<String, Int>> = _playCounts

    private val _countrySummaries = MutableLiveData<List<CountrySummary>>(emptyList())
    val countrySummaries: LiveData<List<CountrySummary>> = _countrySummaries

    private val _filteredStations = MutableLiveData<List<RadioStation>>(emptyList())
    val filteredStations: LiveData<List<RadioStation>> = _filteredStations

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)

    private val _favoritePayload = MutableLiveData<Pair<String, Boolean>?>(null)
    val favoritePayload: LiveData<Pair<String, Boolean>?> = _favoritePayload
    val error: LiveData<String?> = _error

    private val _playerState = MutableLiveData<PlayerState>(PlayerState.Idle)
    val playerState: LiveData<PlayerState> = _playerState

    private var currentQuery = ""
    private var currentCategory = CATEGORY_ALL

    private var dataLoaded = false

    // Cihaz dilinde karşılığı olan ülke(ler); veri setinde bulunmuyorsa
    // İngilizce (fallback) ülkeler kullanılır. "Tümü" sıralamasında ve
    // "Ülkeler" listesinde en üste alınacak ülkeleri belirler.
    private var priorityCountries: List<String> = emptyList()

    init {
        loadStations()
    }

    fun loadStations() {
        if (_isLoading.value == true) return
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            var attempt = 0
            var success = false

            while (attempt < 3 && !success) {
                if (attempt > 0) delay(2000L * attempt)

                repository.getStations()
                    .onSuccess { stations ->
                        applyLoadedStations(stations)
                        success = true
                    }
                    .onFailure { e ->
                        attempt++
                        if (attempt >= 3) {
                            val cached = repository.getCachedStations()
                            val ctx = getApplication<Application>()
                            if (!cached.isNullOrEmpty()) {
                                applyLoadedStations(cached)
                                success = true
                                _error.postValue(ctx.getString(R.string.error_offline_cache))
                            } else {
                                val msg = e.message ?: ""
                                _error.postValue(
                                    when {
                                        msg.contains("Unable to resolve host", ignoreCase = true) ||
                                                msg.contains("No address", ignoreCase = true) ||
                                                msg.contains("UnknownHost", ignoreCase = true) ->
                                            ctx.getString(R.string.error_no_internet)

                                        msg.contains("timeout", ignoreCase = true) ->
                                            ctx.getString(R.string.error_timeout)

                                        else ->
                                            ctx.getString(R.string.error_load)
                                    }
                                )
                            }
                        }
                    }
            }
            _isLoading.postValue(false)
        }
    }

    private fun applyLoadedStations(stations: List<RadioStation>) {
        dataLoaded = true
        _allStations.postValue(stations)
        searchCache = stations.map { s ->
            StationSearchCache(
                id = s.id,
                nameLower = s.name.lowercase(),
                nameTokens = tokenize(s.name),
                tagTokens = tokenize(s.tags),
                countryTokens = tokenize(s.country)
            )
        }
        buildCountries(stations)
        updateAllSection(stations)
        updateGenreSection(stations)
        updateFavorites(stations)
        refreshMostListened()
    }

    /**
     * Bir metni harf/rakam olmayan karakterlerden (boşluk, virgül, parantez vb.)
     * ayırarak küçük harfli "kelime" parçalarına böler. Arama kutusunda
     * büyük/küçük harf farkı gözetmeksizin ve "başlangıç eşleşmesi" (prefix)
     * mantığıyla anında sonuç üretmek için kullanılır.
     */
    private fun tokenize(text: String): List<String> =
        text.lowercase().split(TOKEN_SPLIT_REGEX).filter { it.isNotEmpty() }

    fun reloadIfEmpty() {
        if (!dataLoaded || _allStations.value.isNullOrEmpty()) {
            loadStations()
        }
    }

    private fun updateAllSection(stations: List<RadioStation>) {
        // NOT: "Tümü" sekmesi artık türe göre filtrelenmiyor (bkz. filterGenre /
        // updateGenreSection) — bu sayede her zaman TÜM istasyonları temsil eder
        // ve cihaz diline göre öncelik sıralaması bütün listeye uygulanır.
        var result: List<RadioStation> =
            if (currentCategory == CATEGORY_FAVORITES) stations.filter { it.isFavorite }
            else stations

        if (currentQuery.isNotEmpty()) {
            val qLower = currentQuery.lowercase()
            val queryTokens = tokenize(currentQuery)
            val matchIds = if (searchCache.isNotEmpty()) {
                searchCache.filter { cache -> tokensMatch(queryTokens, cache.nameTokens, cache.tagTokens, cache.countryTokens) }
                    .sortedWith(
                        compareBy(
                            { !it.nameLower.startsWith(qLower) },
                            { it.nameLower }
                        )).map { it.id }.toHashSet()
            } else {
                result.filter { st ->
                    tokensMatch(queryTokens, tokenize(st.name), tokenize(st.tags), tokenize(st.country))
                }.map { it.id }.toHashSet()
            }
            result = result.filter { it.id in matchIds }
                .sortedWith(
                    compareBy(
                        { !it.name.lowercase().startsWith(qLower) },
                        { it.name.lowercase() }
                    ))
        } else {
            result = StationListOrganizer.sortByPriorityCountry(result, priorityCountries)
        }

        try {
            _allSectionStations.value = result
            _filteredStations.value = result
        } catch (_: Exception) {
            _allSectionStations.postValue(result)
            _filteredStations.postValue(result)
        }
    }

    /**
     * Yazılan her kelime (queryTokens) için, isim/etiket/ülke kelimelerinden
     * (nameTokens/tagTokens/countryTokens) EN AZ BİRİNİN o kelimeyle
     * BAŞLAMASI gerekir. Böylece "a" → a ile başlayanlar, "al" → al ile
     * başlayanlar, "alt" → alt ile başlayanlar şeklinde anında ve büyük/küçük
     * harf duyarsız bir "başlangıç eşleşmesi" arama davranışı elde edilir.
     */
    private fun tokensMatch(
        queryTokens: List<String>,
        nameTokens: List<String>,
        tagTokens: List<String>,
        countryTokens: List<String>
    ): Boolean {
        if (queryTokens.isEmpty()) return false
        return queryTokens.all { qt ->
            nameTokens.any { it.startsWith(qt) } ||
                    tagTokens.any { it.startsWith(qt) } ||
                    countryTokens.any { it.startsWith(qt) }
        }
    }

    private fun updateFavorites(stations: List<RadioStation>) {
        _favoriteStations.postValue(stations.filter { it.isFavorite })
    }

    fun filterByCategory(category: String) {
        currentCategory = category
        currentQuery = ""
        val stations = _allStations.value ?: return
        updateAllSection(stations)
    }

    /**
     * "Türler" (Genres) sekmesine özel filtre. "Tümü" sekmesinin durumuna
     * (currentCategory/currentQuery) ve LiveData'sına (_allSectionStations)
     * DOKUNMAZ — iki sekme artık tamamen bağımsızdır.
     */
    fun filterGenre(genre: String) {
        currentGenre = genre
        val stations = _allStations.value ?: return
        updateGenreSection(stations)
    }

    private fun updateGenreSection(stations: List<RadioStation>) {
        if (currentGenre.isEmpty()) return
        var result = stations.filter { st ->
            st.getTagList().any { tag -> tag.equals(currentGenre, ignoreCase = true) }
        }
        result = StationListOrganizer.sortByPriorityCountry(result, priorityCountries)
        try {
            _genreSectionStations.value = result
        } catch (_: Exception) {
            _genreSectionStations.postValue(result)
        }
    }

    fun search(query: String) {
        currentQuery = query.trim()
        currentCategory = CATEGORY_ALL
        val stations = _allStations.value
        if (stations.isNullOrEmpty()) {
            if (!dataLoaded) loadStations()
            return
        }
        updateAllSection(stations)
    }

    fun refreshMostListened() {
        val all = _allStations.value ?: return
        val counts = historyManager.getAllCounts()
        _playCounts.postValue(counts)
        val top = counts.entries
            .sortedByDescending { it.value }
            .take(50)
            .mapNotNull { (id, _) -> all.find { it.id == id } }
        _mostListenedStations.postValue(top)
    }

    fun recordPlay(station: RadioStation) {
        historyManager.increment(station.id)
        refreshMostListened()
    }

    fun toggleFavorite(station: RadioStation) {
        val isNowFav = repository.toggleFavorite(station)
        val updated = _allStations.value?.map { s ->
            if (s.id == station.id) s.copy(isFavorite = isNowFav) else s
        } ?: emptyList()
        try {
            _allStations.value = updated
        } catch (_: Exception) {
            _allStations.postValue(updated)
        }
        _favoritePayload.postValue(Pair(station.id, isNowFav))
        viewModelScope.launch {
            updateAllSection(updated)
            updateGenreSection(updated)
            updateFavorites(updated)
        }
    }

    private fun buildCountries(stations: List<RadioStation>) {
        val userCountries = LocaleCountryMapper.getDeviceCountries()
        val effectiveCountries = StationListOrganizer.resolveEffectiveCountries(
            stations = stations,
            deviceCountries = userCountries,
            fallbackCountries = LocaleCountryMapper.getFallbackCountries()
        )
        priorityCountries = effectiveCountries

        val summaries = StationListOrganizer.buildCountrySummaries(stations, effectiveCountries)
        _countrySummaries.postValue(summaries)
    }

    fun updatePlayerState(state: PlayerState) {
        _playerState.postValue(state)
    }

    fun playlistFor(station: RadioStation): List<RadioStation> {
        fun has(list: List<RadioStation>?) = list?.any { it.id == station.id } == true
        val active = _allSectionStations.value
        if (has(active)) return active!!
        val genre = _genreSectionStations.value
        if (has(genre)) return genre!!
        val fav = _favoriteStations.value
        if (has(fav)) return fav!!
        val top = _mostListenedStations.value
        if (has(top)) return top!!
        val all = _allStations.value
        return when {
            has(all) -> all!!
            !all.isNullOrEmpty() -> listOf(station) + all
            else -> listOf(station)
        }
    }

    fun getNextStation(current: RadioStation): RadioStation? {
        val list = playlistFor(current)
        if (list.isEmpty()) return null
        val idx = list.indexOfFirst { it.id == current.id }
        return if (idx >= 0 && idx < list.size - 1) list[idx + 1] else list.firstOrNull()
    }

    fun getPreviousStation(current: RadioStation): RadioStation? {
        val list = playlistFor(current)
        if (list.isEmpty()) return null
        val idx = list.indexOfFirst { it.id == current.id }
        return if (idx > 0) list[idx - 1] else list.lastOrNull()
    }

    fun getCurrentStationIndex(station: RadioStation): Int =
        playlistFor(station).indexOfFirst { it.id == station.id }

    fun getTotalCount(): Int = getActiveSectionList().size

    fun allStationsList(): List<RadioStation> = _allStations.value ?: emptyList()

    fun getActiveSectionList(): List<RadioStation> =
        _allSectionStations.value ?: emptyList()

    companion object {
        const val CATEGORY_ALL = "__ALL__"
        const val CATEGORY_FAVORITES = "__FAVORITES__"

        // Harf/rakam olmayan her şeyden böler (boşluk, virgül, parantez, tire vb.)
        // \p{L} ve \p{N} Unicode uyumludur; Türkçe (ı, ş, ğ, ö, ü, ç) ve diğer
        // dillerin harfleriyle de doğru çalışır.
        private val TOKEN_SPLIT_REGEX = Regex("[^\\p{L}\\p{N}]+")
    }
}