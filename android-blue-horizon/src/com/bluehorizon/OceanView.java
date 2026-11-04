package com.bluehorizon;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/** All artwork is drawn specifically for this game, without external assets. */
public final class OceanView extends View {
    public interface PortTap { void select(int target); }
    private final Engine game;
    private final PortTap listener;
    private final Paint paint=new Paint(3);
    private float width,height;
    public OceanView(Context context,Engine state,PortTap tap) {
        super(context);game=state;listener=tap;
        setContentDescription("다섯 항구의 항해 지도. 아래 출항할 항구 선택 버튼으로도 이동할 수 있습니다.");
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);width=getWidth();height=getHeight();
        canvas.save();canvas.scale(width/360f,height/300f);
        sea(canvas);routes(canvas);islands(canvas);compass(canvas);
        for(int i=0;i<5;i++) port(canvas,i);
        ship(canvas,Engine.POS[game.port][0]*360+22,Engine.POS[game.port][1]*250+32);
        paint.setTextAlign(Paint.Align.LEFT);paint.setTypeface(Typeface.create("serif",Typeface.ITALIC));
        paint.setTextSize(12);paint.setColor(0xff8ab3bc);canvas.drawText("The sea remembers every journey",18,283,paint);
        canvas.restore();
    }
    private void sea(Canvas c) {
        paint.setShader(new LinearGradient(0,0,360,300,new int[]{0xff1e4a5a,0xff102f43,0xff173c4c},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(0,8,360,296),16,16,paint);paint.setShader(null);
        paint.setColor(0xff2b5363);paint.setStrokeWidth(.6f);
        for(int x=30;x<360;x+=30) c.drawLine(x,10,x,270,paint);
        for(int y=30;y<270;y+=30) c.drawLine(0,y,360,y,paint);
        paint.setColor(0xff396070);
        for(int i=0;i<34;i++) {
            float x=(i*71)%345,y=25+(i*43)%225;
            c.drawArc(new RectF(x,y,x+12,y+4),0,170,false,paint);
        }
    }
    private void routes(Canvas c) {
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.2f);
        paint.setPathEffect(new DashPathEffect(new float[]{3,5},0));paint.setColor(0xff71959c);
        for(int i=0;i<4;i++) {
            Path path=new Path();path.moveTo(Engine.POS[i][0]*360,Engine.POS[i][1]*250+10);
            path.lineTo(Engine.POS[i+1][0]*360,Engine.POS[i+1][1]*250+10);c.drawPath(path,paint);
        }
        paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
    }
    private void islands(Canvas c) {
        island(c,new float[]{15,25,60,22,84,47,70,65,86,87,55,104,34,88,12,98,0,65});
        island(c,new float[]{99,108,132,91,152,116,147,140,168,157,139,172,117,151,95,146});
        island(c,new float[]{177,26,218,19,242,39,234,65,215,80,181,66,167,48});
        island(c,new float[]{239,117,274,109,290,140,280,162,304,184,280,193,259,171,232,149});
        island(c,new float[]{296,192,321,183,348,204,359,238,336,251,303,233,288,215});
    }
    private void island(Canvas c,float[] coords) {
        Path p=new Path();p.moveTo(coords[0],coords[1]);
        for(int i=2;i<coords.length;i+=2) p.lineTo(coords[i],coords[i+1]);p.close();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(9);paint.setColor(0xff356271);c.drawPath(p,paint);
        paint.setStrokeWidth(3);paint.setColor(0xffb7af7c);c.drawPath(p,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xff708776);c.drawPath(p,paint);
        paint.setColor(0xff56756b);
        float x=coords[0]+14,y=coords[1]+19;
        Path mountain=new Path();mountain.moveTo(x-8,y+9);mountain.lineTo(x,y-8);mountain.lineTo(x+8,y+9);mountain.close();c.drawPath(mountain,paint);
    }
    private void port(Canvas c,int i) {
        float x=Engine.POS[i][0]*360,y=Engine.POS[i][1]*250+10;
        paint.setColor(i==game.port?0xffe8c483:0xffd8e3d9);c.drawCircle(x,y,6,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);c.drawCircle(x,y,10,paint);paint.setStyle(Paint.Style.FILL);
        String name=Engine.PORTS[i]+(i==game.port?" ⚑":"");
        paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(13);
        paint.setTextAlign(Paint.Align.CENTER);
        float length=paint.measureText(name);
        paint.setColor(0xe6102536);c.drawRoundRect(new RectF(x-length/2-7,y+14,x+length/2+7,y+37),6,6,paint);
        paint.setColor(i==game.port?0xffe8c483:0xffecede5);c.drawText(name,x,y+30,paint);
    }
    private void ship(Canvas c,float x,float y) {
        c.save();c.translate(x,y);paint.setColor(0xffdeb779);
        Path hull=new Path();hull.moveTo(-10,5);hull.lineTo(10,5);hull.lineTo(5,11);hull.lineTo(-5,11);hull.close();c.drawPath(hull,paint);
        paint.setStrokeWidth(1.5f);c.drawLine(0,-17,0,6,paint);
        Path sail=new Path();sail.moveTo(-2,-16);sail.lineTo(-2,2);sail.lineTo(-15,2);sail.close();
        paint.setColor(0xfff1e9d4);c.drawPath(sail,paint);c.restore();
    }
    private void compass(Canvas c) {
        c.save();c.translate(315,50);paint.setColor(0xffa4b6ab);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);c.drawCircle(0,0,18,paint);
        c.drawLine(-22,0,22,0,paint);c.drawLine(0,-22,0,22,paint);paint.setStyle(Paint.Style.FILL);
        Path arrow=new Path();arrow.moveTo(0,-17);arrow.lineTo(-4,8);arrow.lineTo(4,8);arrow.close();c.drawPath(arrow,paint);
        paint.setTextSize(10);paint.setTextAlign(Paint.Align.CENTER);c.drawText("N",0,-26,paint);c.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()!=MotionEvent.ACTION_UP) return true;
        float x=event.getX()/getWidth()*360,y=event.getY()/getHeight()*300;
        for(int i=0;i<5;i++) {
            float dx=x-Engine.POS[i][0]*360,dy=y-(Engine.POS[i][1]*250+20);
            if(dx*dx+dy*dy<25*25) { performClick();listener.select(i);return true; }
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
