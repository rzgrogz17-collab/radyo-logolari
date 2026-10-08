-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.huawei.openalliance.ad.** { *; }
-keep class com.huawei.hms.ads.** { *; }

-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

-keep class com.yandex.mobile.ads.** { *; }
-dontwarn com.yandex.mobile.ads.**
-dontwarn com.yandex.mobile.**

-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn androidx.compose.**
