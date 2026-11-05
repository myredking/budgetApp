package com.towerfront;
import android.graphics.*;

/** Small clay diorama buildings, drawn at logical battlefield coordinates. */
public final class BuildingPainter {
    private final Paint p=new Paint(3);
    private void polygon(Canvas c,int color,float... points) {
        Path path=new Path();path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2) path.lineTo(points[i],points[i+1]);
        path.close();p.setColor(color);c.drawPath(path,p);
    }
    private int shade(int color,float factor) {
        return Color.rgb((int)(Color.red(color)*factor),(int)(Color.green(color)*factor),(int)(Color.blue(color)*factor));
    }
    private void block(Canvas c,float x,float y,float w,float d,float h,int color) {
        polygon(c,shade(color,.72f),x-w/2,y-h,x,y+d/2-h,x,y+d/2,x-w/2,y);
        polygon(c,shade(color,.88f),x,y+d/2-h,x+w/2,y-h,x+w/2,y,x,y+d/2);
        polygon(c,color,x,y-d/2-h,x+w/2,y-h,x,y+d/2-h,x-w/2,y-h);
    }
    public void draw(Canvas c,Engine.Tower t,float x,float y,boolean selected,float pop,double time) {
        c.save();c.translate(x,y);c.scale(pop,pop);
        p.setColor(0x33254124);c.drawOval(-27,4,39,23,p);
        block(c,0,7,65,34,6,0xffe6d7b7);
        if(selected) selection(c,time);
        int color=SceneStyle.team(t.owner);
        switch(t.kind) {
            case Engine.FACTORY: factory(c,color,time);break;
            case Engine.RELAY: relay(c,color,time);break;
            case Engine.FORTRESS: fortress(c,color);break;
            default: headquarters(c,color);break;
        }
        flag(c,color,time);badge(c,t,color);c.restore();
    }
    private void selection(Canvas c,double time) {
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xffffedac);
        c.drawOval(-38,-14,38,23,p);p.setStyle(Paint.Style.FILL);
        p.setColor(0x44fff2bc);c.drawOval(-35,-12,35,21,p);
    }
    private void headquarters(Canvas c,int color) {
        block(c,0,0,49,27,31,0xfff4e9ce);
        block(c,0,-31,53,31,6,color);
        polygon(c,shade(color,.84f),-27,-37,0,-56,27,-37,0,-22);
        polygon(c,color,-27,-37,0,-56,0,-42,0,-22);
        polygon(c,0xff304954,3,-18,12,-22,12,-2,3,2);
        windows(c,-20,-22,3);
    }
    private void factory(Canvas c,int color,double time) {
        block(c,0,1,54,30,22,0xfff2dfb8);
        block(c,0,-21,57,32,5,color);
        block(c,-11,-20,11,11,29,0xffd7c6a6);
        block(c,-11,-49,13,13,4,color);
        block(c,12,-16,10,10,21,0xffd7c6a6);
        p.setColor(0xccfffaf0);
        for(int i=0;i<3;i++) {
            float drift=(float)((time*.55+i*.33)%1);
            c.drawCircle(-11+drift*9,-57-drift*20,3+drift*4,p);
        }
        windows(c,-21,-15,4);
    }
    private void relay(Canvas c,int color,double time) {
        block(c,0,1,43,27,20,0xfff3e4c4);block(c,0,-19,46,29,5,color);
        p.setStrokeWidth(4);p.setColor(0xff52655f);c.drawLine(0,-23,0,-47,p);
        p.setColor(0xffece8d9);c.drawOval(-15,-54,15,-37,p);
        p.setColor(color);c.drawOval(-9,-50,9,-41,p);
        p.setColor(0xffefb94d);c.drawCircle(0,-47,4,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(0x77417f8b);
        c.drawArc(-22,-67,22,-27,205,110,false,p);p.setStyle(Paint.Style.FILL);
        windows(c,-15,-13,2);
    }
    private void fortress(Canvas c,int color) {
        block(c,0,2,55,31,26,0xffded5bc);
        block(c,0,-24,58,34,5,color);
        for(int side=-1;side<=1;side+=2) {
            block(c,side*23,2,15,18,37,0xffe8dfc8);
            block(c,side*23,-35,18,21,5,color);
        }
        polygon(c,0xff304954,-6,-17,3,-22,10,-17,10,0,-6,5);
        p.setColor(0xfff5cd69);c.drawCircle(0,-29,5,p);
    }
    private void windows(Canvas c,float x,float y,int count) {
        for(int i=0;i<count;i++) {
            polygon(c,0xff426f80,x+i*8,y,x+i*8+4,y+2,x+i*8+4,y+9,x+i*8,y+7);
            p.setColor(0xff9bcbd0);c.drawLine(x+i*8+1,y+2,x+i*8+1,y+6,p);
        }
    }
    private void flag(Canvas c,int color,double time) {
        p.setColor(0xff52615d);p.setStrokeWidth(1.7f);c.drawLine(23,-13,23,-49,p);
        float wave=(float)Math.sin(time*3)*2;
        polygon(c,color,23,-49,37,-46+wave,34,-36+wave,23,-39);
        p.setColor(0xffffd77c);c.drawCircle(23,-49,2,p);
    }
    private void badge(Canvas c,Engine.Tower t,int color) {
        p.setColor(0x33344330);c.drawRoundRect(-21,18,23,41,10,10,p);
        p.setColor(0xff24394b);c.drawRoundRect(-23,16,21,39,9,9,p);
        p.setColor(color);c.drawCircle(-16,27,3,p);
        p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        p.setTextSize(17);p.setTextAlign(Paint.Align.CENTER);c.drawText(String.valueOf(t.units),3,33,p);
        p.setTextSize(9);p.setColor(SceneStyle.INK);c.drawText(SceneStyle.title(t.kind),0,51,p);
        p.setColor(0xffe5b849);
        for(int i=0;i<t.rank;i++) c.drawCircle(-7+i*6,8,2,p);
    }
}
