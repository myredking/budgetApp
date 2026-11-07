package com.towerfront;
import android.graphics.*;

public final class SkillEffectsPainter {
    private final Paint p=new Paint(3);
    private final GameIcons icons=new GameIcons();
    private static final int[] COLORS={0xff69d5a1,0xff69bfff,0xffffd35c};
    public void world(Canvas c,BattleEffects effects) {
        for(BattleEffects.SkillBurst b:effects.casts) {
            float age=(float)b.age;int alpha=(int)(255*(1-age/.85f));
            p.setColor(COLORS[b.type]);p.setAlpha(Math.max(0,alpha));
            if(b.type==0) heal(c,b,age);else if(b.type==1) shield(c,b,age);else speed(c,b,age);
            float y=b.y-65-age*20;
            int layer=c.saveLayerAlpha(b.x-22,y-22,b.x+22,y+24,Math.max(0,alpha));
            icons.draw(c,GameIcons.MEDKIT+b.type,b.x,y,16);c.restoreToCount(layer);
        }
        p.setAlpha(255);
    }
    private void heal(Canvas c,BattleEffects.SkillBurst b,float age) {
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawOval(b.x-30-age*25,b.y-12-age*8,b.x+30+age*25,b.y+20+age*8,p);
        for(int i=0;i<7;i++) {
            float x=b.x-30+i*10,y=b.y-20-age*65-(i%2)*14;
            c.drawLine(x-4,y,x+4,y,p);c.drawLine(x,y-4,x,y+4,p);
        }
        p.setStyle(Paint.Style.FILL);
    }
    private void shield(Canvas c,BattleEffects.SkillBurst b,float age) {
        float r=35+age*25;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
        c.drawOval(b.x-r,b.y-65-age*10,b.x+r,b.y+18,p);
        c.drawOval(b.x-r-5,b.y-69-age*10,b.x+r+5,b.y+22,p);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<5;i++) {double a=i*Math.PI*.4;float x=b.x+(float)Math.cos(a)*r,y=b.y-25+(float)Math.sin(a)*r;c.drawCircle(x,y,3,p);}
    }
    private void speed(Canvas c,BattleEffects.SkillBurst b,float age) {
        p.setStrokeWidth(4);p.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<5;i++) {float y=b.y-45+i*13-age*30;c.drawLine(b.x-50-age*20,y+12,b.x+15+age*25,y-14,p);}
        p.setStrokeCap(Paint.Cap.BUTT);
    }
    public void screen(Canvas c,BattleEffects effects,ScreenLayout s) {
        if(effects.casts.isEmpty()) return;
        BattleEffects.SkillBurst b=effects.casts.get(effects.casts.size()-1);
        float fade=Math.max(0,1-(float)b.age/.85f);p.setColor(COLORS[b.type]);
        p.setAlpha((int)(100*fade));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(9);
        c.drawRoundRect(s.boardLeft+5,s.boardTop+5,s.boardRight-5,s.boardBottom-5,18,18,p);
        p.setAlpha((int)(220*fade));p.setStrokeWidth(2);
        c.drawRoundRect(s.boardLeft+8,s.boardTop+8,s.boardRight-8,s.boardBottom-8,16,16,p);
        p.setAlpha(255);p.setStyle(Paint.Style.FILL);
    }
}
