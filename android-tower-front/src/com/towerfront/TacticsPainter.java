package com.towerfront;
import android.graphics.*;

public final class TacticsPainter {
    private final Paint p=new Paint(3);
    private static final int[] ACCENTS={0xff4ca77e,0xff429cda,0xffd49a28};
    private static final int[] TILES={0xffe4f5df,0xffe3f2ff,0xffffefc6};
    private final GameIcons icons=new GameIcons();
    private void text(Canvas c,String text,float x,float y,float size,int color) {
        p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);
        c.drawText(text,x,y,p);
    }
    private void box(Canvas c,float x,float y,float w,float h,int color) {
        p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,9,9,p);
    }
    public void ground(Canvas c,Engine e) {
        int index=e.rules.supplyIndex;if(index<0) return;
        Engine.Tower t=e.towers.get(index);float x=BattleControls.x(t),y=BattleControls.y(t);
        float scale=SceneStyle.buildingScale(e);
        p.setColor(0x66f2d06b);c.drawOval(x-42*scale,y-19*scale,x+42*scale,y+24*scale,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xffad7519);
        c.drawOval(x-42*scale,y-19*scale,x+42*scale,y+24*scale,p);p.setStyle(Paint.Style.FILL);
        float top=y-69*scale;
        box(c,x-23,top-13,46,17,0xfff8d984);text(c,"보급",x,top,11,0xff59431b);
    }
    public void buffs(Canvas c,Engine e) {
        float scale=SceneStyle.buildingScale(e);
        for(int i=0;i<e.towers.size();i++) {
            Engine.Tower t=e.towers.get(i);float x=BattleControls.x(t),y=BattleControls.y(t);
            if(e.rules.shields[i]>0) shield(c,x,y,scale);
            if(e.rules.haste[i]>0) arrows(c,x,y,scale);
        }
    }
    private void shield(Canvas c,float x,float y,float scale) {
        RectF dome=new RectF(x-35*scale,y-60*scale,x+35*scale,y+18*scale);
        p.setColor(0x4482cef1);c.drawOval(dome,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xff398ec9);c.drawOval(dome,p);
        p.setStyle(Paint.Style.FILL);
    }
    private void arrows(Canvas c,float x,float y,float scale) {
        p.setColor(0xffe0a82e);p.setStrokeWidth(3);
        for(int i=0;i<3;i++) {
            float at=y-(10+i*9)*scale;
            c.drawLine(x-36*scale,at,x-31*scale,at-5*scale,p);
            c.drawLine(x-31*scale,at-5*scale,x-26*scale,at,p);
        }
    }
    public void controls(Canvas c,Engine e,int selected,ScreenLayout screen) {
        box(c,18,screen.energyTop,364,screen.energyHeight,0xffd9cdb4);
        box(c,18,screen.energyTop,(float)(364*e.rules.energy/100),screen.energyHeight,0xfff6d367);
        icons.draw(c,GameIcons.HASTE,32,screen.energyTop+screen.energyHeight/2,7);
        controlText(c,"스킬 에너지 "+(int)e.rules.energy+" / 100",200,
            screen.energyTop+screen.energyHeight/2+screen.font(10)/3,10,SceneStyle.INK,screen,350);
        for(int type=0;type<3;type++) skillButton(c,e,selected,type,screen);
    }
    private void controlText(Canvas c,String value,float x,float y,float size,int color,ScreenLayout screen,float limit) {
        p.setColor(color);p.setTextSize(screen.font(size));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);
        if(p.measureText(value)>limit) {
            int end=p.breakText(value,true,Math.max(0,limit-p.measureText("…")),null);value=value.substring(0,end)+"…";
        }
        c.drawText(value,x,y,p);
    }
    private void skillButton(Canvas c,Engine e,int selected,int type,ScreenLayout screen) {
        float x=18+type*124;boolean ready=e.rules.ready(type,selected);
        float top=screen.skillTop,center=top+screen.buttonHeight/2;
        box(c,x,top+3,116,screen.buttonHeight,0xffb7a17d);
        box(c,x,top,116,screen.buttonHeight,ready?TILES[type]:0xffe9e4d9);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(0xffc9b492);
        c.drawRoundRect(x,top,x+116,top+screen.buttonHeight,10,10,p);p.setStyle(Paint.Style.FILL);
        icons.draw(c,GameIcons.MEDKIT+type,x+23,center,15);
        cooldownRing(c,x+23,center,type,SkillDisplay.remaining(e.rules,type));
        controlText(c,SkillDisplay.label(type),x+78,center-3,11,SceneStyle.INK,screen,72);
        int cooldown=SkillDisplay.seconds(e.rules,type);
        if(cooldown>0) controlText(c,cooldown+"초",x+78,center+screen.font(10)+1,10,SceneStyle.INK,screen,72);
        else {
            icons.draw(c,GameIcons.HASTE,x+60,center+screen.font(10)-3,5);
            controlText(c,String.valueOf(BattleRules.cost(type)),x+85,center+screen.font(10)+1,10,0xff8d6b29,screen,48);
        }
    }
    private void cooldownRing(Canvas c,float x,float y,int type,float remaining) {
        if(remaining<=0) return;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xffc9c3b8);c.drawCircle(x,y,18,p);
        p.setColor(ACCENTS[type]);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawArc(x-18,y-18,x+18,y+18,-90,360*remaining,false,p);
        p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);
    }
}
