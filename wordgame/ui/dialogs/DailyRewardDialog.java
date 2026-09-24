package ogzapp.wordgame.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;

import ogzapp.wordgame.config.GameConfig;
import ogzapp.wordgame.config.UIConfig;
import ogzapp.wordgame.graphics.AtlasRegions;
import ogzapp.wordgame.graphics.NinePatches;
import ogzapp.wordgame.managers.DailyRewardManager;
import ogzapp.wordgame.managers.LanguageManager;
import ogzapp.wordgame.managers.ResourceManager;
import ogzapp.wordgame.screens.BaseScreen;

// 7 günlük giriş ödülü takvimi. GameConfig.DAILY_REWARD_COINS dizisindeki
// tutarları gösterir; bugünün günü vurgulanır, önceki günler "alındı" olarak
// işaretlenir, sonraki günler kilitli görünür.
public class DailyRewardDialog extends BaseDialog {

    // --- Gün kutucuğu görünümü (kullanıcı isteğiyle YENİLENDİ) ---
    // Eskiden düz/mat, gölgesiz tek renkli kutucuklardı ("eski tarz"
    // görünüm veriyordu). Şimdi Menü'deki buzlu-cam-kart dokusu
    // (NinePatches.round_rect_shadow - yumuşak kenarlı, hafif gölgeli
    // kart) kullanılıyor ve her duruma özel küçük bir rozet eklendi:
    // alınan günlerde onay işareti, henüz gelmemiş günlerde asma kilit,
    // bugünün kutucuğunda ise yumuşak nabız gibi atan bir parıltı halkası.
    private static final Color DAY_BG_LOCKED_COLOR       = new Color(0x27394CE6);
    private static final Color DAY_BG_CLAIMED_COLOR      = new Color(0x2ECC71CC);
    private static final Color DAY_BG_TODAY_COLOR        = new Color(0x1DBAACFF);
    private static final Color DAY_TEXT_COLOR             = Color.WHITE;
    // Kullanıcı isteğiyle: bugünün kutucuğundaki gün adı ve coin miktarı
    // yazıları (ör. "1. Gün" / "10") artık altın/turuncu DEĞİL, diğer
    // günlerle AYNI beyaz renk. (Yalnızca bugünün turkuaz zemini ve nabız
    // efekti "bugün" vurgusunu veriyor artık.)
    private static final Color DAY_TEXT_TODAY_COLOR       = Color.WHITE;
    private static final Color DAY_TODAY_GLOW_COLOR       = new Color(0xFFC940FF);
    private static final float DAY_LOCKED_CONTENT_ALPHA   = 0.6f;
    private static final float DAY_CLAIMED_CONTENT_ALPHA  = 0.85f;

    private TextButton claimButton;

