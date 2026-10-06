# Block Jungle — what changed, and how to present the game

## Ad switch

Open `app/src/main/res/values/strings.xml`.

- `use_admob` = **true** → Google AdMob (banner, interstitial, rewarded)
- `use_admob` = **false** → Yandex Ads in those same three places

Replace the demo ids before release:

- AdMob: `id_banner`, `id_inter`, `id_reward`, and the AdMob app id in `AndroidManifest.xml`
- Yandex: `yandex_banner_id`, `yandex_inter_id`, `yandex_reward_id`

---

## English

### What changed

Block Jungle is the same block-puzzle game: drag pieces onto the board, fill rows and columns, and chase a high score. The new version adds to that loop instead of replacing it.

The how-to-play now runs once. It is saved on the phone, not only in the page, so it does not come back every time the app opens. A Skip control ends it early. After that, the only help is a small pulse on a piece that still fits.

The home screen shows the title, best score, best combo and games played, plus sound and theme controls. The jungle backgrounds are no longer covered by a red tint. Switching theme keeps the current game on the board.

Loading shows a moving logo, a real progress percent and short tips. The phone splash stays up until that screen is ready, then goes away. Music is a new jungle loop that pauses and continues instead of starting over on every screen. Sound effects were replaced, with extra sounds for combos and a new record. Mute is remembered.

On the board, a ghost shows where the piece will land, blocks pick up soft colors, and clearing several lines at once builds a combo and bonus points. New T, L, S and Z pieces are mixed into the tray without changing the original pieces the tutorial uses. Beating a saved record shows a banner. A bad drop flashes the tray. If an ad is not ready, the game no longer gets stuck on the continue screen.

Banner, interstitial and rewarded ads still use the same moments. One switch chooses AdMob or Yandex.

### Store text

Block Jungle is a quick block puzzle with a warm jungle board. Drag wooden pieces into the gaps, clear full rows and columns, and build combos when you clear more than one line. Ghost blocks show the landing spot, your best score stays saved, and the how-to-play appears only the first time. Relax with the music, switch the day and night theme, and see how long you can keep the jungle clear.

---

## Türkçe

### Ne değişti

Block Jungle aynı blok oyunu: taşları tahtaya sürükle, satır ve sütunları doldur, skoru büyüt. Yeni sürüm bu akışı bozmadan üstüne ekliyor.

Nasıl oynanır artık bir kez çıkıyor. Kayıt sayfanın içinde değil, telefonda duruyor; bu yüzden uygulama her açıldığında eğitim baştan başlamıyor. İsteyen Geç ile kapatıyor. Sonrasında yalnızca yerleşebilen bir taşın hafifçe büyümesi var.

Ana ekranda başlık, en iyi skor, en iyi kombo ve oynanan oyun sayısı var. Ses ve tema düğmeleri de orada. Orman görsellerinin üstündeki kırmızı örtü kalktı. Tema değişince eldeki oyun silinmiyor.

Yüklemede logo hareket ediyor, yüzde gerçekten ilerliyor ve kısa ipuçları geçiyor. Telefonun açılış görseli, bu ekran hazır olunca kapanıyor. Müzik yeni bir orman döngüsü; ekran değişince başa sarmıyor, durduğu yerden devam ediyor. Ses efektleri yenilendi, komboya ve yeni rekora ayrı sesler eklendi. Sessiz seçimi hatırlanıyor.

Tahtada taşın ineceği yer hayalet olarak görünüyor, bloklar hafif renk alıyor, aynı anda birden fazla sıra silinince kombo ve bonus puan geliyor. T, L, S ve Z taşları tepsiye karışıyor; eğitimin kullandığı eski taşlar duruyor. Kayıtlı rekor geçilince yazı çıkıyor. Taş yanlış yere bırakılırsa tepsi yanıp sönüyor. Reklam hazır değilse devam ekranında oyun kilitlenmiyor.

Banner, geçiş ve ödüllü reklam aynı anlarda duruyor. Tek anahtar AdMob veya Yandex seçiyor.

### Tanıtım yazısı

Block Jungle, sıcak bir orman tahtasında oynanan hızlı bir blok oyunu. Tahta parçalarını boşluklara sürükle, dolu satır ve sütunları temizle, birden fazla sırayı aynı anda silince kombo yap. Taşın nereye oturacağını hayalet bloklar gösterir, en iyi skorun saklanır ve nasıl oynanır yalnızca ilk seferde çıkar. Müziği aç, gündüz ve gece temasını değiştir, ormanı ne kadar açık tutabildiğine bak.

---

## Русский

### Что изменилось

Block Jungle — та же головоломка с блоками: перетаскиваешь фигуры на поле, заполняешь ряды и столбцы и копишь счёт. Новая версия дополняет этот цикл, а не заменяет его.

Обучение теперь один раз. Оно сохраняется в телефоне, а не только на странице, поэтому не начинается заново при каждом запуске. Его можно закрыть кнопкой «Пропустить». Дальше остаётся только лёгкая подсветка фигуры, которая ещё помещается.

На главном экране есть название, рекорд, лучшее комбо и число партий, а также звук и тема. Красная заливка больше не закрывает фоны джунглей. Смена темы не стирает текущую партию.

Загрузка показывает движущийся логотип, настоящий процент и короткие подсказки. Заставка телефона держится, пока этот экран не готов, и затем исчезает. Музыка — новый цикл джунглей: она ставится на паузу и продолжается, а не начинается сначала на каждом экране. Звуки заменены, для комбо и нового рекорда есть отдельные эффекты. Беззвучный режим запоминается.

На поле призрак показывает, куда встанет фигура, блоки получают мягкий цвет, а несколько линий сразу дают комбо и бонус. Новые фигуры T, L, S и Z подмешиваются в лоток, учебные фигуры при этом не меняются. Побитый рекорд отмечается надписью. Неудачная постановка мигает лотком. Если реклама не готова, игра больше не зависает на экране продолжения.

Баннер, межстраничная и реклама с наградой остаются на тех же местах. Один переключатель выбирает AdMob или Yandex.

### Текст для магазина

Block Jungle — быстрая головоломка на тёплом поле джунглей. Перетаскивай деревянные фигуры в свободные клетки, очищай полные ряды и столбцы и собирай комбо, когда снимаешь сразу несколько линий. Призрачные блоки показывают место посадки, рекорд сохраняется, а обучение появляется только в первый раз. Включи музыку, смени дневную и ночную тему и проверь, как долго поле останется свободным.
