package com.towerfront;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

public final class EffectRenderer {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] palette={0xff3fcfee,0xffffd36f,0xffff776b,0xffeef8ff};
    public void drawBursts(Canvas c,BattleEffects effects) {
        c.save();c.clipRect(14,166,386,658);
        for(BattleEffects.Burst b:effects.bursts) burst(c,b);
        c.restore();
    }
    private int color(BattleEffects.Burst b) {
        if(b.impact.kind==Engine.HIT) return palette[1];
        return b.impact.owner==1?palette[0]:palette[2];
    }
    private void burst(Canvas c,BattleEffects.Burst b) {
        float age=(float)b.age;int alpha=(int)(255*Math.max(0,1-age/1.1f));
        paint.setColor(color(b));paint.setAlpha(alpha);paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        float radius=27+age*38;
        c.drawCircle(b.x,b.y,radius,paint);
        paint.setStyle(Paint.Style.FILL);
        if(b.impact.kind==Engine.REINFORCE) reinforcement(c,b,age);
        else sparks(c,b,age);
        label(c,b,age,alpha);
        paint.setAlpha(255);
    }
    private void reinforcement(Canvas c,BattleEffects.Burst b,float age) {
        for(int i=0;i<6;i++) {
            float x=b.x-22+i*9,y=b.y+22-age*65-(i%2)*8;
            c.drawRect(x-1.5f,y-5,x+1.5f,y+5,paint);
            c.drawRect(x-5,y-1.5f,x+5,y+1.5f,paint);
        }
    }
    private void sparks(Canvas c,BattleEffects.Burst b,float age) {
        for(int i=0;i<12;i++) {
            double angle=i*Math.PI/6+b.impact.target;
            float distance=28+age*(45+(i%3)*22);
            float x=b.x+(float)Math.cos(angle)*distance;
            float y=b.y+(float)Math.sin(angle)*distance+age*age*22;
            c.drawCircle(x,y,Math.max(1,3.5f-age*2),paint);
        }
    }
    private void label(Canvas c,BattleEffects.Burst b,float age,int alpha) {
        String text="−"+b.impact.amount;
        if(b.impact.kind==Engine.REINFORCE) text="+"+b.impact.amount;
        if(b.impact.kind==Engine.CAPTURE) text=b.impact.owner==1?"점령!":"적군 점령";
        paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        paint.setTextSize(17);paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(0xff09121f);paint.setAlpha(alpha);
        c.drawText(text,b.x+1,b.y-39-age*24+1,paint);
        paint.setColor(color(b));paint.setAlpha(alpha);c.drawText(text,b.x,b.y-39-age*24,paint);
    }
    public void drawConfetti(Canvas c,BattleEffects effects) {
        c.save();c.clipRect(14,110,386,395);
        for(BattleEffects.Confetti p:effects.confetti) {
            paint.setColor(palette[p.palette]);paint.setAlpha((int)(255*Math.min(1,(3.2-p.age)*2)));
            c.save();c.rotate(p.angle,p.x,p.y);c.drawRect(p.x-3,p.y-6,p.x+3,p.y+6,paint);c.restore();
        }
        paint.setAlpha(255);c.restore();
    }
}
