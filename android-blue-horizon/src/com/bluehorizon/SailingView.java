package com.bluehorizon;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

public final class SailingView extends View {
    private final Paint paint=new Paint(3);
    private final Bitmap ships;
    private final int tier;
    private final Runnable done;
    private final ShipMotion motion;
    private final RouteInset inset;
    private final VoyageTimeline timeline=new VoyageTimeline();
    private float bearing;
    private long previous,elapsed;
    private boolean finished;
    public SailingView(Context context,int level,int from,int to,Runnable callback) {
        super(context);tier=level-1;done=callback;
        motion=new ShipMotion(from,to);
        bearing=motion.heading(0);inset=new RouteInset(context,from,to,motion);
        ships=SceneView.load(context,"sailing-ships-v1.png");previous=SystemClock.uptimeMillis();
        setContentDescription("파도를 가르며 항해하는 배");
    }
    public void pause() { timeline.pause(); }
    public void resume() { timeline.resume();previous=SystemClock.uptimeMillis();invalidate(); }
    public void speed(int value) { timeline.speed(value); }
    public void stop() { finished=true;removeCallbacks(done); }
    @Override protected void onDetachedFromWindow() { stop();super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);if(finished) return;
        long now=SystemClock.uptimeMillis();timeline.advance(now-previous);elapsed=timeline.elapsed();previous=now;
        updateBearing();canvas.drawColor(0xff16647b);waves(canvas);ship(canvas);
        inset.draw(canvas,getWidth(),motion.position(Math.min(1,elapsed/5500.0)));
        paint.setColor(0xffe4bf7b);canvas.drawRect(24,getHeight()-30,24+(getWidth()-48)*Math.min(1f,elapsed/5500f),getHeight()-22,paint);
        if(timeline.done()) { finished=true;post(done);return; }
        if(!timeline.paused()) postInvalidateOnAnimation();
    }
    private void updateBearing() {
        float target=motion.heading(Math.min(1,elapsed/5500.0)),difference=(target-bearing+540)%360-180;
        bearing+=difference*.06f;
    }
    private void waves(Canvas canvas) {
        paint.setStrokeWidth(3);paint.setColor(0x558be0e5);
        for(int row=0;row<18;row++) {
            float y=(row*70+elapsed*.08f)%(getHeight()+70)-35;
            for(int col=0;col<5;col++) {
                float x=col*getWidth()/4f+(float)Math.sin(row)*28;
                canvas.drawLine(x,y,x+38,y+5,paint);
            }
        }
    }
    private void wake(Canvas canvas,float size) {
        paint.setColor(0x99d4fcf5);paint.setStrokeWidth(2);
        for(int i=0;i<7;i++) {
            float pulse=(elapsed%650)/650f,spread=size*(.08f+i*.025f+pulse*.03f),y=size*.65f+i*10+pulse*10;
            paint.setAlpha(150-i*18);
            canvas.drawLine(-spread,y,-spread-12,y+12,paint);canvas.drawLine(spread,y,spread+12,y+12,paint);
        }
    }
    private void ship(Canvas canvas) {
        if(ships==null) return;
        paint.setAlpha(255);
        float angle=bearing+(float)Math.sin(elapsed/650f)*2;
        float size=ShipMotion.fitSize(getWidth(),getHeight(),angle),x=getWidth()/2f+(float)Math.sin(elapsed/1000f)*getWidth()*.015f;
        float y=(getHeight()+110)/2f+(float)Math.sin(elapsed/300f)*3;
        canvas.save();canvas.translate(x,y);canvas.rotate(angle);wake(canvas,size);paint.setAlpha(255);
        int width=ships.getWidth()/3;
        canvas.drawBitmap(ships,new Rect(tier*width,0,(tier+1)*width,ships.getHeight()),
            new RectF(-size/2,-size,size/2,size),paint);canvas.restore();
    }
}
