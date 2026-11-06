package com.bluehorizon;
import android.content.SharedPreferences;
import android.widget.LinearLayout;
import java.lang.reflect.*;

public final class VoyageSpeedUiChecks {
    private interface Action { void run() throws Exception; }
    private static Field field(String name) throws Exception {
        Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field;
    }
    private static void invoke(MainActivity activity,String name) throws Exception {
        Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);
    }
    private static void perform(UiSmoke runner,Action action) throws Exception {
        Throwable[] failed={null};runner.runOnMainSync(()->{try { action.run(); } catch(Throwable error) { failed[0]=error; }});
        if(failed[0]!=null) throw new AssertionError(failed[0]);
    }
    public static void run(MainActivity activity,UiSmoke runner) throws Exception {
        Engine original=(Engine)field("game").get(activity);
        SharedPreferences prefs=activity.getSharedPreferences("blue_horizon_v1",0);
        String save=prefs.getString("save",null),backup=prefs.getString("backup",null);
        boolean had=prefs.contains("voyage_speed");int speed=prefs.getInt("voyage_speed",1);
        Engine journey=new Engine();TravelFlow.begin(journey,1,1);int gold=journey.gold,day=journey.day;
        try {
            perform(runner,()->{
                field("game").set(activity,journey);invoke(activity,"showSailing");
                LinearLayout choices=activity.getWindow().getDecorView().findViewWithTag("voyage-speed-choices");
                choices.getChildAt(2).performClick();
                if(prefs.getInt("voyage_speed",0)!=4) throw new AssertionError("speed preference not saved");
            });
            Thread.sleep(300);runner.screenshot(activity,"v0.10-voyage-speed.png");
            boolean[] moving={true};
            for(int i=0;i<50;i++) { perform(runner,()->moving[0]=journey.town.moving());if(!moving[0]) break;Thread.sleep(200); }
            if(moving[0] || journey.gold!=gold || journey.day!=day) throw new AssertionError("speed changed outcome or failed arrival");
        } finally {
            perform(runner,()->{
                SailingView view=(SailingView)field("sailing").get(activity);if(view!=null) view.stop();field("sailing").set(activity,null);
                field("game").set(activity,original);invoke(activity,"render");
                SharedPreferences.Editor editor=prefs.edit().putString("save",save).putString("backup",backup);
                if(had) editor.putInt("voyage_speed",speed);else editor.remove("voyage_speed");editor.commit();
            });
        }
    }
}
