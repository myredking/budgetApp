package com.bluehorizon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.List;

/** Natural Earth coastlines, original styling, and world/region port selection. */
public final class OceanView extends View {
    public interface PortTap { void select(int target); }
    private final Engine game;
    private final PortTap listener;
    private final Paint paint=new Paint(3);
    private final MapProjection projection;
    private final MapLabels labels;
    private final List<float[]> coast;
    public OceanView(Context context,Engine state,int region,PortTap tap) {
        super(context);game=state;listener=tap;projection=new MapProjection(region);
        labels=region<0?null:new MapLabels(projection);coast=Coastline.load(context);
        setContentDescription("세계 항해 지도. 아래 항구 목록으로도 출항할 수 있습니다.");
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/360f,getHeight()/300f);
        sea(canvas);canvas.save();canvas.clipRect(4,12,356,265);
        for(float[] ring:coast) land(canvas,ring);
        routes(canvas);
        if(labels!=null) portLabels(canvas);
        for(int port:World.portsIn(projection.region)) marker(canvas,port);
        if(projection.visible(game.port)) {
            float[] p=projection.point(World.LON[game.port],World.LAT[game.port]);ship(canvas,p[0]+12,p[1]-15);
        }
        canvas.restore();caption(canvas);canvas.restore();
    }
    private void sea(Canvas c) {
        paint.setShader(new LinearGradient(0,0,360,300,new int[]{0xff1e4a5a,0xff102f43,0xff173c4c},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(0,8,360,296),16,16,paint);paint.setShader(null);
        paint.setColor(0xff2b5363);paint.setStrokeWidth(.6f);
        for(int x=30;x<360;x+=30) c.drawLine(x,10,x,265,paint);
        for(int y=30;y<265;y+=30) c.drawLine(0,y,360,y,paint);
    }
    private void land(Canvas c,float[] ring) {
        Path path=new Path();float[] start=projection.point(ring[0],ring[1]);path.moveTo(start[0],start[1]);
        for(int i=2;i<ring.length;i+=2) {
            float[] p=projection.point(ring[i],ring[i+1]);path.lineTo(p[0],p[1]);
        }
        path.close();paint.setStyle(Paint.Style.FILL);paint.setColor(0xff657e70);c.drawPath(path,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffb4ad82);paint.setStrokeWidth(.65f);c.drawPath(path,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private void routes(Canvas c) {
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.8f);
        paint.setPathEffect(new DashPathEffect(new float[]{2,4},0));paint.setColor(0xff6a9297);
        for(int[] edge:WorldRoutes.EDGES) drawLeg(c,WorldRoutes.leg(edge[0],edge[1]));
        paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
    }
    private void drawLeg(Canvas c,float[][] points) {
        Path path=new Path();float[] first=projection.point(points[0][0],points[0][1]);path.moveTo(first[0],first[1]);
        for(int i=1;i<points.length;i++) {
            float[] p=projection.point(points[i][0],points[i][1]);path.lineTo(p[0],p[1]);
        }
        c.drawPath(path,paint);
    }
    private void marker(Canvas c,int port) {
        float[] p=projection.point(World.LON[port],World.LAT[port]);
        paint.setColor(game.hasVisited(port)?0xffe8c483:0xffbecfc9);
        c.drawCircle(p[0],p[1],projection.region<0?2.4f:4,paint);
        if(port==game.port) {
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);c.drawCircle(p[0],p[1],7,paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
    private void portLabels(Canvas c) {
        int[] ports=World.portsIn(projection.region);
        paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(11);
        paint.setTextAlign(Paint.Align.CENTER);
        for(int i=0;i<ports.length;i++) {
            float[] box=labels.boxes[i],p=projection.point(World.LON[ports[i]],World.LAT[ports[i]]);
            float center=(box[0]+box[2])/2;
            paint.setStrokeWidth(.6f);paint.setColor(0xff9ab0a4);c.drawLine(p[0],p[1],center,box[1]+9,paint);
            paint.setColor(0xed102536);c.drawRoundRect(new RectF(box[0],box[1],box[2],box[3]),4,4,paint);
            paint.setColor(ports[i]==game.port?0xffe8c483:0xffecede5);
            c.drawText(World.PORTS[ports[i]],center,box[1]+13,paint);
        }
    }
    private void ship(Canvas c,float x,float y) {
        c.save();c.translate(x,y);paint.setColor(0xffdeb779);
        Path hull=new Path();hull.moveTo(-8,4);hull.lineTo(8,4);hull.lineTo(4,9);hull.lineTo(-4,9);hull.close();c.drawPath(hull,paint);
        paint.setStrokeWidth(1.5f);c.drawLine(0,-14,0,5,paint);
        Path sail=new Path();sail.moveTo(-2,-13);sail.lineTo(-2,2);sail.lineTo(-12,2);sail.close();
        paint.setColor(0xfff1e9d4);c.drawPath(sail,paint);c.restore();
    }
    private void caption(Canvas c) {
        paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        paint.setTextSize(11);paint.setColor(0xffd5d0b1);
        c.drawText("금색: 방문한 항구   ·   회색: 미방문",12,282,paint);
        if(projection.region<0) {
            paint.setTextSize(10);paint.setColor(0xffe8c483);
            c.drawText("WORLD ATLAS · 56 PORTS",14,28,paint);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()!=MotionEvent.ACTION_UP) return true;
        float x=event.getX()/getWidth()*360,y=event.getY()/getHeight()*300;
        int port=labels==null?-1:labels.hit(x,y);
        if(port<0) port=projection.hit(x,y);
        if(port>=0) { performClick();listener.select(port); }
        return true;
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
