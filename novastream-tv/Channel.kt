package tv.garden.global.webapp

data class Channel(
    val id: String = "",
    val name: String = "",
    val logo: String = "",
    val url: String = "",
    val group: String = "",
    val country: String = "",
    val is_premium: Boolean = false,
    val quality: String = ""
) {
    val regionCode: String
        get() = CountryCatalog.resolve(country, group)
}

fun detectQuality(channel: Channel): String {
    val url = channel.url.lowercase()
    val name = channel.name.lowercase()
    val quality = channel.quality.uppercase()
    return when {
        quality.isNotEmpty() -> quality
        "4k" in url || "4k" in name || "uhd" in name -> "4K"
        "fhd" in url || "1080" in url || "fhd" in name -> "FHD"
        "hd" in url || "720" in url || "hd" in name -> "HD"
        "sd" in url || "480" in url || "360" in url || "sd" in name -> "SD"
        else -> ""
    }
}
