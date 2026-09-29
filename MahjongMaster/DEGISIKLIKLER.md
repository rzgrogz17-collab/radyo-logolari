# Mahjong Master — değişiklik özeti

Bu klasör, orijinal Mahjong Classic Game projesinin yeniden tasarlanmış
ve reklam yönlendirmesi eklenmiş sürümüdür (sürüm 2.31 / versionCode 31).

## Reklam anahtarı (en önemli ayar)

Dosya: `app/src/main/java/com/example/mahjongmaster/AdConfig.kt`

```
USE_YANDEX_AND_HUAWEI = true
```

- **true** → listedeki ülkelerde Yandex, diğer tüm ülkelerde Huawei Petal Ads
- **false** → tüm ülkelerde yalnızca Google AdMob

Yandex ülkeleri: RU, TR, KZ, BY, UZ, AM, KG, AZ, TJ, GE, MD, RS

Reklam birim ID’leri: `app/src/main/res/values/strings.xml`

- `yandex_*` : mevcut gerçek Yandex ID’leri (korundu)
- `huawei_*` : Huawei’nin resmi **test** ID’leri — yayına almadan kendi Petal Ads ID’lerinizle değiştirin
- `admob_*`  : Google’ın resmi **test** ID’leri — yayına almadan kendi AdMob ID’lerinizle değiştirin

AdMob kullanacaksanız AdMob konsolunda **Privacy & messaging** bölümünden
GDPR mesajı oluşturun. Ayarlar ekranı, gerekli olduğunda gizlilik formunu açar.

## Oyun / arayüz

- Ana menü, ayarlar, istatistik, eğitim, duraklatma, kazanınca yıldız/süre bonusu
- 5 masa teması (Yeşim, Gece, Sakura, Lake, Okyanus) ve 2 taş stili (Fildişi / Oniks)
- Süre sayacı, combo rozeti, kilitli-taş soldurma, eşleşen çift parıltısı
- Orijinal mahjong kuralları, dizilimler, puanlama, ipucu/karıştırma reklam hakları aynıdır
- Dil: İngilizce + Türkçe + Rusça (cihaz diline göre)

## Derleme

Android Studio ile açın. `local.properties` içindeki `sdk.dir` yolunu kendi
SDK yolunuzla güncelleyin. AGP 8.7.3, Kotlin 2.2.21, compileSdk 35.
