package com.globalradio.livetuneinogzapp.utils

import java.text.Normalizer
import java.util.Locale

/**
 * Ülke adı / ISO kodundan bayrak üretir.
 *
 * Yeni bir ülke `stations.json` içine eklendiğinde:
 *  1. `countrycode` / `iso` alanı varsa doğrudan kullanılır
 *  2. Yoksa ülke adı, cihaz Locale ISO listesi + yaygın takma adlarla eşleştirilir
 * Böylece hardcoded haritaya her ülkeyi tek tek eklemek gerekmez.
 */
object CountryFlags {

    fun resolveIso(countryName: String?, countryCode: String? = null): String? {
        val fromCode = sanitizeIso(countryCode)
        if (fromCode != null) return fromCode

        val name = countryName?.trim().orEmpty()
        if (name.isEmpty()) return null

        sanitizeIso(name)?.let { return it }

        val key = normalizeName(name)
        if (key.isEmpty()) return null
        ALIASES[key]?.let { return it }
        return displayNameIndex[key]
    }

    fun imageUrl(iso: String): String =
        "https://flagcdn.com/w160/${iso.lowercase(Locale.US)}.png"

    fun emoji(iso: String): String {
        if (iso.length != 2) return "🌍"
        return iso.uppercase(Locale.US).map { c ->
            String(Character.toChars(0x1F1E6 - 'A'.code + c.code))
        }.joinToString("")
    }

    fun emojiFor(countryName: String?, countryCode: String? = null): String {
        val iso = resolveIso(countryName, countryCode) ?: return "🌍"
        return emoji(iso)
    }

    fun groupKey(countryName: String, countryCode: String? = null): String {
        val iso = resolveIso(countryName, countryCode)
        return iso ?: normalizeName(countryName)
    }

    fun normalizeName(name: String): String {
        val trimmed = name.trim().lowercase(Locale.US)
        if (trimmed.isEmpty()) return ""
        val decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
        return decomposed
            .replace(DIACRITICS, "")
            .replace(NON_ALNUM, " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun sanitizeIso(raw: String?): String? {
        val code = raw?.trim()?.uppercase(Locale.US) ?: return null
        if (code.length != 2) return null
        if (!code[0].isLetter() || !code[1].isLetter()) return null
        return code
    }

    private val DIACRITICS = Regex("\\p{M}+")
    private val NON_ALNUM = Regex("[^a-z0-9]+")

    private val displayNameIndex: Map<String, String> by lazy {
        val map = HashMap<String, String>(512)
        val locales = listOf(
            Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN, Locale.ITALIAN,
            Locale("es"), Locale("pt"), Locale("tr"), Locale("ru"), Locale("nl"),
            Locale("pl"), Locale("ar"), Locale.JAPANESE, Locale.CHINESE, Locale.KOREAN,
            Locale.getDefault()
        )
        for (iso in Locale.getISOCountries()) {
            for (lang in locales) {
                val display = Locale("", iso).getDisplayCountry(lang)
                val key = normalizeName(display)
                if (key.isNotEmpty()) map.putIfAbsent(key, iso)
            }
        }
        map
    }

    // RadioBrowser / JSON'da sık görülen resmi olmayan yazımlar
    private val ALIASES: Map<String, String> = hashMapOf(
        "turkey" to "TR",
        "turkiye" to "TR",
        "the united states of america" to "US",
        "united states" to "US",
        "united states of america" to "US",
        "usa" to "US",
        "us" to "US",
        "america" to "US",
        "russia" to "RU",
        "russian federation" to "RU",
        "great britain" to "GB",
        "united kingdom" to "GB",
        "uk" to "GB",
        "england" to "GB",
        "holland" to "NL",
        "the netherlands" to "NL",
        "netherlands" to "NL",
        "czech republic" to "CZ",
        "czechia" to "CZ",
        "korea" to "KR",
        "south korea" to "KR",
        "republic of korea" to "KR",
        "north korea" to "KP",
        "uae" to "AE",
        "united arab emirates" to "AE",
        "bosnia" to "BA",
        "bosnia and herzegovina" to "BA",
        "macedonia" to "MK",
        "north macedonia" to "MK",
        "ivory coast" to "CI",
        "cote d ivoire" to "CI",
        "cote divoire" to "CI",
        "congo" to "CG",
        "democratic republic of the congo" to "CD",
        "drc" to "CD",
        "tanzania" to "TZ",
        "syria" to "SY",
        "syrian arab republic" to "SY",
        "iran" to "IR",
        "islamic republic of iran" to "IR",
        "venezuela" to "VE",
        "bolivia" to "BO",
        "moldova" to "MD",
        "republic of moldova" to "MD",
        "vietnam" to "VN",
        "viet nam" to "VN",
        "laos" to "LA",
        "lao" to "LA",
        "brunei" to "BN",
        "palestine" to "PS",
        "state of palestine" to "PS",
        "kosovo" to "XK",
        "taiwan" to "TW",
        "hong kong" to "HK",
        "macau" to "MO",
        "macao" to "MO",
        "myanmar" to "MM",
        "burma" to "MM",
        "swaziland" to "SZ",
        "eswatini" to "SZ",
        "cape verde" to "CV",
        "cabo verde" to "CV",
        "east timor" to "TL",
        "timor leste" to "TL",
        "vatican" to "VA",
        "vatican city" to "VA",
        "holy see" to "VA"
    )
}
