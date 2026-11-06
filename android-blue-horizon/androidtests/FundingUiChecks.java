package com.bluehorizon;
import android.content.SharedPreferences;
import android.view.View;
import java.lang.reflect.*;

public final class FundingUiChecks {
    private interface Action { void run() throws Exception; }
    private static Field field(String name) throws Exception {
        Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field;
    }
    private static void render(MainActivity activity) throws Exception {
        Method method=MainActivity.class.getDeclaredMethod("render");method.setAccessible(true);method.invoke(activity);
    }
    private static void perform(UiSmoke runner,Action action) throws Exception {
        Throwable[] failed={null};runner.runOnMainSync(()->{try { action.run(); } catch(Throwable error) { failed[0]=error; }});
        if(failed[0]!=null) throw new AssertionError(failed[0]);
    }
    public static void run(MainActivity activity,UiSmoke runner) throws Exception {
        Engine original=(Engine)field("game").get(activity);
        SharedPreferences prefs=activity.getSharedPreferences("blue_horizon_v1",0);
        String save=prefs.getString("save",null),backup=prefs.getString("backup",null);
        try {
            Engine empty=new Engine();empty.gold=0;earn(activity,runner,empty);
            Engine dig=new Engine();dig.port=7;dig.visited=149;dig.expedition.clues=21;dig.gold=50;earn(activity,runner,dig);
            Engine remote=new Engine();remote.port=17;remote.visited|=1L<<17;remote.expedition.clues=1L<<17;remote.gold=40;earn(activity,runner,remote);
        } finally {
            perform(runner,()->{
                field("game").set(activity,original);field("tab").setInt(activity,7);render(activity);
                prefs.edit().putString("save",save).putString("backup",backup).commit();
            });
        }
    }
    private static void earn(MainActivity activity,UiSmoke runner,Engine scenario) throws Exception {
        int gold=scenario.gold,day=scenario.day;
        perform(runner,()->{field("game").set(activity,scenario);field("tab").setInt(activity,2);render(activity);});
        runner.waitForIdleSync();Thread.sleep(500);
        perform(runner,()->{
            View work=activity.getWindow().getDecorView().findViewWithTag("funding-recovery");
            android.graphics.Rect bounds=new android.graphics.Rect();
            if(work==null || !work.getGlobalVisibleRect(bounds)) throw new AssertionError("recovery action is hidden");
            work.performClick();
            if(scenario.gold!=gold+120 || scenario.day!=day+1) throw new AssertionError("recovery outcome incorrect");
        });
    }

}
