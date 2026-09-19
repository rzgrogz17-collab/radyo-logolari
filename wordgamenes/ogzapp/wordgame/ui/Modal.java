package ogzapp.wordgame.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

import ogzapp.wordgame.config.UIConfig;


public class Modal extends Group {

    // NOT: Önceden bu karartma katmanı dıştan yüklenen NinePatches.rect
    // dokusuyla çiziliyordu. O dokunun kendi içindeki olası
    // saydamlık/gradyan farkları, dialog açıldığında arkasındaki oyun
    // ekranının (harfler, tahta) gereğinden fazla belli olmasına yol
    // açıyordu. Artık kod içinde üretilen, TAM KAPLAYAN, düz tek renkli
    // (1x1) bir doku kullanılıyor - böylece UIConfig.DIALOG_MODAL_BACKGROUND_COLOR
    // içindeki alfa değeri ekranda TAM OLARAK o oranda karartma sağlıyor.
    //
    // KÖK NEDEN DÜZELTMESİ (context-loss dayanıklılığı): Bu doku STATIC,
    // yani bir kere üretiliyor ve OYUNDAKİ HER DİYALOĞUN (Menü, Dil,
    // Onay pencereleri, vs.) arkasında tekrar tekrar kullanılıyor. Önceden
    // düz "new Texture(pixmap); pixmap.dispose();" ile üretiliyordu - bu,
    // LevelEndView.java'daki ödül halkası için zaten tespit edilip
    // düzeltilmiş olan KLASİK "unmanaged doku" hatasının BİREBİR AYNISI.
    // Böyle üretilen bir Texture, Android'de uygulama arka plana alınıp
    // GL context kaybolduğunda libGDX'in otomatik doku kurtarma sistemine
    // DAHİL OLMUYOR; geçersiz bir GL ID ile "hayalet" halde kalıyor ve o
    // ID daha sonra BAŞKA bir dokuya (ör. bir atlas sayfasına) yeniden
    // atanınca, bu karartma katmanı - ve dolayısıyla ARKASINDA AÇILAN HER
    // DİYALOG - o dokunun içeriğini göstermeye başlıyordu. Bu doku TÜM
    // diyaloglarda paylaşıldığı için, olası bozulma tek bir ekranla sınırlı
    // kalmıyor, uygulamanın herhangi bir yerinde rastgele/kalıcı görsel
    // sorunlara yol açabiliyordu. Çözüm, ring dokusuyla AYNI: pixmap ASLA
    // dispose edilmiyor, managed=true ile bir PixmapTextureData'ya sarılıp
    // Texture'a öyle veriliyor - böylece context kaybından sonra bu doku da
    // kendini GEÇERLİ, yeni bir GL ID ile doğru (düz beyaz) içerikle
    // yeniden oluşturuyor.
    private static Texture solidPixel;

    public Modal(float width, float height){

        setSize(width, height);

        if(solidPixel == null){
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(1f, 1f, 1f, 1f);
            pixmap.fill();
            PixmapTextureData texData = new PixmapTextureData(pixmap, pixmap.getFormat(), false, false, true);
            solidPixel = new Texture(texData);
        }

        Image bg = new Image(solidPixel);
        bg.setSize(width, height);
        bg.setColor(UIConfig.DIALOG_MODAL_BACKGROUND_COLOR);
        addActor(bg);
    }
}
