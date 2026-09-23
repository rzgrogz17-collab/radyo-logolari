package com.globalradio.livetuneinogzapp.utils

import android.content.Context
import com.globalradio.livetuneinogzapp.data.AppDatabase
import com.globalradio.livetuneinogzapp.data.FavoriteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Favori istasyonları artık Room (SQLite) üzerinde saklar.
 *
 * Dışa açılan API bilinçli olarak senkron bırakıldı (runBlocking ile IO'ya
 * geçilip sonucu bekliyor); böylece mevcut tüm çağıran kodlar
 * (CountryStationsActivity, StationRepository, vb.) hiçbir değişiklik
 * yapmadan çalışmaya devam eder. Favoriler tablosu küçük olduğu için bu
 * kısa bekleme pratikte hissedilmez.
 */
class FavoritesManager(context: Context) {

    private val dao = AppDatabase.getInstance(context).favoriteDao()
    private val legacyPrefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val migrationPrefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)

    init {
        migrateLegacyDataIfNeeded()
    }

    fun getFavoriteIds(): Set<String> = runBlocking(Dispatchers.IO) {
        dao.getAllIds().toSet()
    }

    fun isFavorite(stationId: String): Boolean = runBlocking(Dispatchers.IO) {
        dao.exists(stationId)
    }

    /**
     * Favoriye ekle / çıkar. True döner = artık favori, False = çıkarıldı.
     */
    fun toggleFavorite(stationId: String): Boolean = runBlocking(Dispatchers.IO) {
        val isFav = dao.exists(stationId)
        if (isFav) {
            dao.deleteById(stationId)
            false
        } else {
            dao.insert(FavoriteEntity(stationId))
            true
        }
    }

    fun addAll(ids: Collection<String>): Unit = runBlocking(Dispatchers.IO) {
        ids.filter { it.isNotBlank() }.forEach { dao.insert(FavoriteEntity(it)) }
    }

    /** Eski SharedPreferences verisini (varsa) Room'a bir kereye mahsus taşır. */
    private fun migrateLegacyDataIfNeeded() {
        if (migrationPrefs.getBoolean(KEY_MIGRATED, false)) return
        val legacyIds = legacyPrefs.getStringSet(LEGACY_KEY, emptySet()) ?: emptySet()
        if (legacyIds.isNotEmpty()) {
            runBlocking(Dispatchers.IO) {
                legacyIds.forEach { id -> dao.insert(FavoriteEntity(id)) }
            }
        }
        migrationPrefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    companion object {
        private const val LEGACY_KEY = "favorite_ids"
        private const val KEY_MIGRATED = "favorites_migrated_to_room"
    }
}
