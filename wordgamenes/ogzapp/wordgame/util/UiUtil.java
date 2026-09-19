package ogzapp.wordgame.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;


public class UiUtil {


    private static Shaker shaker;


    public static void shake(Actor actor, boolean horizontal, float amount, Runnable callback){
        if(shaker == null) shaker = new Shaker();

        shaker.shake(actor, horizontal, amount, callback);
        actor.addAction(shaker);
    }





    public static void pulsate(Actor actor){

        SequenceAction sequenceAction = new SequenceAction(
                Actions.scaleTo(0.95f, 0.95f, 1),
                Actions.scaleTo(1,1, 1)
        );

        actor.addAction(Actions.forever(sequenceAction));
    }





    // NOT: Onceden burada "ice dogru kaybolma / icten acilma" hissi veren,
    // olcegi (scale) 0 ile 1 arasinda (fazlasiyla tasan "backIn/backOut"
    // egrisiyle) degistiren bir animasyon vardi. Bu olcek animasyonu, ayni
    // actor uzerinde ust uste/ic ice tetiklendiginde (or. seviye gecisi
    // sirasinda dial'in kendi olcegiyle cakismasi) olcegin tasip kalici
    // olarak buyuk/bozuk kalmasina yol acabiliyordu - Seviye 41 gorselindeki
    // dev, bicimsiz daire hatasinin kok nedeni tam olarak buydu. Artik
    // olcege HIC dokunulmuyor; sadece duz (flat) bir saydamlik gecisi var.
    public static void actorAnimIn(Actor actor, float delay, Runnable callback){
        if(actor != null && actor.isVisible()){
            // hideInstantly() zaten clearActions() yapıyordu; burada da
            // aynısını ekliyoruz - aksi halde bu metot aynı actor için üst
            // üste (ör. çok hızlı seviye geçişlerinde) çağrılırsa, eski
            // Sequence bitmeden yenisi eklenip callback (ör. resumeCombo,
            // checkTutorial vb.) İKİ KEZ çalışabiliyordu.
            actor.clearActions();
            actor.setScale(1f);
            actor.getColor().a = 0f;
            SequenceAction sequenceAction = new SequenceAction();
            sequenceAction.addAction(Actions.delay(delay));
            sequenceAction.addAction(Actions.fadeIn(0.2f));
            if(callback != null) sequenceAction.addAction(Actions.run(callback));
            actor.addAction(sequenceAction);
        }
    }





    public static void actorAnimOut(Actor actor, float delay, Runnable callback){
        actorAnimOut(actor, delay, 0f, callback);
    }



    public static void actorAnimOut(Actor actor, float delay1, float delay2, Runnable callback){
        if(actor != null && actor.isVisible()){
            actor.clearActions();
            SequenceAction sequenceAction = new SequenceAction();
            sequenceAction.addAction(Actions.delay(delay1));
            sequenceAction.addAction(Actions.fadeOut(0.2f));
            if(delay2 > 0f) sequenceAction.addAction(Actions.delay(delay2));
            if(callback != null) sequenceAction.addAction(Actions.run(callback));
            actor.addAction(sequenceAction);
        }
    }






    public static boolean isScreenWide(){
        return (float)Gdx.graphics.getHeight() / (float)Gdx.graphics.getWidth() < 1.43f;
    }
}
