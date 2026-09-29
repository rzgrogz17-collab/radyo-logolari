package com.example.satranc

/**
 * ============================================================
 *  REKLAM ANAHTARI — TÜM REKLAM AĞLARI TEK YERDEN
 * ============================================================
 *
 *  USE_YANDEX_AND_HUAWEI = true
 *      Yandex + Huawei Petal kullanılır:
 *        • YANDEX_COUNTRIES içindeki ülkeler → Yandex
 *        • Listedeki ülkelerin DIŞI        → Huawei Petal
 *
 *  USE_YANDEX_AND_HUAWEI = false
 *      Yandex ve Huawei hiç gösterilmez.
 *      Tüm ülkelerde yalnızca Google AdMob gösterilir.
 *
 *  Birim kimlikleri: res/values/strings.xml
 *  (yandex_*, huawei_*, admob_*)
 * ============================================================
 */
object AdConfig {

    const val USE_YANDEX_AND_HUAWEI = true

    /**
     * Alt şerit büyük banner yüksekliği (dp).
     * Standart banner 50dp'dir; bir standart banner kadar yukarı uzatılmış hali 100dp'dir.
     */
    const val LARGE_BANNER_HEIGHT_DP = 100

    /** Bu ülkelerde Yandex, diğer tüm ülkelerde Huawei Petal. */
    val YANDEX_COUNTRIES: Set<String> = setOf(
        "RU", "TR", "KZ", "BY", "UZ", "AM", "KG", "AZ", "TJ", "GE", "MD", "RS"
    )

    /**
     * Yalnızca debug derlemede ülke tespitini elle ezer ("RU" → Yandex, "DE" → Huawei).
     * Release sürümünde yok sayılır. Normalde null kalır.
     */
    val DEBUG_FORCE_COUNTRY: String? = null
}
