# Add project specific ProGuard rules here.

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Huawei Ads ---
-keep class com.huawei.openalliance.ad.** { *; }
-keep class com.huawei.hms.ads.** { *; }

# --- Google AdMob / UMP ---
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

# --- Coil (SVG image loading) ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# --- Compose ---
-dontwarn androidx.compose.**

# --- Data classes (keep for SharedPreferences serialization) ---
-keep class com.example.mahjongmaster.Tile { *; }
-keep class com.example.mahjongmaster.MoveRecord { *; }


# --- Yandex Mobile Ads ---
-keep class com.yandex.mobile.ads.** { *; }
-keep class com.yandex.mobile.ads.banner.** { *; }
-keep class com.yandex.mobile.ads.interstitial.** { *; }
-keep class com.yandex.mobile.ads.rewarded.** { *; }
-keep class com.yandex.mobile.ads.common.** { *; }
-dontwarn com.yandex.mobile.ads.**
-dontwarn com.yandex.mobile.**
