package ogzapp.wordgame.ui.dial;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.utils.Disposable;

import ogzapp.wordgame.graphics.shader.MeshShader;
import ogzapp.wordgame.managers.ResourceManager;

public class DialAnimationContainer extends Group implements Disposable {

    private ResourceManager resourceManager;
    private Actor dial;
    private boolean running;
    public MeshShader dialAnimation;

    private boolean autoIncrease;
    private float autoIncreaseTime;
    private float autoIncreaseInterval;
    private int autoIncreaseIndex;
    private int targetState;
    private boolean fadeInShader;

    private ParticleEmitter particleEmitter;
    private ParticleSettings particleSettings = new ParticleSettings();
    private ShaderProgram shaderProgram;





    public DialAnimationContainer(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        getColor().a = 0f;
    }





    public void setDial(Actor dial){
        this.dial = dial;
    }





    public void init(){
        if(particleEmitter == null) {
            setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            setParticleEmitter();
        }

        if(shaderProgram == null)
            shaderProgram = resourceManager.get(ResourceManager.SHADER_DIAL, ShaderProgram.class);

        if(dialAnimation == null) {
            dialAnimation = new MeshShader(shaderProgram);
            dialAnimation.setPaused(true);
            dialAnimation.setWidth(Gdx.graphics.getWidth() );
            dialAnimation.setHeight(Gdx.graphics.getHeight() );
            dialAnimation.setUniformFloat("u_dial_diameter", dial.getHeight() * (Gdx.graphics.getWidth() / getStage().getWidth()));
            dialAnimation.setUniformVec2("u_center", new Vector2(0.5f, (dial.getY() + dial.getHeight() * 0.5f) / getStage().getHeight()));
            addActor(dialAnimation);
        }
    }





    public void stop(){
        // KÖK NEDEN DÜZELTMESİ ("daire ekranı kaplıyor" hatası): Burada
        // ÖNCEDEN sadece "Actions.fadeOut(0.1f)" eklenip bitince running/
        // dialAnimation kapatılıyordu - ama draw() içindeki fadeInShader ve
        // autoIncrease bayrakları HÂLÂ AÇIKSA, bu Action'ın ilerlettiği alfa
        // değeriyle AYNI ANDA her karede "getColor().a += 0.05f" de
        // uygulanmaya devam ediyordu (iki farklı mekanizma AYNI alfa
        // değerini yarışarak değiştiriyordu). Bir önceki seviyenin combo
        // parıltısı tam bu fadeOut ortasındayken yeni seviye/kombo
        // tetiklenirse (bkz. setStage/setAutoIncrease), bu yarış bazen
        // alfayı 0'a değil 1'e kilitli bırakıyor ve tam ekranı kaplayan o
        // "dev daire" hep açık kalıyordu - hata "bazen"di çünkü zamanlamaya
        // (kare hızına) bağlıydı.
        //
        // Düzeltme: clearActions() ile bekleyen/çakışan HER Action iptal
        // ediliyor ve bayraklar ANINDA (Action'ın bitmesini beklemeden)
        // kapatılıyor - böylece hiçbir şey artık fadeOut'un alfasıyla
        // yarışamıyor.
        clearActions();
        autoIncrease = false;
        fadeInShader = false;

        RunnableAction runnableAction = new RunnableAction();
        runnableAction.setRunnable(new Runnable() {
            @Override
            public void run() {
                running = false;
                dialAnimation.setPaused(true);
                dialAnimation.setVisible(false);
            }
        });

        addAction(new SequenceAction(Actions.fadeOut(0.1f), runnableAction));
    }





    public void setStage(int stage){

        // KÖK NEDEN DÜZELTMESİ (devamı - bkz. stop() yorumu): stage sıfırdan
        // farklıysa (yani efekt yeniden AÇILIYORSA), önceki bir stop()
        // çağrısından kalmış olabilecek, henüz tamamlanmamış gecikmeli
        // fadeOut Action'ı burada iptal ediliyor. Aksi halde o eski Action
        // birazdan kendiliğinden tamamlanıp "running=false; dialAnimation
        // paused/invisible" yaparak DAHA YENİ başlattığımız bu animasyonu
        // sessizce ve beklenmedik şekilde kapatabiliyor ya da tam tersi
        // şekilde alfa değerini bozabiliyordu.
        if(stage != 0) clearActions();

        switch (stage){
            case 0:
                stop();
                return;
            case 1:
                particleSettings.count = 0;
                dialAnimation.setPaused(false);
                if(!autoIncrease)dialAnimation.setVisible(true);
                break;
            case 2:
                particleSettings.count = 2;
                break;
            case 3:
                particleSettings.count = 4;
                particleSettings.speedMin   = getStage().getWidth() * 0.004f;
                particleSettings.speedMax   = getStage().getWidth() * 0.007f;
                break;
            case 4:
                particleSettings.count = 6;
                break;
            case 5:
                particleSettings.count = 8;
                particleSettings.speedMin   = getStage().getWidth() * 0.005f;
                particleSettings.speedMax   = getStage().getWidth() * 0.008f;
                break;
            case 6:
                particleSettings.count = 10;
                break;
            case 7:
                particleSettings.count = 12;
                particleSettings.speedMin   = getStage().getWidth() * 0.006f;
                particleSettings.speedMax   = getStage().getWidth() * 0.009f;
                break;
            case 8:
                particleSettings.count = 14;
                break;
            case 9:

                particleSettings.count = 16;
                break;
            case 10:
                particleSettings.speedMin   = getStage().getWidth() * 0.007f;
                particleSettings.speedMax   = getStage().getWidth() * 0.01f;
                particleSettings.count = 18;
                break;
            case 11:
                particleSettings.count = 20;
                break;
            case 12:
                particleSettings.count = 22;
                break;
            case 13:
                particleSettings.count = 24;
                break;
            case 14:
                particleSettings.count = 26;
                break;
            case 15:
                particleSettings.count = 27;
                break;
            case 16:
                particleSettings.count = 28;
                break;
            default:
                particleSettings.count = 28;
        }

        running = true;
        particleEmitter.setStage(stage);
        dialAnimation.setPaused(false);
        setVisible(true);

        if(getColor().a < 1f)
            fadeInShader = true;
    }





