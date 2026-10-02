package com.globalradio.livetuneinogzapp.utils

import com.globalradio.livetuneinogzapp.model.CountrySummary
import com.globalradio.livetuneinogzapp.model.RadioStation

object StationListOrganizer {

    val DEFAULT_FALLBACK_PRIORITY = listOf(
        "Turkey", "United States", "The United States Of America",
        "Germany", "France", "United Kingdom", "Italy", "Spain", "Russia", "Brazil"
    )

    fun resolveEffectiveCountries(
        stations: List<RadioStation>,
        deviceCountries: List<String>,
        fallbackCountries: List<String>
    ): List<String> {
        val availableCountries = stations.map { it.country }
        val hasDeviceCountryData = deviceCountries.any { dc ->
            availableCountries.any { it.equals(dc, ignoreCase = true) } ||
                CountryFlags.resolveIso(dc)?.let { iso ->
                    stations.any { CountryFlags.resolveIso(it.country, it.countryCode) == iso }
                } == true
        }
        return if (hasDeviceCountryData) deviceCountries else fallbackCountries
    }

    fun sortByPriorityCountry(
        stations: List<RadioStation>,
        priorityCountries: List<String>
    ): List<RadioStation> {
        val priorityKeys = priorityCountries.map { CountryFlags.groupKey(it) }.toHashSet()
        val (priority, others) = stations.partition { st ->
            priorityKeys.contains(CountryFlags.groupKey(st.country, st.countryCode)) ||
                priorityCountries.any { it.equals(st.country, ignoreCase = true) }
        }
        return priority + others
    }

    fun buildCountrySummaries(
        stations: List<RadioStation>,
        effectiveCountries: List<String>,
        extraFallback: List<String> = DEFAULT_FALLBACK_PRIORITY
    ): List<CountrySummary> {
        data class Bucket(
            val names: MutableMap<String, Int> = mutableMapOf(),
            var count: Int = 0,
            var iso: String? = null
        )

        val buckets = LinkedHashMap<String, Bucket>()
        for (st in stations) {
            val name = st.country.trim()
            if (name.isEmpty()) continue
            val iso = CountryFlags.resolveIso(name, st.countryCode)
            val key = iso ?: CountryFlags.normalizeName(name)
            val bucket = buckets.getOrPut(key) { Bucket(iso = iso) }
            bucket.names[name] = (bucket.names[name] ?: 0) + 1
            bucket.count++
            if (bucket.iso == null) bucket.iso = iso
        }

        fun displayName(bucket: Bucket): String =
            bucket.names.maxByOrNull { it.value }?.key ?: bucket.names.keys.first()

        val priorityKeys = LinkedHashSet<String>()
        for (n in effectiveCountries + extraFallback) {
            val trimmed = n.trim()
            if (trimmed.isEmpty()) continue
            priorityKeys.add(CountryFlags.groupKey(trimmed))
        }

        val used = mutableSetOf<String>()
        val priority = priorityKeys.mapNotNull { key ->
            val bucket = buckets[key] ?: return@mapNotNull null
            used.add(key)
            CountrySummary(
                name = displayName(bucket),
                stationCount = bucket.count,
                isoCode = bucket.iso
            )
        }
        val rest = buckets
            .filter { it.key !in used }
            .values
            .sortedByDescending { it.count }
            .map {
                CountrySummary(
                    name = displayName(it),
                    stationCount = it.count,
                    isoCode = it.iso
                )
            }

        return priority + rest
    }
}
