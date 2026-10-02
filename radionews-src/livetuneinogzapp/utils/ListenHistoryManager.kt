package com.globalradio.livetuneinogzapp.utils

import android.content.Context
import com.globalradio.livetuneinogzapp.data.AppDatabase
import com.globalradio.livetuneinogzapp.data.PlayCountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Her istasyonun kaç kez çalındığını artık Room (SQLite) üzerinde saklar.
 * Eskiden SharedPreferences üzerinde `prefs.all` ile tüm anahtarlar taranıyordu;
 * bu, geçmiş büyüdükçe yavaşlayan bir yaklaşımdı. Room ile sorgular indeksli
 * ve doğrudan SQL üzerinden çalışır.
 *
 * Dışa açılan API bilinçli olarak senkron bırakıldı (runBlocking ile IO'ya
 * geçilip sonucu bekliyor); böylece mevcut tüm çağıran kodlar
 * (MainViewModel, SettingsActivity) hiçbir değişiklik yapmadan çalışmaya
 * devam eder.
 */
class ListenHistoryManager(context: Context) {

    private val dao = AppDatabase.getInstance(context).playCountDao()
    private val legacyPrefs = context.getSharedPreferences("listen_history", Context.MODE_PRIVATE)
    private val migrationPrefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)

    init {
        migrateLegacyDataIfNeeded()
    }

    /** Belirtilen istasyonun sayacını 1 artır */
    fun increment(stationId: String): Unit = runBlocking(Dispatchers.IO) {
        val current = dao.getCount(stationId) ?: 0
        dao.upsert(PlayCountEntity(stationId, current + 1))
    }

    fun importCounts(counts: Map<String, Int>): Unit = runBlocking(Dispatchers.IO) {
        counts.forEach { (id, count) ->
            if (id.isBlank() || count <= 0) return@forEach
            val current = dao.getCount(id) ?: 0
            dao.upsert(PlayCountEntity(id, maxOf(current, count)))
        }
    }

    /** İstasyonun çalınma sayısını döner */
    fun getCount(stationId: String): Int = runBlocking(Dispatchers.IO) {
        dao.getCount(stationId) ?: 0
    }

    /**
     * Tüm istasyon ID'lerini çalınma sayılarıyla birlikte döner.
     * Map<stationId, playCount> — sadece count > 0 olanlar
     */
    fun getAllCounts(): Map<String, Int> = runBlocking(Dispatchers.IO) {
        dao.getAll().associate { it.stationId to it.count }
    }

    /**
     * En çok çalınan N istasyonun ID'lerini sıralı döner (azalan).
     */
    fun getTopStationIds(limit: Int = 30): List<String> =
        getAllCounts()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }

    /** Geçmişi temizle */
    fun clearAll(): Unit = runBlocking(Dispatchers.IO) {
        dao.clearAll()
    }

    /** Eski SharedPreferences verisini (varsa) Room'a bir kereye mahsus taşır. */
    private fun migrateLegacyDataIfNeeded() {
        if (migrationPrefs.getBoolean(KEY_MIGRATED, false)) return
        val legacyCounts = mutableMapOf<String, Int>()
        legacyPrefs.all.forEach { (key, value) ->
            if (key.startsWith(PREFIX) && value is Int && value > 0) {
                legacyCounts[key.removePrefix(PREFIX)] = value
            }
        }
        if (legacyCounts.isNotEmpty()) {
            runBlocking(Dispatchers.IO) {
                legacyCounts.forEach { (id, count) -> dao.upsert(PlayCountEntity(id, count)) }
            }
        }
        migrationPrefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    companion object {
        private const val PREFIX = "play_count_"
        private const val KEY_MIGRATED = "history_migrated_to_room"
    }
}
