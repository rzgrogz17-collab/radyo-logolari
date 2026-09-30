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

## 1. Reklam — yalnızca Google AdMob

Tüm ülkelerde banner, geçiş ve ödüllü reklam Google AdMob ile gösterilir.
Yandex ve Huawei SDK, depo ve kodu projede yoktur.

Reklam birim ID'leri: `app/src/main/res/values/strings.xml`

- `admob_app_id`
- `admob_banner_id`
- `admob_interstitial_id`
- `admob_rewarded_id`

Şu an Google'ın resmi test ID'leri yazılıdır. Yayınlamadan önce kendi AdMob ID'lerinizle değiştirin.
AdMob konsolunda Privacy & messaging bölümünden GDPR mesajı oluşturun.

Oyun akışı:

- İpucu hakkı bitince ödüllü reklam, 3 ipucu verir
- Karıştırma hakkı bitince ödüllü reklam, 10 karıştırma verir
- Reklam yüklenemezse acil 1 karıştırma verilir
- İnternet yoksa uyarı çıkar, reklam açılmaz
- Seviye bitince geçiş reklamından sonra sonraki seviye açılır
- Banner ekranın altındadır

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

- `AdManager.kt` — AdMob geçiş, ödüllü ve banner akışı
- `AdMobAdProvider.kt` — Google AdMob ve UMP onay formu
- `GameEngine.kt` — kurallar, kayıt, süre, yıldız (dizilimler orijinal)
- `SoundManager.kt` — efekt ve müzik
- `Theme.kt` — renk, 5 tema, 2 taş stili
- `TileView.kt` / `Scene.kt` / `UiComponents.kt` — taş ve cam arayüz
- `GameScreen.kt` / `Screens.kt` / `MainActivity.kt` — ekranlar

Manifest’te AdMob `APPLICATION_ID` vardır. Bağımlılık: `play-services-ads` ve `user-messaging-platform`.
