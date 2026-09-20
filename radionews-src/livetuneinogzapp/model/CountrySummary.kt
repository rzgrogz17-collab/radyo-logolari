package com.globalradio.livetuneinogzapp.model

import com.globalradio.livetuneinogzapp.utils.CountryFlags

data class CountrySummary(
    val name: String,
    val stationCount: Int,
    val isoCode: String? = CountryFlags.resolveIso(name)
) {
    val flagEmoji: String = isoCode?.let { CountryFlags.emoji(it) }
        ?: CountryFlags.emojiFor(name)

    val flagImageUrl: String? = isoCode?.let { CountryFlags.imageUrl(it) }

    companion object {
        fun getFlagEmoji(countryName: String): String = CountryFlags.emojiFor(countryName)
    }
}
