package com.bluehorizon;
import android.app.Instrumentation;
import android.widget.TextView;
import java.lang.reflect.Method;

public final class TownUiChecks {
    public static void run(MainActivity activity,Instrumentation runner) throws Exception {
        TownView[] town={null};int[] arrivals={0};
        try {
            runner.runOnMainSync(()->{
                town[0]=new TownView(activity,1,place->arrivals[0]++);activity.setContentView(town[0]);
                town[0].walkTo(0);town[0].walkTo(1);
            });
            waitForWalk(runner,arrivals);
            runner.runOnMainSync(()->{
                if(arrivals[0]!=1) throw new AssertionError("walking callback must enter only once");
                town[0].walkTo(2);town[0].cancelWalk();activity.setContentView(new TextView(activity));
            });
            Thread.sleep(1000);
            runner.runOnMainSync(()->{if(arrivals[0]!=1) throw new AssertionError("detached/cancelled walk entered facility");});
        } finally {
            Method render=MainActivity.class.getDeclaredMethod("render");render.setAccessible(true);
            runner.runOnMainSync(()->{try { render.invoke(activity); } catch(Exception error) { throw new RuntimeException(error); }});
        }
    }
    private static void waitForWalk(Instrumentation runner,int[] arrivals) throws InterruptedException {
        boolean[] complete={false};
        for(int attempt=0;attempt<30;attempt++) {
            runner.runOnMainSync(()->complete[0]=arrivals[0]>0);
            if(complete[0]) return;
            Thread.sleep(100);
        }
    }
}
