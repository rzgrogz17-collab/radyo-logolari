# Satranç Salonu — yapılanlar

Bu paket, depoya `satranc.zip` hiç yüklenmediği için mevcut bir satranç projesinin üstüne yama değil, oynanabilir yeni bir Android Studio projesidir. Reklam kararı, diğer oyunlardaki (Mahjong / Sudoku) anahtarla aynı kuralla kuruldu; ülke listesi ve true/false geçişi bozulmadı.

## Reklam anahtarı

Tek yer: `app/src/main/java/com/example/satranc/AdConfig.kt`

- `USE_YANDEX_AND_HUAWEI = true`  
  RU, TR, KZ, BY, UZ, AM, KG, AZ, TJ, GE, MD, RS → Yandex  
  Bu ülkelerin dışı → Huawei Petal
- `USE_YANDEX_AND_HUAWEI = false`  
  Yandex ve Huawei hiç açılmaz. Her ülkede yalnızca AdMob.

Ülke sırası: şebeke, SIM, daha önce görülen şebeke, saat dilimi, cihaz dili. Ayarlar ekranının altında ülke ve seçilen ağ yazılır.

Birim kimlikleri `res/values/strings.xml` içindedir. Yandex şu an demo kimlik (`demo-banner-yandex` vb.), Huawei ve AdMob resmi test kimlikleridir. Yayına çıkmadan önce kendi kimliklerinle değiştir. AdMob uygulama kimliği manifestteki `APPLICATION_ID` alanından okunur.

## Büyük banner

Alt şerit artık 50dp standart banner değil. Yükseklik 100dp: bir standart banner kadar yukarı uzar. Tahta ve alttaki düğmeler bu kadar yükselir.

- Yandex: `BannerAdSize.inline`, en fazla 100dp
- Huawei: `BannerAdSize(genişlik, 100)`
- AdMob: `getInlineAdaptiveBannerAdSize(genişlik, 100)`

Aynı büyük şerit ana menünün altında da durur.

Geçiş reklamı, bitmiş bir partiden yeni oyuna veya ana menüye geçerken gösterilir. İpucu hakkı bitince ödüllü reklam +3 ipucu verir. Bulmacalarda geçiş reklamı yoktur.

## Oyun

- Ana menü: devam et, bilgisayara karşı, iki oyuncu, bulmaca, zorluk, renk, tahta rengi, ayarlar, istatistik, kurallar.
- 10 tahta: ceviz, turnuva yeşili, zümrüt, okyanus, mor, mermer, gül, buz, mercan, obsidyen.
- 3 taş stili: klasik, altın, gece.
- Üst çubuk: geri, parti adı, ses, (iki oyuncuda) ileri al.
- Alt düğmeler: geri al, ipucu, tahtayı çevir, teslim, yeni oyun.
- Rok, geçerken alma, terfi, şah, mat, pat, 50 hamle, üç tekrar, yetersiz taş.
- İsteğe bağlı saat: 5, 10 veya 15 dakika.
- Sekiz adet “bir hamlede mat” bulmacası.
- Parti kaydı: menüden devam edilir.
- Arayüz Türkçe; telefon dili İngilizceyse İngilizce metinler açılır.

Kurallar, başlangıç pozisyonunun 1–4 hamle derinliği ve bilinen test pozisyonlarıyla doğrulandı (örneğin başlangıç derinliği 4 = 197281).
