package com.example.mahjongmaster

/**
 * ============================================================
 *  REKLAM ANAHTARI (SWITCH) — TÜM REKLAM AYARLARI TEK YERDE
 * ============================================================
 *
 *  USE_YANDEX_AND_HUAWEI = true
 *      → Yandex + Huawei Petal reklamları kullanılır:
 *          • YANDEX_COUNTRIES listesindeki ülkelerde → YANDEX reklamları
 *          • Listede OLMAYAN tüm ülkelerde           → HUAWEI PETAL reklamları
 *
 *  USE_YANDEX_AND_HUAWEI = false
 *      → Yandex ve Huawei tamamen devre dışı kalır,
 *        TÜM ülkelerde yalnızca GOOGLE ADMOB reklamları gösterilir.
 *
 *  Reklam birim (unit) ID'leri: res/values/strings.xml
 *  (yandex_*, huawei_*, admob_* satırları)
 * ============================================================
 */
object AdConfig {

    const val USE_YANDEX_AND_HUAWEI = true

    /** Bu ülkelerde Yandex, diğer tüm ülkelerde Huawei Petal reklamları gösterilir. */
    val YANDEX_COUNTRIES: Set<String> = setOf(
        "RU", "TR", "KZ", "BY", "UZ", "AM", "KG", "AZ", "TJ", "GE", "MD", "RS"
    )

    /**
     * Sadece TEST için: ülke tespitini elle geçersiz kılar (ör. "RU" → Yandex,
     * "DE" → Huawei). Yalnızca debug (Android Studio'dan çalıştırılan) sürümlerde
     * etkilidir; release/mağaza sürümünde yok sayılır. Normalde null kalmalıdır.
     */
    val DEBUG_FORCE_COUNTRY: String? = null
}
