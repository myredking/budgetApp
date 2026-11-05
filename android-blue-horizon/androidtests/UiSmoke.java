package com.bluehorizon;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Bitmap;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;

public final class UiSmoke extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args);start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();int status=Activity.RESULT_OK;
        try {
            Intent intent=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            MainActivity activity=(MainActivity)startActivitySync(intent);
            final Throwable[] failure={null};
            runOnMainSync(()->{try { checkImport(activity); } catch(Throwable error) { failure[0]=error; }});
            if(failure[0]!=null) throw new AssertionError(failure[0]);
            TradeUiChecks.run(activity,this);
            purchaseScreenshot(activity);
            checkAnimation(activity);
            result.putString("result","PASS: in-transit import, automatic arrival, budget/quantity/max/sale UI; original progress restored");
        } catch(Throwable error) { status=Activity.RESULT_CANCELED;result.putString("error",error.toString()); }
        finish(status,result);
    }
    private void checkAnimation(MainActivity activity) throws Exception {
        Field field=MainActivity.class.getDeclaredField("game");field.setAccessible(true);
        Engine original=(Engine)field.get(activity);
        SharedPreferences prefs=activity.getSharedPreferences("blue_horizon_v1",0);
        String save=prefs.getString("save",null),backup=prefs.getString("backup",null);
        Engine journey=new Engine();TravelFlow.begin(journey,1,1);int gold=journey.gold,day=journey.day;
        try {
            runOnMainSync(()->{try { field.set(activity,journey);invoke(activity,"showSailing"); } catch(Exception error) { throw new RuntimeException(error); }});
            Thread.sleep(1400);screenshot(activity,"v0.6-sailing.png");
            awaitArrival(journey);
            if(journey.town.moving() || journey.gold!=gold || journey.day!=day) throw new AssertionError("automatic arrival failed");
        } finally {
            runOnMainSync(()->{try { field.set(activity,original);invoke(activity,"render"); } catch(Exception error) { throw new RuntimeException(error); }});
            prefs.edit().putString("save",save).putString("backup",backup).commit();
        }
    }
    private void awaitArrival(Engine journey) throws InterruptedException {
        boolean[] moving={true};
        for(int attempt=0;attempt<100;attempt++) {
            runOnMainSync(()->moving[0]=journey.town.moving());
            if(!moving[0]) return;
            Thread.sleep(200);
        }
    }
    private void purchaseScreenshot(MainActivity activity) throws Exception {
        AlertDialog[] sample={null};Throwable[] failure={null};
        runOnMainSync(()->{try { sample[0]=TradeUiChecks.sample(activity); } catch(Throwable error) { failure[0]=error; }});
        if(failure[0]!=null) throw new AssertionError(failure[0]);
        try { Thread.sleep(1000);screenshot(activity,"v0.7-order.png"); }
        finally { runOnMainSync(()->sample[0].dismiss()); }
    }
    private void screenshot(MainActivity activity,String name) throws IOException {
        Bitmap bitmap=getUiAutomation().takeScreenshot();
        try(FileOutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir(null),name))) {
            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out)) throw new IOException("Screenshot failed");
        } finally { bitmap.recycle(); }
    }
    private void checkImport(MainActivity activity) throws Exception {
        Field field=MainActivity.class.getDeclaredField("game");field.setAccessible(true);
        Engine original=(Engine)field.get(activity);String before=original.encode();
        SharedPreferences prefs=activity.getSharedPreferences("blue_horizon_v1",0);
        String save=prefs.getString("save",null),backup=prefs.getString("backup",null);
        try { importedJourney(activity,field); }
        finally {
            field.set(activity,Engine.decode(before));invoke(activity,"render");
            prefs.edit().putString("save",save).putString("backup",backup).commit();
        }
    }
    private void importedJourney(MainActivity activity,Field field) throws Exception {
        Engine journey=new Engine();TravelFlow.begin(journey,1,1);
        int gold=journey.gold,day=journey.day;
        File file=new File(activity.getFilesDir(),"ui-smoke-journey.blue.txt");
        try(FileOutputStream out=new FileOutputStream(file)) { out.write(journey.encode().getBytes(StandardCharsets.UTF_8)); }
        Method read=MainActivity.class.getDeclaredMethod("readBackup",Intent.class);read.setAccessible(true);
        read.invoke(activity,new Intent().setData(Uri.fromFile(file)));
        Field sailing=MainActivity.class.getDeclaredField("sailing");sailing.setAccessible(true);
        if(sailing.get(activity)==null) throw new AssertionError("imported journey has no sailing/arrival controls");
        invoke(activity,"finishSailing");invoke(activity,"finishSailing");
        Engine restored=(Engine)field.get(activity);
        if(restored.town.moving() || restored.gold!=gold || restored.day!=day) throw new AssertionError("arrival charged twice");
        if(!file.delete()) file.deleteOnExit();
    }
    private void invoke(MainActivity activity,String name) throws Exception {
        Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);
    }
}
