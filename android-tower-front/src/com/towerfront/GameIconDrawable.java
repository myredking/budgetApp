package com.towerfront;
import android.graphics.*;
import android.graphics.drawable.Drawable;

public final class GameIconDrawable extends Drawable {
    private final GameIcons icons=new GameIcons();
    private final int type,size;
    private int alpha=255;
    public GameIconDrawable(int kind,int pixels) {type=kind;size=pixels;}
    @Override public void draw(Canvas c) {
        Rect b=getBounds();int layer=c.saveLayerAlpha(b.left,b.top,b.right,b.bottom,alpha);
        icons.draw(c,type,b.exactCenterX(),b.exactCenterY(),Math.min(b.width(),b.height())*.46f);c.restoreToCount(layer);
    }
    @Override public void setAlpha(int value) {alpha=value;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() {return PixelFormat.TRANSLUCENT;}
    @Override public int getIntrinsicWidth() {return size;}
    @Override public int getIntrinsicHeight() {return size;}
}
