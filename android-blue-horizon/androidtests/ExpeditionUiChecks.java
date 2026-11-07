package com.bluehorizon;
import android.app.*;
import android.content.*;
import android.net.Uri;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;

public final class ExpeditionUiChecks {
    private interface Action { void run() throws Exception; }
    private static Field field(String name) throws Exception {
        Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field;
    }
    private static void invoke(MainActivity activity,String name,Class<?>[] types,Object... args) throws Exception {
        Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(activity,args);
    }
    private static void perform(Instrumentation runner,Action action) throws Exception {
        Throwable[] failed={null};runner.runOnMainSync(()->{try { action.run(); } catch(Throwable error) { failed[0]=error; }});
        if(failed[0]!=null) throw new AssertionError(failed[0]);
    }
    public static void run(MainActivity activity,UiSmoke runner) throws Exception {
        Engine original=(Engine)field("game").get(activity);
        SharedPreferences prefs=activity.getSharedPreferences("blue_horizon_v1",0);
        String save=prefs.getString("save",null),backup=prefs.getString("backup",null);
        File file=new File(activity.getFilesDir(),"ui-boss.blue.txt");
        try {
            perform(runner,()->{Engine e=seed();field("game").set(activity,e);field("tab").setInt(activity,9);invoke(activity,"render",new Class<?>[0]);});
            Thread.sleep(900);runner.screenshot(activity,"v0.9-expedition.png");
            talk(activity,runner);bossImport(activity,runner,file);
        } finally {
            perform(runner,()->{
                close(activity,"eventDialog");close(activity,"dialogueDialog");
                field("game").set(activity,original);field("tab").setInt(activity,7);invoke(activity,"render",new Class<?>[0]);
                prefs.edit().putString("save",save).putString("backup",backup).commit();
            });
            if(file.exists() && !file.delete()) throw new IOException("Could not remove test fixture");
        }
    }
    private static Engine seed() {
        Engine e=new Engine();e.gold=10000;e.port=7;e.visited=149;e.expedition.clues=21;e.expedition.treasures=1;return e;
    }
    private static void talk(MainActivity activity,UiSmoke runner) throws Exception {
        perform(runner,()->invoke(activity,"talkChoice",new Class<?>[]{int.class,int.class},1,0));
        Thread.sleep(900);runner.screenshot(activity,"v0.9-conversation.png");
        perform(runner,()->{
            invoke(activity,"finishTalk",new Class<?>[]{int.class,int.class,int.class},1,0,0);
            Engine e=(Engine)field("game").get(activity);
            if(e.expedition.trust[0]!=2) throw new AssertionError("dialogue choice failed");
        });
    }
    private static void bossImport(MainActivity activity,UiSmoke runner,File file) throws Exception {
        perform(runner,()->{
            Engine e=(Engine)field("game").get(activity);RegionBoss.begin(e);
            try(FileOutputStream out=new FileOutputStream(file)) { out.write(e.encode().getBytes(StandardCharsets.UTF_8)); }
            invoke(activity,"readBackup",new Class<?>[]{Intent.class},new Intent().setData(Uri.fromFile(file)));
            AlertDialog dialog=(AlertDialog)field("eventDialog").get(activity);
            if(dialog==null || !dialog.isShowing()) throw new AssertionError("boss import lacks battle controls");
            if(frame(activity).action!=-1) throw new AssertionError("imported battle must not replay a shot");
        });
        Thread.sleep(900);runner.screenshot(activity,"v0.9-boss.png");
        checkRetreat(activity,runner);
        perform(runner,()->{
            Engine e=(Engine)field("game").get(activity);int hp=e.expedition.bossHP;
            invoke(activity,"chooseBoss",new Class<?>[]{int.class},0);
            if(e.expedition.bossHP>=hp || Engine.decode(e.encode())==null) throw new AssertionError("battle control/save failed");
            if(frame(activity).enemyDamage!=hp-e.expedition.bossHP || !frame(activity).firing()) throw new AssertionError("attack visual differs from real damage");
        });
        runner.waitForIdleSync();Thread.sleep(2500);runner.screenshot(activity,"v0.13-attack.png");
        perform(runner,()->{
            Engine e=(Engine)field("game").get(activity);int hp=e.hull;
            invoke(activity,"chooseBoss",new Class<?>[]{int.class},1);
            if(!frame(activity).guarding() || frame(activity).ourDamage!=hp-e.hull) throw new AssertionError("defense visual differs from real damage");
        });
        runner.waitForIdleSync();Thread.sleep(2500);runner.screenshot(activity,"v0.13-defend.png");
    }
    private static BattleFrame frame(MainActivity activity) throws Exception {
        Dialog dialog=(Dialog)field("eventDialog").get(activity);
        android.view.View scene=dialog.getWindow().getDecorView().findViewWithTag("battle-scene");
        if(scene==null) throw new AssertionError("battle scene missing");
        Field snapshot=BattleSceneView.class.getDeclaredField("frame");snapshot.setAccessible(true);return (BattleFrame)snapshot.get(scene);
    }
    private static void checkRetreat(MainActivity activity,UiSmoke runner) throws Exception {
        perform(runner,()->{
            Dialog dialog=(Dialog)field("eventDialog").get(activity);
            android.widget.ScrollView scroll=dialog.getWindow().getDecorView().findViewWithTag("boss-scroll");
            scroll.fullScroll(android.view.View.FOCUS_DOWN);
        });runner.waitForIdleSync();Thread.sleep(400);
        perform(runner,()->{
            Dialog dialog=(Dialog)field("eventDialog").get(activity);
            android.view.View retreat=dialog.getWindow().getDecorView().findViewWithTag("boss-retreat");
            android.graphics.Rect bounds=new android.graphics.Rect();
            if(!retreat.getGlobalVisibleRect(bounds) || bounds.height()<retreat.getHeight()*.9) throw new AssertionError("boss retreat inaccessible");
        });
        runner.screenshot(activity,"v0.9-boss-retreat.png");
    }
    private static void close(MainActivity activity,String name) throws Exception {
        Dialog dialog=(Dialog)field(name).get(activity);if(dialog!=null) dialog.dismiss();field(name).set(activity,null);
    }
}
