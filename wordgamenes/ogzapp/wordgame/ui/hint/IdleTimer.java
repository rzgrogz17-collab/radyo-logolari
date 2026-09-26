package ogzapp.wordgame.ui.hint;

import ogzapp.wordgame.config.GameConfig;

public class IdleTimer {

    private static float time;
    private static boolean paused = true;
    private static Runnable callback;



    public static void setCallback(Runnable callback){
        IdleTimer.callback = callback;
    }


    public static void reset(){
        time = 0;
    }


    public static void setPaused(boolean flag){
        paused = flag;
    }


    public static void update(float dt){
        if(!paused){
            time += dt;

            if(time >= GameConfig.IDLE_TIMER_DURATION){
                callback.run();
                setPaused(true);
            }
        }
    }

}
