package com.towerfront;
import android.graphics.*;

/** Rounded toy badges drawn as vectors, so icons stay crisp on every phone. */
public final class GameIcons {
    public static final int CASTLE=0,FLAGS=1,STAR=2,MAP=3,GEAR=4,PLAY=5,MEDKIT=6,SHIELD=7,HASTE=8,HAMMER=9;
    private final Paint p=new Paint(3);
    private static final int INK=0xff425269;
    private static final int[] COLORS={0xffa4dcf4,0xffffc5ac,0xffffdf83,0xffc8e8b0,0xffd6c0f2,
        0xffafe2d3,0xffffc5d3,0xffadddfb,0xffffdf93,0xffffc7a0};
    private void fill(int color) {p.setStyle(Paint.Style.FILL);p.setColor(color);}
    private void shape(Canvas c,int color,float... xy) {
        Path path=new Path();path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2) path.lineTo(xy[i],xy[i+1]);
        path.close();fill(color);c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);
        p.setColor(INK);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
    }
    private void round(Canvas c,float l,float t,float r,float b,int color) {
        fill(color);c.drawRoundRect(l,t,r,b,4,4,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(INK);p.setStrokeWidth(1.6f);c.drawRoundRect(l,t,r,b,4,4,p);p.setStyle(Paint.Style.FILL);
    }
    public void draw(Canvas c,int type,float x,float y,float radius) {
        int kind=Math.max(0,Math.min(9,type));c.save();c.translate(x,y);c.scale(radius/24,radius/24);
        fill(0x333b5265);c.drawCircle(0,3,23,p);fill(COLORS[kind]);c.drawCircle(0,0,23,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(0xfffffff1);p.setStrokeWidth(2);c.drawCircle(0,-1,21,p);p.setStyle(Paint.Style.FILL);
        if(kind<6) scene(c,kind);else skill(c,kind);
        c.restore();
    }
    private void scene(Canvas c,int type) {
        switch(type) {
            case FLAGS: flags(c);break;
            case STAR: star(c);break;
            case MAP: map(c);break;
            case GEAR: gear(c);break;
            case PLAY: shape(c,0xfffffff1,-6,-12,12,0,-6,12);break;
            default: castle(c);break;
        }
    }
    private void skill(Canvas c,int type) {
        switch(type) {
            case MEDKIT: medkit(c);break;
            case SHIELD: shield(c);break;
            case HASTE: haste(c);break;
            default: hammer(c);break;
        }
    }
    private void face(Canvas c,float x,float y) {
        fill(INK);c.drawCircle(x-4,y,1.4f,p);c.drawCircle(x+4,y,1.4f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.3f);c.drawArc(x-3,y+1,x+3,y+6,0,180,false,p);p.setStyle(Paint.Style.FILL);
        fill(0x88f28c99);c.drawOval(x-8,y+2,x-4,y+4,p);c.drawOval(x+4,y+2,x+8,y+4,p);
    }
    private void castle(Canvas c) {
        round(c,-14,-6,14,15,0xfffff4ce);
        shape(c,0xff80b9e7,-17,-6,-10,-17,-3,-6);shape(c,0xff80b9e7,3,-6,10,-17,17,-6);
        round(c,-5,-12,5,1,0xfffff4ce);shape(c,0xffffa4a0,0,-12,0,-22,11,-18,0,-16);
        face(c,0,6);
    }
    private void flags(Canvas c) {
        p.setColor(INK);p.setStrokeWidth(2.5f);c.drawLine(-14,15,7,-17,p);c.drawLine(14,15,-7,-17,p);
        shape(c,0xffff8e9e,-6,-17,-17,-13,-9,-3,2,-7);shape(c,0xff75b6e9,7,-17,18,-13,10,-3,-1,-7);
        fill(0xfffffff1);c.drawCircle(-9,-10,2,p);c.drawCircle(10,-10,2,p);
    }
    private void star(Canvas c) {
        float[] points=new float[20];
        for(int i=0;i<10;i++) {double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?17:8;points[i*2]=(float)Math.cos(a)*r;points[i*2+1]=(float)Math.sin(a)*r;}
        shape(c,0xffffd05d,points);face(c,0,0);
    }
    private void map(Canvas c) {
        round(c,-14,-16,14,16,0xfffff5d2);
        p.setColor(0xff79bfa3);p.setStrokeWidth(3);c.drawLine(-8,8,5,4,p);c.drawLine(5,4,-4,-5,p);
        fill(0xffed8796);c.drawCircle(5,-8,5,p);fill(0xfffffff1);c.drawCircle(5,-8,2,p);
        fill(0xff7bb9e7);c.drawCircle(-8,8,3,p);
    }
    private void gear(Canvas c) {
        for(int i=0;i<8;i++) {double a=i*Math.PI/4;fill(0xffbda0e5);c.drawCircle((float)Math.cos(a)*12,(float)Math.sin(a)*12,5,p);}
        fill(0xffe7d6fa);c.drawCircle(0,0,12,p);face(c,0,-2);
    }
    private void medkit(Canvas c) {
        round(c,-7,-16,7,-5,0xfffff7e5);round(c,-15,-10,15,13,0xfffff7e5);
        fill(0xffef829c);c.drawRoundRect(-3,-7,3,5,1,1,p);c.drawRoundRect(-8,-3,8,2,1,1,p);
        fill(INK);c.drawCircle(-5,9,1.3f,p);c.drawCircle(5,9,1.3f,p);
    }
    private void shield(Canvas c) {
        shape(c,0xff77b9ed,0,-17,14,-11,12,6,0,17,-12,6,-14,-11);
        shape(c,0xffdbf3ff,0,-12,9,-8,7,4,0,11,-7,4,-9,-8);face(c,0,-2);
    }
    private void haste(Canvas c) {
        p.setColor(0xfffffff1);p.setStrokeWidth(3);c.drawLine(-17,-9,-8,-9,p);c.drawLine(-19,-3,-11,-3,p);
        shape(c,0xffffc33f,0,-17,11,-17,3,-3,12,-3,-7,18,-3,3,-12,3);
    }
    private void hammer(Canvas c) {
        round(c,-3,-2,4,17,0xffd79765);round(c,-14,-13,14,-1,0xff8bbfe5);
        fill(0xffe8f8ff);c.drawRoundRect(-10,-10,6,-7,1,1,p);
    }
}
