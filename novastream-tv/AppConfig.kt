package tv.garden.global.webapp

object AppConfig {
    const val JSON_URL = "https://premiumtvlive.rzgrogz17.workers.dev/api/channels"
    const val JSON_FALLBACK_URL =
        "https://raw.githubusercontent.com/rzgrogz17-collab/PremiumTv/refs/heads/main/channels_fixed.json"
    const val PRIVACY_POLICY_URL = "https://novastreamlivetvplayer.blogspot.com/"
    const val YANDEX_BANNER_ID = "R-M-19538844-1"
    const val YANDEX_INTERSTITIAL_ID = "R-M-19538844-2"
    const val YANDEX_REWARDED_ID = "R-M-19538844-3"
    const val PREFS_NAME = "DiamondPrefs"
    // Reklam rızası yalnızca buradan yönetilir (Ayarlar'da gösterilmez).
    // true = kişiselleştirilmiş reklamlar açık, false = kapalı.
    const val ADS_CONSENT_ENABLED = false
    const val PLAYBACK_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
}
