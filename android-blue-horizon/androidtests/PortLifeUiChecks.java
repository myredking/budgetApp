package com.bluehorizon;
import android.app.*;
import android.content.SharedPreferences;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;

public final class PortLifeUiChecks {
    public static void run(MainActivity activity,UiSmoke runner) throws Exception {
        Field game=field("game");Engine original=(Engine)game.get(activity);
        SharedPreferences prefs=activity.getSharedPreferences("blue_horizon_v1",0);
        String save=prefs.getString("save",null),backup=prefs.getString("backup",null);
        try {
            perform(runner,()->{game.set(activity,new Engine());field("tab").setInt(activity,7);invoke(activity,"render");invoke(activity,"localStory");});
            perform(runner,()->{AlertDialog d=(AlertDialog)field("dialogueDialog").get(activity);ScrollView scroll=d.getWindow().getDecorView().findViewWithTag("port-life-scroll");scroll.fullScroll(View.FOCUS_DOWN);});
            runner.waitForIdleSync();Thread.sleep(1500);runner.screenshot(activity,"v0.14-port-story.png");
            perform(runner,()->exercise(activity));
            runner.waitForIdleSync();Thread.sleep(1200);runner.screenshot(activity,"v0.14-companion.png");
            perform(runner,()->chapterBoundary(activity));
        } finally {
            perform(runner,()->{
                AlertDialog d=(AlertDialog)field("dialogueDialog").get(activity);if(d!=null) d.dismiss();
                d=(AlertDialog)field("storyDialog").get(activity);if(d!=null) d.dismiss();
                game.set(activity,original);field("tab").setInt(activity,7);invoke(activity,"render");
                prefs.edit().putString("save",save).putString("backup",backup).commit();
            });
        }
    }
    private static void chapterBoundary(MainActivity activity) throws Exception {
        AlertDialog d=(AlertDialog)field("dialogueDialog").get(activity);if(d!=null) d.dismiss();field("dialogueDialog").set(activity,null);
        Engine e=new Engine();e.chapter=1;e.fame=9;e.visited=13;field("game").set(activity,e);
        invoke(activity,"render");invoke(activity,"regionalTalk",1);
        d=(AlertDialog)field("dialogueDialog").get(activity);find(d.getWindow().getDecorView(),"함께 기록하기").performClick();
        if(e.chapter!=2 || field("dialogueDialog").get(activity)!=null || field("storyDialog").get(activity)==null) throw new AssertionError("chapter and companion response must share one dialog");
        ((AlertDialog)field("storyDialog").get(activity)).dismiss();
    }
    private static void exercise(MainActivity activity) throws Exception {
        AlertDialog d=(AlertDialog)field("dialogueDialog").get(activity);
        Button paid=find(d.getWindow().getDecorView(),"품삯 받고");RectCheck(paid);paid.performClick();
        Engine e=(Engine)field("game").get(activity);
        if(e.gold!=1890 || e.day!=2 || !PortLife.done(e) || Engine.decode(e.encode())==null) throw new AssertionError("local story UI reward/save mismatch");
        invoke(activity,"regionalTalk",1);
        d=(AlertDialog)field("dialogueDialog").get(activity);find(d.getWindow().getDecorView(),"이야기 듣기").performClick();
        if(!PortLife.talked(e,1) || e.town.morale!=77 || Engine.decode(e.encode())==null) throw new AssertionError("companion UI reward/save mismatch");
    }
    private static void RectCheck(View button) {
        android.graphics.Rect bounds=new android.graphics.Rect();
        if(button==null || !button.getGlobalVisibleRect(bounds) || bounds.height()<button.getHeight()*.9) throw new AssertionError("local choice is clipped");
    }
    private static Button find(View view,String prefix) {
        if(view instanceof Button && ((Button)view).getText().toString().startsWith(prefix)) return (Button)view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            Button match=find(((ViewGroup)view).getChildAt(i),prefix);if(match!=null) return match;
        }
        return null;
    }
    private static Field field(String name) throws Exception { Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f; }
    private static void invoke(MainActivity activity,String name) throws Exception { Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity); }
    private static void invoke(MainActivity activity,String name,int person) throws Exception { Method m=MainActivity.class.getDeclaredMethod(name,int.class);m.setAccessible(true);m.invoke(activity,person); }
    private interface Action { void run() throws Exception; }
    private static void perform(UiSmoke runner,Action action) throws Exception {
        Throwable[] failure={null};runner.runOnMainSync(()->{try { action.run(); } catch(Throwable error) { failure[0]=error; }});
        if(failure[0]!=null) throw new AssertionError(failure[0]);
    }
}
