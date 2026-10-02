package tv.garden.global.webapp

import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Ülke/bölge kodlarını bayrak görsellerine çevirir.
 *
 * Yeni bir ülke JSON'a eklendiğinde ekstra kod yazmaya gerek kalmaması için:
 *  - Android'in ISO 3166-1 listesindeki her 2 harfli kod otomatik tanınır
 *  - 3 harfli ISO kodları 2 harfe çevrilir
 *  - Ülke adları (TR/EN/DE/FR/ES/PT/RU/AR/IT/ZH) otomatik eşlenir
 *  - Bilinmeyen 2 harfli kodlar yine flagcdn.com üzerinden denenir
 *  - INT / International → BM (UN) bayrağı, yüklenemezse küre ikonu
 */
object CountryCatalog {

    private val iso2: Set<String> by lazy {
        Locale.getISOCountries().map { it.uppercase(Locale.ROOT) }.toSet() +
            setOf("XK", "EU", "UN", "INT")
    }

    private val aliasMapRef = AtomicReference<Map<String, String>?>(null)

    private val genreTokens = setOf(
        "undefined", "haber", "news", "muzik", "müzik", "music", "ulusal", "national",
        "yerel", "eglence", "eğlence", "entertainment", "spor", "sports", "sport",
        "dini", "religious", "islamic", "belgesel", "documentary", "doc", "cocuk",
        "çocuk", "kids", "dizi", "series", "ekonomi", "business", "kültür", "kultur",
        "culture", "genel", "general", "yurt disi", "yurt dışı", "sinema", "film",
        "movies", "lifestyle", "yaşam", "yemek"
    )

    private val continentTokens = setOf("asya", "asia", "amerika", "afrika", "africa", "europe", "avrupa")

    /** flagcdn.com slug'ları — INT için UN bayrağı kullanılır. */
    private val flagSlugOverrides = mapOf(
        "INT" to "un",
        "INTL" to "un",
        "XX" to "un",
        "UN" to "un",
        "WORLD" to "un",
        "GLOBAL" to "un",
        "UK" to "gb",
        "EN" to "gb",
        "ENG" to "gb",
        "GBR" to "gb",
        "EU" to "eu",
        "XK" to "xk",
        "KOS" to "xk",
        "KV" to "xk",
        "EL" to "gr",
        "UAE" to "ae",
        "USA" to "us",
        "TUR" to "tr",
        "AZE" to "az",
        "GER" to "de",
        "DEU" to "de",
        "NED" to "nl",
        "SUI" to "ch",
        "POR" to "pt",
        "RSA" to "za",
        "KOR" to "kr",
        "JPN" to "jp",
        "CHI" to "cn"
    )

    private val extraAliases = listOf(
        "turkiye" to "TR",
        "türkiye" to "TR",
        "turkey" to "TR",
        "amerika" to "US",
        "usa" to "US",
        "united states of america" to "US",
        "england" to "GB",
        "great britain" to "GB",
        "uk" to "GB",
        "united kingdom" to "GB",
        "rusya" to "RU",
        "azerbaycan" to "AZ",
        "holland" to "NL",
        "ivory coast" to "CI",
        "cote divoire" to "CI",
        "côte d'ivoire" to "CI",
        "palestine" to "PS",
        "czechia" to "CZ",
        "czech republic" to "CZ",
        "south korea" to "KR",
        "korea" to "KR",
        "north korea" to "KP",
        "hong kong" to "HK",
        "macao" to "MO",
        "macau" to "MO",
        "taiwan" to "TW",
        "vietnam" to "VN",
        "laos" to "LA",
        "moldova" to "MD",
        "north macedonia" to "MK",
        "macedonia" to "MK",
        "kosovo" to "XK",
        "international" to "INT",
        "int" to "INT",
        "world" to "INT",
        "global" to "INT",
        "dominican republic" to "DO",
        "saudi arabia" to "SA",
        "united arab emirates" to "AE",
        "south africa" to "ZA",
        "new zealand" to "NZ",
        "sri lanka" to "LK",
        "bosnia and herzegovina" to "BA",
        "el salvador" to "SV",
        "costa rica" to "CR",
        "democratic republic of the congo" to "CD",
        "republic of the congo" to "CG",
        "bonaire" to "BQ",
        "sint maarten" to "SX",
        "curacao" to "CW",
        "curaçao" to "CW",
        "faroe islands" to "FO",
        "u.s. virgin islands" to "VI",
        "british virgin islands" to "VG",
        "puerto rico" to "PR",
        "trinidad and tobago" to "TT",
        "papua new guinea" to "PG",
        "equatorial guinea" to "GQ",
        "french guiana" to "GF",
        "western sahara" to "EH",
        "saint kitts and nevis" to "KN",
        "saint lucia" to "LC"
    )

