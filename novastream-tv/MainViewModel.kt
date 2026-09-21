package tv.garden.global.webapp

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ConsentType { NONE, PERSONALIZED, NON_PERSONALIZED }

interface ApiService {
    @GET
    suspend fun getChannels(@Url url: String): List<Channel>
}

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun clearAppCache(context: Context): Long {
    var totalFreed = 0L
    try {
        val cacheDir = context.cacheDir
        totalFreed += deleteDirContents(cacheDir)
        context.externalCacheDir?.let { totalFreed += deleteDirContents(it) }
        val coilCache = File(cacheDir, "image_cache")
        if (coilCache.exists()) totalFreed += deleteDirContents(coilCache)
    } catch (_: Exception) {
    }
    return totalFreed / 1024
}

private fun deleteDirContents(dir: File): Long {
    var freed = 0L
    dir.listFiles()?.forEach { file ->
        if (file.isDirectory) freed += deleteDirContents(file)
        freed += file.length()
        file.delete()
    }
    return freed
}

data class SkipResult(
    val neighbor: Channel?,
    val neighborIndex: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)

    var allChannels by mutableStateOf(listOf<Channel>())
        private set
    var displayedChannels by mutableStateOf(listOf<Channel>())
        private set
    var availableCountries by mutableStateOf(listOf<String>())
        private set
    var countryCounts by mutableStateOf(mapOf<String, Int>())
        private set
    var epgData by mutableStateOf(mapOf<String, String>())
        private set
    var blacklistedIds by mutableStateOf(
        prefs.getStringSet("blacklist_ids", emptySet())?.toSet() ?: emptySet()
    )
        private set
    var favSet by mutableStateOf(prefs.getStringSet("favs", emptySet()) ?: emptySet())
    var historyList by mutableStateOf(
        prefs.getString("history_ids", "")?.split(",")?.filter { it.isNotEmpty() } ?: listOf()
    )
    var isLoading by mutableStateOf(true)
    var isRefreshing by mutableStateOf(false)
    var hasNetworkError by mutableStateOf(false)
    var hasFetchError by mutableStateOf(false)
    var activeFilterMode by mutableStateOf("ALL")
    var selectedFilterName by mutableStateOf("")
    var searchQuery by mutableStateOf("")
    var lastChannelId by mutableStateOf(prefs.getString("last_channel_id", "") ?: "")
        private set
    var lastListIndex by mutableStateOf(prefs.getInt("last_list_index", 0))
        private set

    var adsConsentEnabled by mutableStateOf(prefs.getBoolean("ads_consent_enabled", false))
        private set
    var consentType by mutableStateOf(
        if (prefs.getBoolean("ads_consent_enabled", false)) ConsentType.PERSONALIZED
        else ConsentType.NONE
    )
    val hasConsent: Boolean get() = adsConsentEnabled

    private var filterJob: Job? = null
    private val gson = Gson()

    fun setConsent(type: ConsentType) {
        setAdsConsent(type == ConsentType.PERSONALIZED || type == ConsentType.NON_PERSONALIZED)
    }

    fun setAdsConsent(enabled: Boolean) {
        adsConsentEnabled = enabled
        consentType = if (enabled) ConsentType.PERSONALIZED else ConsentType.NONE
        prefs.edit()
            .putBoolean("ads_consent_enabled", enabled)
            .putString("consent_type", consentType.name)
            .putBoolean("gdpr_consent", enabled)
            .apply()
    }

    init {
        fetchChannels()
    }

    private val channelsCacheFile: File by lazy {
        File(getApplication<Application>().filesDir, "channels_cache.json")
    }

    private fun loadCachedChannels(): List<Channel>? = try {
        if (channelsCacheFile.exists()) {
            val json = channelsCacheFile.readText()
            if (json.isNotBlank()) {
                val type = object : TypeToken<List<Channel>>() {}.type
                gson.fromJson<List<Channel>>(json, type)
            } else null
        } else null
    } catch (_: Exception) {
        null
    }

    private fun saveCachedChannels(channels: List<Channel>) {
        try {
            channelsCacheFile.writeText(gson.toJson(channels))
        } catch (_: Exception) {
        }
    }

    private fun persistBlacklist() {
        prefs.edit().putStringSet("blacklist_ids", HashSet(blacklistedIds)).apply()
    }

    fun saveLastChannel(id: String) {
        lastChannelId = id
        prefs.edit().putString("last_channel_id", id).apply()
    }

    fun saveListIndex(index: Int) {
        lastListIndex = index.coerceAtLeast(0)
        prefs.edit().putInt("last_list_index", lastListIndex).apply()
    }

    private fun applyFetchedChannels(channels: List<Channel>) {
        val localCountryCodes = LanguageManager.getLocalCountryCodes()
            .map { CountryCatalog.resolve(it) }
            .toSet()
        val distinct = channels.distinctBy { it.id.ifBlank { it.url } }
        val (local, others) = distinct.partition { it.regionCode in localCountryCodes }
        val sorted = local + others
        allChannels = sorted
        rebuildCountryIndex(sorted)
        applyFilter(activeFilterMode, selectedFilterName, searchQuery, showSpinner = false)
    }

    private fun rebuildCountryIndex(channels: List<Channel>) {
        val counts = LinkedHashMap<String, Int>()
        channels.forEach { ch ->
            val code = ch.regionCode
            if (code.isNotBlank()) counts[code] = (counts[code] ?: 0) + 1
        }
        countryCounts = counts
        availableCountries = counts.entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    fun fetchChannels() {
        hasNetworkError = false
        hasFetchError = false
        val hadList = allChannels.isNotEmpty()
        if (!hadList) isLoading = true else isRefreshing = true
        viewModelScope.launch(Dispatchers.IO) {
            val cached = loadCachedChannels()
            if (!cached.isNullOrEmpty() && !hadList) {
                withContext(Dispatchers.Main) {
                    applyFetchedChannels(cached)
                    isLoading = false
                    generateMockEpg()
                }
            }

            if (!isNetworkAvailable(getApplication())) {
                if (cached.isNullOrEmpty() && !hadList) {
                    withContext(Dispatchers.Main) {
                        hasNetworkError = true
                        isLoading = false
                        isRefreshing = false
                    }
                } else {
                    withContext(Dispatchers.Main) { isRefreshing = false }
                }
                return@launch
            }

            try {
                val httpClient = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .callTimeout(25, TimeUnit.SECONDS)
                    .build()
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://placeholder.base/")
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
                val channels = try {
                    retrofit.getChannels(AppConfig.JSON_URL)
                } catch (_: Exception) {
                    retrofit.getChannels(AppConfig.JSON_FALLBACK_URL)
                }
                saveCachedChannels(channels)
                withContext(Dispatchers.Main) {
                    applyFetchedChannels(channels)
                    isLoading = false
                    isRefreshing = false
                    generateMockEpg()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (cached.isNullOrEmpty() && !hadList) {
                    withContext(Dispatchers.Main) {
                        hasFetchError = true
                        isLoading = false
                        isRefreshing = false
                    }
                } else {
                    withContext(Dispatchers.Main) { isRefreshing = false }
                }
            }
        }
    }

    fun reorderByLocale() {
        if (allChannels.isEmpty()) return
        val localCountryCodes = LanguageManager.getLocalCountryCodes()
            .map { CountryCatalog.resolve(it) }
            .toSet()
        val (local, others) = allChannels.partition { it.regionCode in localCountryCodes }
        val resorted = local + others
        if (resorted == allChannels) return
        allChannels = resorted
        applyFilter(activeFilterMode, selectedFilterName, searchQuery, showSpinner = false)
    }

    private fun generateMockEpg() {
        viewModelScope.launch(Dispatchers.Default) {
            val mock = mutableMapOf<String, String>()
            allChannels.forEach { ch ->
                val g = ch.group.lowercase(Locale.ROOT)
                mock[ch.id] = when {
                    "spor" in g || "sport" in g -> "⚽ ${LanguageManager.getTranslatedCategory("sports")}"
                    "haber" in g || "news" in g -> "📰 ${LanguageManager.getTranslatedCategory("news")}"
                    "belgesel" in g || "doc" in g -> "🦁 ${LanguageManager.getTranslatedCategory("documentary")}"
                    "çocuk" in g || "kid" in g || "cocuk" in g -> "🧸 ${LanguageManager.getTranslatedCategory("kids")}"
                    "sinema" in g || "movie" in g || "film" in g -> "🎬 ${LanguageManager.getTranslatedCategory("movies")}"
                    "müzik" in g || "music" in g || "muzik" in g -> "🎵 ${LanguageManager.getTranslatedCategory("music")}"
                    "dini" in g || "relig" in g || "islam" in g -> "🕌 ${LanguageManager.getTranslatedCategory("religious")}"
                    else -> "📺 ${LanguageManager.getTranslatedCategory("general")}"
                }
            }
            epgData = mock
        }
    }

    fun applyFilter(
        type: String,
        filterValue: String,
        query: String = "",
        showSpinner: Boolean = false
    ) {
        activeFilterMode = type
        selectedFilterName = filterValue
        searchQuery = query
        filterJob?.cancel()
        if (showSpinner && displayedChannels.isEmpty()) isLoading = true
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            delay(if (query.isNotEmpty()) 180 else 0)
            try {
                val q = query.lowercase(Locale.getDefault()).trim()
                val filtered = allChannels.filter { ch ->
                    if (blacklistedIds.contains(ch.id)) return@filter false
                    val matchQuery = q.isEmpty() || ch.name.lowercase(Locale.getDefault()).contains(q)
                    val matchType = when (type) {
                        "ALL" -> true
                        "FAV" -> favSet.contains(ch.url)
                        "HISTORY" -> historyList.contains(ch.id)
                        "COUNTRY" -> ch.regionCode == CountryCatalog.resolve(filterValue, filterValue)
                        else -> true
                    }
                    matchQuery && matchType
                }
                val final = if (type == "HISTORY")
                    filtered.sortedBy { historyList.indexOf(it.id) }.reversed()
                else filtered
                withContext(Dispatchers.Main) {
                    displayedChannels = final
                    if (showSpinner) isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    displayedChannels = emptyList()
                    if (showSpinner) isLoading = false
                }
            }
        }
    }

    fun addToHistory(channel: Channel) {
        val limited = ((historyList - channel.id) + channel.id).takeLast(40)
        historyList = limited
        prefs.edit().putString("history_ids", limited.joinToString(",")).apply()
    }

    fun toggleFavorite(channelUrl: String) {
        val newSet = if (favSet.contains(channelUrl)) favSet - channelUrl else favSet + channelUrl
        favSet = newSet
        prefs.edit().putStringSet("favs", newSet).apply()
        if (activeFilterMode == "FAV") applyFilter("FAV", "", searchQuery, showSpinner = false)
    }

    /**
     * Bozuk kanalı listeden çıkarır ama kaydırma konumunu sıfırlamaz.
     * Kullanıcı 20. sıradayken komşu olarak 19. kanal (önceki) döner.
     * İlk kanal bozuksa bir sonrakine geçilir — listenin başına sarılmaz.
     */
    fun hideBrokenKeepPlace(failedId: String): SkipResult {
        val list = displayedChannels
        val idx = list.indexOfFirst { it.id == failedId }
        val neighbor = when {
            idx > 0 -> list[idx - 1]
            idx == 0 && list.size > 1 -> list[1]
            else -> null
        }
        blacklistedIds = blacklistedIds + failedId
        persistBlacklist()
        val nextList = list.filter { it.id != failedId }
        displayedChannels = nextList
        val neighborIndex = neighbor?.let { n -> nextList.indexOfFirst { it.id == n.id } } ?: -1
        return SkipResult(neighbor, neighborIndex)
    }

    fun restoreHiddenChannels() {
        blacklistedIds = emptySet()
        persistBlacklist()
        applyFilter(activeFilterMode, selectedFilterName, searchQuery, showSpinner = false)
    }

    fun channelById(id: String?): Channel? =
        id?.let { allChannels.find { ch -> ch.id == it && ch.id !in blacklistedIds } }
}
