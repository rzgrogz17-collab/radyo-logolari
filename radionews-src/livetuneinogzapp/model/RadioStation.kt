package com.globalradio.livetuneinogzapp.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadioStation(
    @SerializedName("stationuuid") val id: String = "",
    @SerializedName("name")        val name: String = "",
    @SerializedName("url")         val url: String = "",
    @SerializedName("url_resolved") val urlResolved: String = "",
    @SerializedName("favicon")     val favicon: String? = null,
    @SerializedName("country")     val country: String = "",
    @SerializedName(
        value = "countrycode",
        alternate = ["countryCode", "country_code", "iso", "iso2"]
    ) val countryCode: String = "",
    @SerializedName("tags")        val tags: String = "",
    var isFavorite: Boolean = false
) : Parcelable {
    /** Oynatma URL'si – url_resolved öncelikli, yoksa url kullanılır */
    fun getStreamUrl(): String =
        if (!urlResolved.isNullOrBlank() && urlResolved != url) urlResolved else url

    /** Etiketleri liste olarak döner */
    fun getTagList(): List<String> =
        tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /** Geçerli favicon URL'si var mı? */
    fun hasValidFavicon(): Boolean =
        !favicon.isNullOrBlank() &&
        favicon != "null" &&
        !favicon.startsWith("data:image") &&
        (favicon.startsWith("http://") || favicon.startsWith("https://"))

    /** İlk harflerden avatar metni üret (logo yoksa gösterilir) */
    fun getAvatarText(): String {
        val parts = name.trim().split(" ").filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "R"
        }
    }

    /** Akış türünü tahmin et */
    fun isHlsStream(): Boolean = getStreamUrl().contains(".m3u8", ignoreCase = true)
}