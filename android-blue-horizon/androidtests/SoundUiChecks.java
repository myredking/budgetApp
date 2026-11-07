package com.bluehorizon;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.widget.SeekBar;
import java.lang.reflect.*;

public final class SoundUiChecks {
    public static void run(MainActivity activity,Instrumentation runner) throws Exception {
        Field tab=MainActivity.class.getDeclaredField("tab");tab.setAccessible(true);int original=tab.getInt(activity);
        Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);
        SharedPreferences prefs=activity.getSharedPreferences("blue_horizon_v1",0);
        int music=prefs.getInt("music_volume",100),effects=prefs.getInt("effects_volume",100);
        boolean hasMusic=prefs.contains("music_volume"),hasEffects=prefs.contains("effects_volume");Throwable[] failure={null};
        try {
            awaitEffects(activity,runner);
            runner.runOnMainSync(()->{try {
                tab.setInt(activity,4);render.invoke(activity);
                verify(activity,prefs,render);
            } catch(Throwable error) { failure[0]=error; }});
            if(failure[0]!=null) throw new AssertionError(failure[0]);
        } finally {
            runner.runOnMainSync(()->{try {
                Field field=MainActivity.class.getDeclaredField("audio");field.setAccessible(true);
                AudioController controller=(AudioController)field.get(activity);
                controller.musicVolume(music);controller.effectsVolume(effects);
            } catch(Exception error) { throw new RuntimeException(error); }});
            SharedPreferences.Editor edit=prefs.edit();
            if(hasMusic) edit.putInt("music_volume",music);else edit.remove("music_volume");
            if(hasEffects) edit.putInt("effects_volume",effects);else edit.remove("effects_volume");edit.commit();
            runner.runOnMainSync(()->{try { tab.setInt(activity,original);render.invoke(activity); } catch(Exception error) { throw new RuntimeException(error); }});
        }
    }
    private static void awaitEffects(MainActivity activity,Instrumentation runner) throws Exception {
        Field field=MainActivity.class.getDeclaredField("audio");field.setAccessible(true);
        AudioController audio=(AudioController)field.get(activity);
        Field ready=AudioController.class.getDeclaredField("ready");ready.setAccessible(true);int[] count={0};
        for(int i=0;i<40;i++) {
            runner.runOnMainSync(()->{try { count[0]=((java.util.Set<?>)ready.get(audio)).size(); } catch(Exception error) { throw new RuntimeException(error); }});
            if(count[0]==15) return;
            Thread.sleep(100);
        }
        throw new AssertionError("native effect loading incomplete: "+count[0]+"/15");
    }
    private static void verify(MainActivity activity,SharedPreferences prefs,Method render) throws Exception {
        SeekBar bgm=activity.getWindow().getDecorView().findViewWithTag("music-volume");
        SeekBar sfx=activity.getWindow().getDecorView().findViewWithTag("effects-volume");
        if(bgm==null || sfx==null) throw new AssertionError("independent audio sliders missing");
        bgm.setProgress(35);sfx.setProgress(65);
        if(prefs.getInt("music_volume",-1)!=35 || prefs.getInt("effects_volume",-1)!=65) throw new AssertionError("audio volume not persisted independently");
        render.invoke(activity);bgm=activity.getWindow().getDecorView().findViewWithTag("music-volume");
        if(bgm.getProgress()!=35) throw new AssertionError("audio slider lost selection after render");
    }
}
