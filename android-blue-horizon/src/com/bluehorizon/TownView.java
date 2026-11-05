package com.bluehorizon;
import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.animation.*;
import java.util.function.IntConsumer;

public final class TownView extends SceneView {
    private final Paint paint=new Paint(3);
    private final IntConsumer enter;
    private ValueAnimator walking;
    private float progress;
    private int destination=3;
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
        captain(canvas);
    }
    public void walkTo(int location) {
        if(walking!=null || !isAttachedToWindow()) return;
        destination=location;walking=ValueAnimator.ofFloat(0,1);walking.setDuration(850);
        walking.addUpdateListener(value->{progress=(float)value.getAnimatedValue();invalidate();});
        walking.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator value) { cancelled=true; }
            @Override public void onAnimationEnd(Animator value) {
                walking=null;if(!cancelled && isAttachedToWindow()) enter.accept(destination);
            }
        });walking.start();
    }
    public void cancelWalk() { if(walking!=null) walking.cancel();walking=null;progress=0;invalidate(); }
    @Override protected void onDetachedFromWindow() { cancelWalk();super.onDetachedFromWindow(); }
    private void captain(Canvas canvas) {
        float[] point=TownWalk.position(destination,progress);float stride=walking==null?0:(float)Math.sin(progress*30)*3;
        canvas.save();canvas.translate(point[0]*getWidth(),point[1]*getHeight());canvas.scale(getWidth()/360f,getWidth()/360f);
        paint.setColor(0x6630261a);canvas.drawOval(-11,-3,11,4,paint);
        paint.setStrokeWidth(4);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0xff30272c);
        canvas.drawLine(-3,-8,-4-stride,0,paint);canvas.drawLine(3,-8,4+stride,0,paint);
        paint.setColor(0xff163e60);canvas.drawRoundRect(-7,-24,7,-7,4,4,paint);
        paint.setColor(0xffe4bf7b);canvas.drawRect(-7,-11,7,-8,paint);
        paint.setColor(0xffedbb8e);canvas.drawCircle(0,-29,5,paint);
        paint.setColor(0xff19334c);canvas.drawOval(-9,-38,9,-29,paint);
        paint.setColor(0xffe4bf7b);canvas.drawLine(-10,-30,10,-30,paint);canvas.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_UP) {
            int location=TownLayout.hit(event.getX()/getWidth(),event.getY()/getHeight());
            if(location>=0) { performClick();walkTo(location); }
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
