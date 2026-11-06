package com.towerfront;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
public final class MenuHero extends View {
    private final WorldRenderer world;
    public MenuHero(Context context) {
        super(context);world=new WorldRenderer(context);
        setContentDescription("푸른 기지와 붉은 요새가 있는 섬 전장 미리보기");
    }
    @Override protected void onDraw(Canvas c) {
        float scale=Math.max(getWidth()/400f,getHeight()/220f);
        c.save();c.translate((getWidth()-400*scale)/2,(getHeight()-220*scale)/2);
        c.scale(scale,scale);world.hero(c);c.restore();
    }
}
