package com.bluehorizon;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;

public final class BattleSceneView extends View {
    private final Bitmap ships;
    private final Paint p=new Paint(3);
    private final int tier;
    private final BattleFrame frame;
    private final long began=SystemClock.uptimeMillis();
    public BattleSceneView(Context context,int level,BattleFrame snapshot) {
        super(context);tier=level-1;frame=snapshot;ships=SceneView.load(context,"sailing-ships-v1.png");
        setTag("battle-scene");setContentDescription("우리 선체 "+frame.ourAfter+" / "+frame.ourMax+", 적 선체 "+frame.enemyAfter+" / "+frame.enemyMax+". 이번 피해: 우리 "+frame.ourDamage+", 적 "+frame.enemyDamage);
    }
    @Override protected void onDraw(Canvas c) {
        float w=getWidth(),h=getHeight(),t=frame.progress(SystemClock.uptimeMillis()-began);
        c.drawColor(0xff123749);p.setStrokeWidth(dp(1));p.setColor(0x4468a8b7);
        for(int i=0;i<9;i++) c.drawLine(0,i*h/8,w,i*h/8+dp(8),p);
        float shake=frame.ourDamage>0 && t>.7f && t<.95f?(float)Math.sin(t*120)*dp(2):0;
        float escape=frame.action==2?t*w*.18f:0;
        vessel(c,tier,w*.25f-escape+shake,h*.59f,55);vessel(c,2,w*.75f,h*.43f,-125);
        effects(c,w,h,t);bars(c,w,h,t);
        if(t<1 && isShown() && hasWindowFocus()) postInvalidateOnAnimation();
    }
    private void effects(Canvas c,float w,float h,float t) {
        shot(c,w,h,t);
        if(frame.guarding()) {
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(0xff79d6cf);
            c.drawOval(w*.12f,h*.34f,w*.38f,h*.8f,p);p.setStyle(Paint.Style.FILL);
        }
        if(frame.enemyDamage>0 && t>.4f && t<.75f) burst(c,w*.75f,h*.43f,(t-.4f)/.35f,0xffffbe68);
        if(frame.ourDamage>0 && t>.7f && t<1) burst(c,w*.25f,h*.59f,(t-.7f)/.3f,0xffa3e5ef);
        if(t>.45f) damage(c,w*.73f,h*.29f,frame.enemyDamage,t,0xffffdc91);
        if(t>.7f) damage(c,w*.26f,h*.78f,frame.ourDamage,t,0xffffa39a);
    }
    private void shot(Canvas c,float w,float h,float t) {
        if(!frame.firing() || t>=.45f) return;
        float shot=Math.min(1,t/.45f);p.setColor(0xffffd37e);
        c.drawCircle(w*(.3f+.4f*shot),h*(.55f-.1f*shot),dp(3),p);
        if(t<.1f) burst(c,w*.3f,h*.55f,t*8,0xffffd37e);
    }
    private void burst(Canvas c,float x,float y,float t,int color) {
        p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));
        c.drawCircle(x,y,dp(5)+t*dp(20),p);
        for(int i=0;i<6;i++) {
            double angle=i*Math.PI/3;float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            c.drawLine(x+dx*dp(8),y+dy*dp(8),x+dx*(dp(12)+t*dp(24)),y+dy*(dp(12)+t*dp(24)),p);
        }
        p.setStyle(Paint.Style.FILL);
    }
    private void damage(Canvas c,float x,float y,int amount,float t,int color) {
        if(amount==0) return;
        p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(18));
        p.setShadowLayer(dp(2),0,dp(1),0xff071522);p.setColor(color);c.drawText("−"+amount,x,y-t*dp(9),p);p.clearShadowLayer();
    }
    private void bars(Canvas c,float w,float h,float t) {
        float inset=dp(7),height=dp(7);p.setTextSize(dp(11));p.setTypeface(Typeface.DEFAULT_BOLD);
        health(c,inset,dp(18),w-2*inset,height,frame.enemyFraction(t),0xffe49c77,"적  "+frame.enemyAfter+" / "+frame.enemyMax);
        health(c,inset,h-dp(10),w-2*inset,height,frame.ourFraction(t),0xff71cec1,"우리  "+frame.ourAfter+" / "+frame.ourMax);
    }
    private void health(Canvas c,float x,float y,float width,float height,float value,int color,String text) {
        p.setColor(0xff081e29);c.drawRoundRect(x,y,x+width,y+height,dp(2),dp(2),p);
        p.setColor(color);c.drawRoundRect(x,y,x+width*value,y+height,dp(2),dp(2),p);
        p.setTextAlign(Paint.Align.LEFT);p.setColor(0xfff2e5c9);c.drawText(text,x,y-dp(3),p);
    }
    private void vessel(Canvas c,int ship,float x,float y,float angle) {
        if(ships==null) return;
        int cell=ships.getWidth()/3;float size=Math.min(getWidth()*.28f,getHeight()*.42f);
        c.save();c.translate(x,y);c.rotate(angle);p.setAlpha(255);
        c.drawBitmap(ships,new Rect(ship*cell,0,(ship+1)*cell,ships.getHeight()),new RectF(-size/2,-size,size/2,size),p);c.restore();
    }
    private float dp(float value) { return value*getResources().getDisplayMetrics().density; }
}
