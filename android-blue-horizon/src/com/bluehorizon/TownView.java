package com.bluehorizon;
import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import java.util.function.IntConsumer;

public final class TownView extends SceneView {
    private final Paint paint=new Paint(3);
    private final IntConsumer enter;
    public TownView(Context context,int style,IntConsumer callback) {
        super(context,"town-scenes-v1.png",style);enter=callback;
        setContentDescription("항구 마을. 교역소, 조선소, 주점, 마을 광장. 아래 버튼으로도 입장할 수 있습니다.");
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
        paint.setTextSize(getResources().getDisplayMetrics().scaledDensity*14);
        for(int i=0;i<4;i++) {
            float[] point=TownLayout.point(i);float x=point[0]*getWidth(),y=point[1]*getHeight();
            float half=paint.measureText(TownLayout.NAMES[i])/2+14;
            paint.setColor(0xe6101f30);canvas.drawRoundRect(x-half,y-24,x+half,y+12,9,9,paint);
            paint.setColor(0xffffdda0);canvas.drawText(TownLayout.NAMES[i],x,y,paint);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_UP) {
            int location=TownLayout.hit(event.getX()/getWidth(),event.getY()/getHeight());
            if(location>=0) { performClick();enter.accept(location); }
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
