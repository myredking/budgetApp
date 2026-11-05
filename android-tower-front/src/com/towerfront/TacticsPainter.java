package com.towerfront;
import android.graphics.*;

public final class TacticsPainter {
    private final Paint p=new Paint(3);
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
    public void controls(Canvas c,Engine e,int selected) {
        box(c,18,688,364,15,0xffe0decb);
        box(c,18,688,(float)(364*e.rules.energy/100),15,0xffb5d8e8);
        text(c,"전술 에너지 "+(int)e.rules.energy+" / 100",200,699,10,SceneStyle.INK);
        for(int type=0;type<3;type++) skillButton(c,e,selected,type);
    }
    private void skillButton(Canvas c,Engine e,int selected,int type) {
        float x=18+type*124;boolean ready=e.rules.ready(type,selected);
        box(c,x,769,116,39,ready?SceneStyle.BLUE:0xfffffcf3);
        int color=ready?Color.WHITE:0xff657469;
        text(c,BattleRules.skillName(type),x+58,783,11,color);
        int cooldown=(int)Math.ceil(e.rules.cooldowns[type]);
        String state=cooldown>0?cooldown+"초 후":"에너지 "+BattleRules.cost(type);
        text(c,state,x+58,800,10,color);
    }
}
