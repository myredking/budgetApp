package com.bluehorizon;
import android.app.Instrumentation;
import android.view.View;
import android.widget.TextView;
import android.graphics.Rect;

public final class ChromeUiChecks {
    public static void run(MainActivity activity,Instrumentation runner) {
        Throwable[] failure={null};
        runner.runOnMainSync(()->{try { verify(activity); } catch(Throwable error) { failure[0]=error; }});
        if(failure[0]!=null) throw new AssertionError(failure[0]);
    }
    private static void verify(MainActivity activity) {
            townLabels(activity);
            View root=activity.getWindow().getDecorView();
            for(String name:new String[]{"마을","항해","교역","동료","일지"}) {
                TextView item=root.findViewWithTag("nav-"+name);
                if(item==null || item.getCompoundDrawables()[1]==null) throw new AssertionError("illustrated navigation missing: "+name);
                Rect bounds=new Rect();
                if(!item.getGlobalVisibleRect(bounds) || bounds.height()<item.getHeight()*.95) throw new AssertionError("navigation clipped: "+name);
                if(item.getHeight()<48*activity.getResources().getDisplayMetrics().density) throw new AssertionError("navigation touch target too small");
            }
            for(String name:new String[]{"gold","cargo","fame"}) {
                TextView stat=root.findViewWithTag("hud-"+name);
                if(stat==null || stat.getCompoundDrawables()[0]==null) throw new AssertionError("resource icon missing: "+name);
            }
    }
    private static void townLabels(MainActivity activity) {
        TownView town=new TownView(activity,0,place->{});town.layout(0,0,600,500);
        android.graphics.Paint text=new android.graphics.Paint();
        text.setTextSize(14*activity.getResources().getDisplayMetrics().scaledDensity);
        android.graphics.Paint.FontMetrics metrics=text.getFontMetrics();
        float padding=8*activity.getResources().getDisplayMetrics().density;
        if(town.labelBounds(0).height()<metrics.descent-metrics.ascent+padding-.1f) throw new AssertionError("town label background clips enlarged glyphs");
        for(int i=0;i<4;i++) {
            android.graphics.RectF plaque=town.labelBounds(i);
            if(town.hitLocation(plaque.centerX(),plaque.top+1)!=i) throw new AssertionError("visible plaque edge must accept touch");
        }
    }
}
