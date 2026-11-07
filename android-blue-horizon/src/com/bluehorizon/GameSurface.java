package com.bluehorizon;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Chart-paper grain, inset brass rules and engraved corner rivets. */
public final class GameSurface extends Drawable {
    private final Paint p=new Paint(3);
    private final boolean gold;
    public GameSurface(boolean gold) { this.gold=gold; }
    @Override public void draw(Canvas c) {
        Rect b=getBounds();float d=c.getDensity()/160f;if(d==0) d=1;
        p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,b.top,0,b.bottom,
            gold?0xfff0d59d:0xff253f4b,gold?0xffb98a43:0xff102a36,Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(b),5*d,5*d,p);p.setShader(null);
        p.setColor(gold?0x18634420:0x0cdbd6b1);p.setStrokeWidth(d);
        for(float y=b.top+8*d;y<b.bottom;y+=7*d) c.drawLine(b.left+4*d,y,b.right-4*d,y,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(d);p.setColor(gold?0xfffbe3ae:0xff927744);
        c.drawRoundRect(new RectF(b.left+2*d,b.top+2*d,b.right-2*d,b.bottom-2*d),4*d,4*d,p);
        p.setColor(gold?0xff9a6e34:0xff3e5960);
        c.drawRect(b.left+5*d,b.top+5*d,b.right-5*d,b.bottom-5*d,p);
        rivets(c,b,d);
    }
    private void rivets(Canvas c,Rect b,float d) {
        p.setStyle(Paint.Style.FILL);p.setColor(gold?0xff805c30:0xffd2ac6c);
        for(float x:new float[]{b.left+5*d,b.right-5*d}) for(float y:new float[]{b.top+5*d,b.bottom-5*d}) c.drawCircle(x,y,1.5f*d,p);
    }
    @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { p.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