    private void setParticleEmitter(){
        particleSettings.count      = 0;
        particleSettings.gravity    = 0.8f;
        particleSettings.speedMin   = getStage().getWidth() * 0.003f;
        particleSettings.speedMax   = getStage().getWidth() * 0.006f;
        particleSettings.friction   = 0.98f;
        particleEmitter = new ParticleEmitter(dial, particleSettings, 1);
    }





    public void setAutoIncrease(int state){
        // Aynı çakışma stop()/setStage() için de geçerliydi - bkz. oradaki
        // yorumlar. Burada da bekleyen eski bir fadeOut Action'ı iptal
        // ediliyor.
        clearActions();
        autoIncreaseTime = 0;
        autoIncreaseInterval = 1f / (float)state;
        targetState = state;
        setVisible(true);
        getColor().a = 0;
        autoIncrease = true;
        fadeInShader = true;
    }





    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);

        Color color = getColor();
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);

        if(running && particleEmitter != null){
            particleEmitter.update(batch);
        }

        if(dialAnimation != null){
            dialAnimation.setUniformFloat("u_alpha", color.a);
        }

        batch.setColor(color.r, color.g, color.b, 1);

        if(autoIncrease){
            if(autoIncreaseTime >= autoIncreaseInterval){
                autoIncreaseIndex++;
                setStage(autoIncreaseIndex);
                autoIncreaseTime = 0;

                if(autoIncreaseIndex >= targetState){
                    autoIncrease = false;
                    fadeInShader = true;
                }
            }
            autoIncreaseTime += Gdx.graphics.getDeltaTime();
        }

        if(fadeInShader){
            // getColor().a'yı 1'i ASLA geçmeyecek şekilde sınırlıyoruz
            // (önceden düz "+= 0.05f" idi; taşma tehlikesi yoktu ama yine de
            // savunmacı bir sınırlama olarak netleştirildi).
            color.a = Math.min(1f, color.a + 0.05f);
            if(color.a >= 1f){
                fadeInShader = false;
            }
        }

    }




    // KÖK NEDEN DÜZELTMESİ ("daire ekranı kaplıyor" hatası - bkz. stop() ve
    // setStage() yorumları): Önceden her yeni seviye başında bu efekt SADECE
    // "hideUI() -> dialAnimationContainer.getColor().a = 0f" ile TEK BİR
    // ALANI (alfayı) sıfırlıyordu; fadeInShader/autoIncrease gibi "her
    // karede bu alfayı kendiliğinden artıran" bayraklar ile bekleyen eski
    // Action'lar (bkz. stop()) o sırada hâlâ etkin kalabiliyordu. Sonuç: bir
    // önceki seviyenin combo parıltısı tam kapanma anındayken yeni seviye
    // başlarsa, alfa hemen ardından (aynı veya bir sonraki karede) tekrar
    // 1'e doğru týrmanıp o "ekranı kaplayan dev daire" hatasına yol
    // açıyordu - "bazen" oluyordu çünkü bu tamamen zamanlama/kare hızına
    // bağlıydı.
    //
    // forceReset(): olası TÜM bekleyen Action'ları iptal edip, bu sınıfın
    // durumunu (bayraklar dahil) TEK SEFERDE, sert ve kesin şekilde
    // "tamamen kapalı" durumuna getiriyor. Artık her yeni seviye
    // kurulurken (GameScreen.initDial()) bu metot çağrılıyor - böylece bir
    // önceki seviyeden kalan HİÇBİR animasyon/bayrak/Action yeni seviyeye
    // sızamıyor.
    public void forceReset(){
        clearActions();
        running = false;
        autoIncrease = false;
        fadeInShader = false;
        autoIncreaseTime = 0;
        autoIncreaseIndex = 0;
        getColor().a = 0f;
        setVisible(false);

        if(particleSettings != null) particleSettings.count = 0;

        if(dialAnimation != null){
            dialAnimation.setPaused(true);
            dialAnimation.setVisible(false);
        }
    }




    @Override
    public void dispose() {
        if(dialAnimation != null) dialAnimation.dispose();
    }
}
