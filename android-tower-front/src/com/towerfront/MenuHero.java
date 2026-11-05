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
        c.save();c.scale(getWidth()/400f,getHeight()/220f);world.hero(c);c.restore();
    }
}