    fun fold(raw: String): String {
        val lower = raw.trim().lowercase(Locale.ROOT)
        val sb = StringBuilder(lower.length)
        for (ch in lower) {
            sb.append(
                when (ch) {
                    'ı', 'ì', 'í', 'î' -> 'i'
                    'ş', 'ś' -> 's'
                    'ğ' -> 'g'
                    'ü', 'ú', 'ù', 'û' -> 'u'
                    'ö', 'ó', 'ò', 'ô' -> 'o'
                    'ç', 'ć' -> 'c'
                    'â' -> 'a'
                    'é', 'è' -> 'e'
                    'ñ' -> 'n'
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    fun normalize(raw: String): String = raw.trim().uppercase(Locale.ROOT)

    private fun aliases(): Map<String, String> {
        aliasMapRef.get()?.let { return it }
        val map = HashMap<String, String>(1024)
        fun putAlias(name: String, code: String) {
            val key = fold(name)
            if (key.isNotEmpty()) map.putIfAbsent(key, code)
        }
        for (code in iso2) {
            putAlias(code, code)
            if (code == "INT" || code == "EU" || code == "UN") continue
            val loc = Locale("", code)
            putAlias(loc.getDisplayCountry(Locale.ENGLISH), code)
            putAlias(loc.getDisplayCountry(Locale("tr")), code)
            putAlias(loc.getDisplayCountry(Locale.FRENCH), code)
            putAlias(loc.getDisplayCountry(Locale.GERMAN), code)
            putAlias(loc.getDisplayCountry(Locale("es")), code)
            putAlias(loc.getDisplayCountry(Locale("pt")), code)
            putAlias(loc.getDisplayCountry(Locale("ru")), code)
            putAlias(loc.getDisplayCountry(Locale("ar")), code)
            putAlias(loc.getDisplayCountry(Locale.ITALIAN), code)
            putAlias(loc.getDisplayCountry(Locale.CHINESE), code)
            try {
                val iso3 = loc.isO3Country
                if (!iso3.isNullOrBlank()) putAlias(iso3, code)
            } catch (_: Exception) {
            }
        }
        extraAliases.forEach { (name, code) -> putAlias(name, code) }
        aliasMapRef.compareAndSet(null, map)
        return aliasMapRef.get() ?: map
    }

    fun isGenre(group: String): Boolean {
        val f = fold(group)
        if (f.isEmpty()) return false
        if (f in continentTokens) return false
        return f in genreTokens || f.contains("yurt disi") || f.contains("yurt disi") ||
            f.contains("yurt dışı")
    }

    fun isoFromAny(raw: String): String? {
        val n = normalize(raw)
        if (n.isEmpty() || n == "UNDEFINED") return null
        when (n) {
            "INT", "INTL", "XX", "WORLD", "GLOBAL", "INTERNATIONAL" -> return "INT"
            "UK", "EN", "ENG", "GBR" -> return "GB"
            "EU", "EUROPE" -> return "EU"
            "XK", "KOS", "KV" -> return "XK"
            "UN" -> return "UN"
        }
        if (n.length == 2 && n.all { it.isLetter() }) {
            return n
        }
        aliases()[fold(raw)]?.let { return it }
        if (n.length == 3) {
            for (code in iso2) {
                if (code.length != 2) continue
                try {
                    if (Locale("", code).isO3Country.equals(n, ignoreCase = true)) return code
                } catch (_: Exception) {
                }
            }
        }
        return null
    }

    fun resolve(country: String, group: String = ""): String {
        val groupCode = isoFromAny(group)
        if (groupCode != null && !isGenre(group) && fold(group) !in continentTokens) {
            return groupCode
        }
        isoFromAny(country)?.let { return it }
        return "INT"
    }

    /**
     * flagcdn.com ISO slug. INT ve tanınmayan bölgeler UN/globe'a düşer.
     * Yeni 2 harfli ülke kodları (ör. "NG", "SE") haritada olmasa bile
     * doğrudan flagcdn'e gönderilir — yeni ülke eklemek yeterlidir.
     */
    fun flagCdnSlug(country: String, group: String = ""): String {
        val resolved = resolve(country, group)
        flagSlugOverrides[resolved]?.let { return it }
        flagSlugOverrides[normalize(country)]?.let { return it }
        if (resolved.length == 2 && resolved.all { it.isLetter() }) {
            return resolved.lowercase(Locale.ROOT)
        }
        val raw = normalize(country)
        if (raw.length == 2 && raw.all { it.isLetter() }) {
            return raw.lowercase(Locale.ROOT)
        }
        return "un"
    }

    fun flagUrl(country: String, group: String = "", width: Int = 160): String {
        val w = width.coerceIn(20, 640)
        return "https://flagcdn.com/w$w/${flagCdnSlug(country, group)}.png"
    }

    fun displayName(country: String, group: String = ""): String {
        val code = resolve(country, group)
        return when (code) {
            "INT" -> LanguageManager.international
            "EU" -> LanguageManager.europe
            "UN" -> LanguageManager.international
            "XK" -> LanguageManager.kosovo
            else -> {
                val name = Locale("", code).displayCountry
                if (name.isNotBlank() && !name.equals(code, ignoreCase = true)) name else code
            }
        }
    }

    fun isInternational(code: String): Boolean =
        resolve(code) == "INT"
}
