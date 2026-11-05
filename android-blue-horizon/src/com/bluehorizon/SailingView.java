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
    private long previous,elapsed;
    private boolean paused,finished;
    public SailingView(Context context,int level,int from,int to,Runnable callback) {
        super(context);tier=level-1;done=callback;
        motion=new ShipMotion(from,to);
        ships=SceneView.load(context,"sailing-ships-v1.png");previous=SystemClock.uptimeMillis();
        setContentDescription("파도를 가르며 항해하는 배");
    }
    public void pause() { paused=true; }
    public void resume() { paused=false;previous=SystemClock.uptimeMillis();invalidate(); }
    public void stop() { finished=true;removeCallbacks(done); }
    @Override protected void onDetachedFromWindow() { stop();super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);if(finished) return;
        long now=SystemClock.uptimeMillis();if(!paused) elapsed+=Math.min(100,now-previous);previous=now;
        canvas.drawColor(0xff16647b);waves(canvas);ship(canvas);route(canvas);
        paint.setColor(0xffe4bf7b);canvas.drawRect(24,getHeight()-30,24+(getWidth()-48)*Math.min(1f,elapsed/5500f),getHeight()-22,paint);
        if(elapsed>=5500) { finished=true;post(done);return; }
        if(!paused) postInvalidateOnAnimation();
    }
    private void route(Canvas canvas) {
        float scale=getWidth()/380f;
        paint.setColor(0xcc101f30);canvas.drawRect(8,8,getWidth()-8,110,paint);
        paint.setColor(0xff65c9b5);paint.setStrokeWidth(2);
        for(int i=1;i<motion.points.size();i++) {
            float[] a=motion.points.get(i-1),b=motion.points.get(i);
            if(Math.abs(a[0]-b[0])>180) continue;
            canvas.drawLine((a[0]+190)*scale,60-a[1]*.5f,(b[0]+190)*scale,60-b[1]*.5f,paint);
        }
        float[] p=motion.position(Math.min(1,elapsed/5500.0));paint.setColor(0xffffdda0);
        canvas.drawCircle((p[0]+190)*scale,60-p[1]*.5f,6,paint);
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
        paint.setColor(0x99d4fcf5);float x=getWidth()/2f,y=getHeight()*.65f;
        for(int i=0;i<7;i++) {
            float spread=18+i*10+(elapsed%500)/35f;
            canvas.drawLine(x-spread,y+i*17,x-spread-20,y+i*17+22,paint);
            canvas.drawLine(x+spread,y+i*17,x+spread+20,y+i*17+22,paint);
        }
    }
    private void ship(Canvas canvas) {
        if(ships==null) return;
        paint.setAlpha(255);
        float size=ShipMotion.spriteSize(getWidth(),getHeight()),x=getWidth()/2f,y=(getHeight()+85)/2f+(float)Math.sin(elapsed/300f)*5;
        canvas.save();canvas.rotate((float)Math.sin(elapsed/650f)*3,x,y);
        int width=ships.getWidth()/3;
        canvas.drawBitmap(ships,new Rect(tier*width,0,(tier+1)*width,ships.getHeight()),
            new RectF(x-size/2,y-size,x+size/2,y+size),paint);canvas.restore();
    }
}
