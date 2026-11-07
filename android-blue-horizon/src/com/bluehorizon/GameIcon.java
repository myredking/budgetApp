package com.bluehorizon;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original resolution-independent brass and enamel game emblems. */
public final class GameIcon extends Drawable {
    public static final int TOWN=0,SHIP=1,CARGO=2,CREW=3,BOOK=4,COIN=5,STAR=6,TOOLS=7,CUP=8,COMPASS=9;
    private final Paint p=new Paint(3);
    private final int kind;
    public GameIcon(int kind) { this.kind=kind; }
    @Override public void draw(Canvas canvas) {
        canvas.save();canvas.translate(getBounds().left,getBounds().top);
        canvas.scale(getBounds().width()/48f,getBounds().height()/48f);
        p.setStyle(Paint.Style.FILL);p.setColor(0xff112b38);canvas.drawCircle(24,25,22,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(0xffb59152);canvas.drawCircle(24,24,21,p);
        p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        symbol(canvas);canvas.restore();
    }
    private void symbol(Canvas c) {
        switch(kind) {
            case TOWN: town(c);break;
            case SHIP: ship(c);break;
            case CARGO: cargo(c);break;
            case CREW: crew(c);break;
            case BOOK: book(c);break;
            case COIN: coin(c);break;
            case STAR: star(c);break;
            case TOOLS: tools(c);break;
            case CUP: cup(c);break;
            default: compass(c);break;
        }
    }
    private void line(Canvas c,float x,float y,float a,float b) { p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xffffd58b);c.drawLine(x,y,a,b,p); }
    private void shape(Canvas c,int color,float... points) {
        Path path=new Path();path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2) path.lineTo(points[i],points[i+1]);
        path.close();p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawPath(path,p);
    }
    private void town(Canvas c) {
        shape(c,0xffcf9a60,10,22,24,11,38,22);shape(c,0xffedcd93,13,22,35,22,35,36,13,36);
        shape(c,0xff244957,21,27,27,27,27,36,21,36);line(c,10,37,38,37);line(c,30,12,30,17);
    }
    private void ship(Canvas c) {
        shape(c,0xffb9854c,9,30,39,30,33,37,16,37);line(c,24,10,24,31);
        shape(c,0xffffe3ab,22,12,12,27,22,27);shape(c,0xff71c3bc,27,14,36,27,27,27);
        line(c,10,40,19,40);line(c,26,40,36,40);
    }
    private void cargo(Canvas c) {
        shape(c,0xffe7bc77,10,17,25,11,38,18,23,24);shape(c,0xffaa743d,10,19,22,26,22,39,10,32);
        shape(c,0xffce9651,24,26,38,20,38,33,24,39);line(c,16,15,31,23);line(c,31,24,31,35);
    }
    private void crew(Canvas c) {
        p.setStyle(Paint.Style.FILL);p.setColor(0xffefd0a0);c.drawCircle(24,19,6,p);
        shape(c,0xff6ebcb5,12,35,16,27,32,27,36,35);shape(c,0xffe8bd70,15,12,24,7,33,12,30,15,18,15);
        line(c,15,16,33,16);line(c,23,28,25,34);
    }
    private void book(Canvas c) {
        shape(c,0xffe4c28b,11,12,22,14,24,17,26,14,37,12,37,34,26,36,24,39,22,36,11,34);
        line(c,24,18,24,35);p.setColor(0xff6d512f);c.drawLine(15,21,20,22,p);c.drawLine(28,22,33,21,p);
    }
    private void coin(Canvas c) {
        p.setStyle(Paint.Style.FILL);p.setColor(0xffb58232);c.drawOval(10,13,38,39,p);
        p.setColor(0xffffd477);c.drawCircle(24,23,14,p);p.setStyle(Paint.Style.STROKE);p.setColor(0xff9f6d29);p.setStrokeWidth(1.5f);c.drawCircle(24,23,10,p);
        p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(18);p.setStyle(Paint.Style.FILL);c.drawText("G",17,29,p);
    }
    private void star(Canvas c) {
        shape(c,0xff68b7b0,15,25,22,25,20,41,14,36,9,38);shape(c,0xff448e91,25,25,32,25,39,38,33,36,28,41);
        shape(c,0xffffd589,24,7,28,17,39,18,31,25,33,35,24,30,15,35,17,25,9,18,20,17);
    }
    private void tools(Canvas c) {
        line(c,14,36,33,14);line(c,14,13,35,36);shape(c,0xffe6bf7e,10,15,17,9,24,15,17,21);
        line(c,28,12,34,10);line(c,34,10,38,15);
    }
    private void cup(Canvas c) {
        shape(c,0xffe7c48b,13,16,31,16,29,34,16,34);p.setColor(0xffe7c48b);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawOval(27,18,37,29,p);
        line(c,16,37,31,37);line(c,18,11,20,8);line(c,26,11,28,8);
    }
    private void compass(Canvas c) {
        p.setStyle(Paint.Style.STROKE);p.setColor(0xffefd09a);p.setStrokeWidth(1);c.drawCircle(24,24,15,p);
        shape(c,0xffffd58b,24,7,28,24,24,41,20,24);shape(c,0xff64bfb8,7,24,24,20,41,24,24,28);
        p.setStyle(Paint.Style.FILL);p.setColor(0xfffaf0d3);c.drawCircle(24,24,3,p);
    }
    @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { p.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