    public DailyRewardDialog(float width, float height, BaseScreen screen) {
        super(width, height, screen);

        content.setSize(width * 0.85f, height * 0.55f);

        int today = DailyRewardManager.getUpcomingStreakDay();
        int totalDays = GameConfig.DAILY_REWARD_COINS.length;
        int claimedUpTo = (today == 1) ? 0 : today - 1;

        BitmapFont dayFont = screen.wordConnectGame.resourceManager.get(ResourceManager.fontSemiBold, BitmapFont.class);
        Label.LabelStyle dayNumberStyle = new Label.LabelStyle(dayFont, DAY_TEXT_COLOR);
        Label.LabelStyle coinAmountStyle = new Label.LabelStyle(dayFont, DAY_TEXT_COLOR);

        Table table = new Table();
        table.setWidth(content.getWidth() * 0.92f);

        float cellSize = table.getWidth() * 0.22f;
        float pad = cellSize * 0.06f;

        for (int day = 1; day <= totalDays; day++) {
            boolean isToday = day == today;
            boolean isClaimed = day <= claimedUpTo;

            Group cell = new Group();
            cell.setSize(cellSize, cellSize);
            cell.setOrigin(Align.center);

            // Bugünün kutucuğunun arkasında yumuşak, nabız gibi yavaşça
            // parlayıp sönen bir halka - dikkat çekmesi için (5 seviyelik
            // ödül halkasındaki sis efektiyle AYNI ruhta, çok daha sade).
            // "bg" kartından ÖNCE ekleniyor ki sadece kartın kenarlarından
            // taşan kısmı görünsün.
            if (isToday) {
                float glowSize = cellSize * 1.35f;
                Image glow = new Image(AtlasRegions.glow);
                glow.setSize(glowSize, glowSize);
                glow.setPosition((cellSize - glowSize) * 0.5f, (cellSize - glowSize) * 0.5f);
                glow.setColor(DAY_TODAY_GLOW_COLOR);
                glow.getColor().a = 0.2f;
                cell.addActor(glow);
                glow.addAction(Actions.forever(Actions.sequence(
                        Actions.alpha(0.5f, 1.1f),
                        Actions.alpha(0.18f, 1.1f)
                )));
            }

            // Düz/mat renkli eski "rrect" yerine, Menü'deki hafif gölgeli
            // buzlu-cam-kart dokusu (round_rect_shadow) kullanılıyor - kart
            // artık zeminden hafifçe yükseliyormuş gibi görünüyor.
            Image bg = new Image(NinePatches.round_rect_shadow);
            bg.setSize(cellSize, cellSize);
            bg.setColor(isToday ? DAY_BG_TODAY_COLOR : (isClaimed ? DAY_BG_CLAIMED_COLOR : DAY_BG_LOCKED_COLOR));
            cell.addActor(bg);

            Label dayLabel = new Label(LanguageManager.format("daily_reward_day", day), dayNumberStyle);
            dayLabel.setFontScale(0.6f);
            dayLabel.setAlignment(Align.center);
            dayLabel.setWidth(cellSize);
            dayLabel.setColor(isToday ? DAY_TEXT_TODAY_COLOR : DAY_TEXT_COLOR);
            dayLabel.setX(0);
            float dayLabelTopMargin = cellSize * 0.12f;
            dayLabel.setY(cellSize - dayLabel.getPrefHeight() - dayLabelTopMargin);
            cell.addActor(dayLabel);

            Label amountLabel = new Label(String.valueOf(GameConfig.DAILY_REWARD_COINS[day - 1]), coinAmountStyle);
            amountLabel.setFontScale(0.55f);
            amountLabel.setAlignment(Align.center);
            amountLabel.setWidth(cellSize);
            amountLabel.setColor(isToday ? DAY_TEXT_TODAY_COLOR : DAY_TEXT_COLOR);
            amountLabel.setX(0);
            float amountLabelBottomMargin = cellSize * 0.08f;
            amountLabel.setY(amountLabelBottomMargin);
            cell.addActor(amountLabel);

            // Kullanıcı isteğiyle: coin ikonu artık SABİT bir oran DEĞİL -
            // gün adı yazısı İLE miktar yazısı ARASINDA KALAN BOŞLUĞA göre
            // DİNAMİK olarak hesaplanıyor: o boşluğun ORTASINA yerleşiyor
            // ve o boşluğu (neredeyse) dolduracak şekilde büyüyor. Böylece
            // kutunun 3 öğesi (gün adı / coin / miktar) her zaman kutunun
            // içindekilerle ORANTILI ve TAM ORTALI kalıyor.
            float middleTop = dayLabel.getY();
            float middleBottom = amountLabel.getY() + amountLabel.getPrefHeight();
            float middleSpace = middleTop - middleBottom;
            Image coinIcon = new Image(AtlasRegions.coin_small);
            float coinSize = middleSpace * 0.92f;
            coinIcon.setSize(coinSize, coinSize);
            coinIcon.setX((cellSize - coinSize) * 0.5f);
            coinIcon.setY(middleBottom + (middleSpace - coinSize) * 0.5f);
            cell.addActor(coinIcon);

            if (!isToday) {
                // Henüz gelmemiş günlerde içerik hafif soluk, alınmış
                // günlerde "tamamlandı" hissi için biraz daha az soluk -
                // ikisi de bugünün canlı renginin yanında geri planda kalıyor.
                float contentAlpha = isClaimed ? DAY_CLAIMED_CONTENT_ALPHA : DAY_LOCKED_CONTENT_ALPHA;
                dayLabel.getColor().a = contentAlpha;
                coinIcon.getColor().a = contentAlpha;
                amountLabel.getColor().a = contentAlpha;
            }

            if (isClaimed) {
                // Alınan günlerde sağ üst köşede küçük, altın renkli bir
                // "toplandı" rozeti - kartın İÇİNDE, kenardan taşırmadan.
                // NOT: Bu, gerçek oyun dokularıyla (atlas1_hd) test edilerek
                // ayarlandı - calendar_cell_checked aslında düz beyaz bir
                // daire, kenardan taşırılınca "kopmuş/boş bir top" gibi
                // görünüyordu; bu yüzden küçültülüp içeri alındı ve coin
                // rengiyle aynı altın tona boyandı.
                float badgeSize = cellSize * 0.22f;
                float badgeMargin = cellSize * 0.08f;
                Image check = new Image(AtlasRegions.calendar_cell_checked);
                check.setSize(badgeSize, badgeSize);
                check.setColor(DAY_TODAY_GLOW_COLOR);
                check.setPosition(cellSize - badgeSize - badgeMargin, cellSize - badgeSize - badgeMargin);
                cell.addActor(check);
            } else if (!isToday) {
                // Henüz gelmemiş (kilitli) günlerde sağ üst köşede küçük
                // bir asma kilit rozeti.
                //
                // NOT (ÖNEMLİ SINIRLAMA): Kullanıcı isteğiyle rozetin
                // arkasındaki turkuaz/yeşil renk kırmızıya çevrilmeye
                // çalışıldı. AtlasRegions.padlock, "daire + kilit" birlikte
                // TEK bir dokuya gömülü hazır bir rozet grafiği - bu
                // projede o dokunun ham PNG dosyası YOK (sadece Java kodu
                // var), bu yüzden pikselleri gerçek anlamda kırmızıya
                // BOYAYAMIYORUM. Yapabildiğim tek şey, aşağıdaki gibi bir
                // renk ÇARPANI (tint) uygulamak - bu turkuazı bastırıp
                // koyulaştırır ve kilit sembolünü kırmızıya döndürebilir,
                // ama net/parlak kırmızı bir DAİRE garanti edemez.
                Image lock = new Image(AtlasRegions.padlock);
                float badgeSize = cellSize * 0.28f;
                lock.setSize(badgeSize, badgeSize);
                lock.setPosition(cellSize - badgeSize * 0.62f, cellSize - badgeSize * 0.62f);
                lock.setColor(1f, 0.12f, 0.12f, 0.95f);
                cell.addActor(lock);
            }

            if (isToday) {
                // Bugünün kutucuğu diğerlerinin arasından hafifçe öne
                // çıksın diye yavaşça büyüyüp küçülüyor (nabız efekti).
                cell.addAction(Actions.forever(Actions.sequence(
                        Actions.scaleTo(1.045f, 1.045f, 1.1f),
                        Actions.scaleTo(1f, 1f, 1.1f)
                )));
            }

            table.add(cell).size(cellSize, cellSize).pad(pad);
            if (day % 4 == 0) table.row();
        }
        table.pack();

        claimButton = new TextButton(LanguageManager.format("daily_reward_claim", DailyRewardManager.getCoinsForDay(today)), buildButtonStyle());
        claimButton.getLabel().setFontScale(0.85f);
        claimButton.setWidth(table.getWidth() * 0.86f);

        float titleGap = AtlasRegions.dialog_title.getRegionHeight() * 0.2f;
        float innerGap = cellSize * 0.15f;
        float bottomGap = innerGap * 1.3f;

        content.setHeight(AtlasRegions.dialog_title.getRegionHeight() * 0.83f + titleGap
                + table.getHeight() + innerGap + claimButton.getHeight() + bottomGap);

        setContentBackground();
        // Diğer buzlu cam dialoglarla AYNI paylaşılan renk sabitleri
        // kullanılıyor (UIConfig.java'ya yeni bir şey EKLENMEDİ).
        setContentBackgroundColor(UIConfig.MENU_DIALOG_BACKGROUND_COLOR);

        table.setX((content.getWidth() - table.getWidth()) * 0.5f);
        table.setY(bottomGap + claimButton.getHeight() + innerGap);
        content.addActor(table);

        claimButton.setX((content.getWidth() - claimButton.getWidth()) * 0.5f);
        claimButton.setY(bottomGap);
        content.addActor(claimButton);

        setTitleLabel(LanguageManager.get("daily_reward_title"));
        setTitleBackgroundColor(UIConfig.MENU_DIALOG_TITLE_BACKGROUND_COLOR);
        // NOT: Kullanıcı isteğiyle başlık MENÜ'deki gibi biraz daha aşağı
        // kaydırıldı. content'in yüksekliği başlık için tam olarak
        // titleContainer.getHeight()*0.83f + titleGap kadar pay bıraktığından,
        // bu küçük ek kaydırma "titleGap" boşluğunun bir kısmını kullanır ve
        // tablo/buton ile çakışmaz.
        titleContainer.setY(titleContainer.getY() - titleContainer.getHeight() * 0.08f);
        // Kullanıcı isteğiyle: başlık YAZISI konteynerin içinde de biraz
        // daha aşağı kaydırıldı (yukarıdaki satır tüm başlık ÇUBUĞUNU,
        // bu satır ise sadece YAZIYI kendi kutusunun içinde aşağı alıyor).
        titleLabel.setY(titleLabel.getY() - titleContainer.getHeight() * 0.05f);
        setCloseButton();

        claimButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DailyRewardManager.claim();
                getStage().getRoot().setTouchable(Touchable.disabled);
                hide();
            }
        });
    }

    private TextButton.TextButtonStyle buildButtonStyle() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        String font = UIConfig.WHEEL_DIALOG_SPIN_BUTTON_USE_SHADOW_FONT ? ResourceManager.fontSemiBoldShadow : ResourceManager.fontSemiBold;
        style.font = screen.wordConnectGame.resourceManager.get(font, BitmapFont.class);
        style.fontColor = UIConfig.WHEEL_DIALOG_SPIN_BUTTON_TEXT_COLOR;
        style.up = new NinePatchDrawable(NinePatches.btn_dialog_up);
        style.down = new NinePatchDrawable(NinePatches.btn_dialog_down);
        return style;
    }

    @Override
    protected void hideAnimFinished() {
        super.hideAnimFinished();
        getStage().getRoot().setTouchable(Touchable.enabled);
        remove();
    }
}
