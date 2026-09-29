# Mahjong Master — yapılan işlemler

Bu klasör, gönderdiğiniz Mahjong Classic Game projesinin yeniden tasarlanmış
halidir. Sürüm: **2.31** (versionCode 31). Android Studio ile açılır.

`local.properties` içindeki `sdk.dir` satırını kendi bilgisayarınızdaki
Android SDK yoluna göre düzeltin. Örnek:

```
sdk.dir=C\:\\Users\\KULLANICI\\AppData\\Local\\Android\\Sdk
```

Derleme: Android Studio’da projeyi açın veya terminalden `gradlew assembleDebug`.
AGP 8.7.3, Kotlin 2.2.21, compileSdk / targetSdk 35, minSdk 24.

---

## 1. Reklam anahtarı (istediğiniz true / false)

Dosya:

`app/src/main/java/com/example/mahjongmaster/AdConfig.kt`

```
const val USE_YANDEX_AND_HUAWEI = true
```

- **true** (şu an böyle)
  - Şu ülkelerde **Yandex**: RU, TR, KZ, BY, UZ, AM, KG, AZ, TJ, GE, MD, RS
  - Bu listenin **dışındaki tüm ülkelerde** **Huawei Petal Ads**
- **false**
  - Yandex ve Huawei tamamen kapanır
  - **Tüm ülkelerde yalnızca Google AdMob** gösterilir

Ülke sırası: mobil şebeke ülkesi, SIM ülkesi, daha önce kaydedilmiş şebeke/SIM,
saat dilimi, cihaz dili. Emülatörde ABD şebekesiyle denendi:
`Country=US -> HUAWEI` (liste dışı olduğu için Petal).

Reklam birim ID’leri tek yerde:

`app/src/main/res/values/strings.xml`

| Anahtar | Durum |
|---|---|
| `yandex_banner_id` / `yandex_interstitial_id` / `yandex_rewarded_id` | Sizin mevcut gerçek Yandex ID’leri, değiştirilmedi |
| `huawei_banner_id` / `huawei_interstitial_id` / `huawei_rewarded_id` | Huawei’nin resmi **test** ID’leri. Yayına almadan kendi Petal Ads ID’lerinizle değiştirin |
| `admob_app_id` / `admob_banner_id` / `admob_interstitial_id` / `admob_rewarded_id` | Google’ın resmi **test** ID’leri. Yayına almadan kendi AdMob ID’lerinizle değiştirin |

AdMob’u açacaksanız (`USE_YANDEX_AND_HUAWEI = false`) AdMob konsolunda
**Privacy & messaging** bölümünden GDPR mesajı oluşturun. Uygulama, gerekli
olduğunda Google UMP onay formunu gösterir; Ayarlar’da “Gizlilik seçenekleri”
çıkar.

Oyun içindeki reklam mantığı bozulmadı:

- İpucu hakkı bitince ödüllü reklam → 3 ipucu
- Karıştırma hakkı bitince ödüllü reklam → 10 karıştırma
- Reklam yüklenemezse acil 1 karıştırma (oyuncu kilitlenmesin)
- İnternet yoksa “bağlantınızı kontrol edin” uyarısı, reklam açılmaz
- Seviye bitince “Sonraki seviye” geçiş (interstitial) reklamından sonra ilerler
- Banner, ekranın altında, seçilen ağa göre (Yandex / Huawei / AdMob)

---

## 2. Arayüz

Oyun tek dosyalık eski ekrandan çıkarıldı. Yeni ekranlar:

- Açılış (splash)
- Ana menü: devam et, yeni oyun, ayarlar, istatistik, nasıl oynanır
- Oyun masası: seviye, skor, kalan çift, süre, ilerleme çubuğu
- Duraklatma (Pause veya geri tuşu)
- Kazanma kartı: yıldız, skor sayacı, süre bonusu, yeni rekor
- Ayarlar ve istatistik
- 4 sayfalık nasıl oynanır (ilk oyunda otomatik)

Görünüm:

- Yeşim zemin üzerinde seigaiha (dalga) deseni ve yumuşak ışıklar
- 3D taş: fildişi yüz, renkli sırt, parlama
- Beyaz ejder (Haku) için klasik mavi çerçeve (SVG boş olduğu için)
- Kilitli taşlar soldurulur, açık eşleşen çiftler altın hale ile nefes alır
- Eşleşen çift havada birleşir, çarpışma kıvılcımı ve “+puan” balonu çıkar
- 5 masa teması: Yeşim, Gece, Sakura, Lake, Okyanus
- 2 taş stili: Fildişi (`tiles/regular`) ve Oniks (`tiles/black`)
- Alt cam buton rafı: Duraklat, Geri Al, İpucu, Karıştır, Yeni
- Yatay ekranda tahta solda, bilgi ve butonlar sağda
- Kenardan kenara (Android 15 / target 35)
- Dil: İngilizce, Türkçe, Rusça (telefon diline göre)

---

## 3. Eklenen oyun özellikleri

Kurallar, 32 dizilim, puan formülü ve kayıt anahtarları eskisiyle aynıdır.
Eski kayıt, ipucu/karıştırma hakkı ve rekor silinmez.

Eklenenler:

- Süre sayacı (oyun açıkken işler, duraklatınca ve uygulama arka plana düşünce durur)
- Par sürenin altında bitirince süre bonusu
- 1–3 yıldız (süre + çok ipucu/karıştırma kullanınca bir yıldız düşer)
- Combo rozeti (ardışık eşleşmede ses perdesi de yükselir)
- İstatistik: biten masa, eşleşme, en iyi combo, en hızlı masa, toplam süre, yıldız
- Kilitli taşa dokununca kısa sallanma
- Yeni masada taşlar sırayla düşer, karıştırmada yüzler döner
- Boş (bitmiş) kayıtla açılışta takılı kalma düzeltildi; sonraki seviyeye geçilir
- “Sonraki seviye”ye çift basınca iki seviye atlanmaz

Puan (değişmedi):

`15 + (combo adımı, en fazla 15) × 8 + (iki taşın katmanı) × 4`

---

## 4. Ses

`res/raw` içine `click`, `match`, `win`, `music` koyarsanız onlar kullanılır.
Yoksa uygulama ilk açılışta kendi taş ve ortam sesini üretir (ek dosya gerekmez).
Müzik ve efekt Ayarlar’dan kapanır. Eski `music_enabled` anahtarı duruyor.

---

## 5. Dosya haritası

`app/src/main/java/com/example/mahjongmaster/`

- `AdConfig.kt` — true/false anahtar ve ülke listesi
- `AdManager.kt` — ülke tespiti ve ortak reklam akışı
- `YandexAdProvider.kt` / `HuaweiAdProvider.kt` / `AdMobAdProvider.kt`
- `GameEngine.kt` — kurallar, kayıt, süre, yıldız (dizilimler orijinal)
- `SoundManager.kt` — efekt ve müzik
- `Theme.kt` — renk, 5 tema, 2 taş stili
- `TileView.kt` / `Scene.kt` / `UiComponents.kt` — taş ve cam arayüz
- `GameScreen.kt` / `Screens.kt` / `MainActivity.kt` — ekranlar

Manifest’e AdMob `APPLICATION_ID`, internet ve reklam kimliği izinleri eklendi.
Huawei deposu `settings.gradle.kts` içinde: `https://developer.huawei.com/repo/`
