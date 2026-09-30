# Satranç Salonu — yapılanlar

Bu paket, depoya `satranc.zip` hiç yüklenmediği için mevcut bir satranç projesinin üstüne yama değil, oynanabilir yeni bir Android Studio projesidir.

## Reklam

Yalnızca Google AdMob kullanılır.

Birim kimlikleri `res/values/strings.xml` içindedir (`admob_app_id`, `admob_banner_id`, `admob_interstitial_id`, `admob_rewarded_id`). Şu an Google’ın resmi test kimlikleridir. Yayına çıkmadan önce kendi kimliklerinle değiştir. Uygulama kimliği manifestteki `APPLICATION_ID` alanından okunur.

## Büyük banner

Alt şerit artık 50dp standart banner değil. Yükseklik 100dp: bir standart banner kadar yukarı uzar. Tahta ve alttaki düğmeler bu kadar yükselir.

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
