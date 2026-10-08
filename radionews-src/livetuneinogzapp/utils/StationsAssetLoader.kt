package com.globalradio.livetuneinogzapp.utils

import android.content.Context
import com.globalradio.livetuneinogzapp.model.RadioStation
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Uygulama içindeki stations.json dosyasını satır satır okur. Tek bozuk kayıt listeyi düşürmez. */
object StationsAssetLoader {

    fun load(context: Context): List<RadioStation> {
        return try {
            val text = context.assets.open("stations.json").bufferedReader().use { it.readText() }
            @Suppress("DEPRECATION")
            val array = JsonParser().parse(text).asJsonArray
            array.mapNotNull { element ->
                try {
                    if (!element.isJsonObject) return@mapNotNull null
                    parse(element.asJsonObject)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parse(o: JsonObject): RadioStation? {
        val id = text(o, "stationuuid")
        val name = text(o, "name")
        if (id.isBlank() || name.isBlank()) return null
        val votes = o.get("votes")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: 0
        return RadioStation(
            id = id,
            name = name,
            url = text(o, "url"),
            urlResolved = text(o, "url_resolved"),
            favicon = text(o, "favicon").ifBlank { null },
            country = text(o, "country"),
            countryCode = text(o, "countrycode").ifBlank { text(o, "countryCode") },
            tags = text(o, "tags"),
            votes = votes
        )
    }

    private fun text(o: JsonObject, key: String): String {
        val value = o.get(key) ?: return ""
        if (!value.isJsonPrimitive || !value.asJsonPrimitive.isString) return ""
        return value.asString.trim()
    }
}
