package com.deckball;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public final class FieldView extends View {
    final Paint paint = new Paint(3);
    final GameEngine engine;
    float progress = -1;
    public FieldView(Context context, GameEngine engine) { super(context); this.engine = engine; }
    void label(Canvas c, String text, float x, float y, float size, int color) {
        paint.setColor(color); paint.setTextSize(size); paint.setTypeface(Typeface.create("sans-serif-medium",0));
        paint.setTextAlign(Paint.Align.CENTER); c.drawText(text,x,y,paint);
    }
    void line(Canvas c,float x,float y,float xx,float yy,int color,float width) {
        paint.setColor(color); paint.setStrokeWidth(width); c.drawLine(x,y,xx,yy,paint);
    }
    void circle(Canvas c,float x,float y,float r,int color) {
        paint.setColor(color); c.drawCircle(x,y,r,paint);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save(); canvas.scale(getWidth()/440f,getHeight()/470f);
        paint.setShader(new LinearGradient(0,0,0,470,Color.rgb(20,43,57),Color.rgb(8,24,32),Shader.TileMode.CLAMP));
        canvas.drawRoundRect(0,0,440,470,22,22,paint); paint.setShader(null);
        label(canvas,"NIGHT AT THE BALLPARK",220,32,13,Color.rgb(128,169,183));
        stadium(canvas); diamond(canvas); players(canvas); hud(canvas);
        canvas.restore();
    }
    void stadium(Canvas c) {
        paint.setColor(Color.rgb(29,74,64));
        c.drawOval(26,65,414,382,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(13); paint.setColor(Color.rgb(47,77,90));
        c.drawArc(17,53,423,387,180,180,false,paint); paint.setStyle(Paint.Style.FILL);
        for (int i=0;i<9;i++) {
            float x = 38 + i*45;
            circle(c,x,58,2,Color.rgb(255,217,142));
        }
        line(c,220,355,57,161,Color.rgb(171,198,170),1);
        line(c,220,355,383,161,Color.rgb(171,198,170),1);
    }
    void diamond(Canvas c) {
        Path clay = new Path(); clay.moveTo(220,355); clay.lineTo(324,264);
        clay.lineTo(220,173); clay.lineTo(116,264); clay.close();
        paint.setColor(Color.rgb(157,112,74)); c.drawPath(clay,paint);
        Path grass = new Path(); grass.moveTo(220,326); grass.lineTo(292,264);
        grass.lineTo(220,201); grass.lineTo(148,264); grass.close();
        paint.setColor(Color.rgb(38,94,70)); c.drawPath(grass,paint);
        base(c,324,264,engine.bases[0]); base(c,220,173,engine.bases[1]); base(c,116,264,engine.bases[2]);
        base(c,220,355,false); circle(c,220,264,16,Color.rgb(157,112,74));
        line(c,213,264,227,264,Color.WHITE,3);
    }
    void base(Canvas c,float x,float y,boolean occupied) {
        c.save(); c.rotate(45,x,y); paint.setColor(occupied ? Color.rgb(255,200,80) : Color.WHITE);
        c.drawRect(x-6,y-6,x+6,y+6,paint); c.restore();
    }
    void person(Canvas c,float x,float y,int color) {
        line(c,x,y+4,x,y+17,color,8); circle(c,x,y,5,Color.rgb(238,195,151));
        line(c,x-2,y+16,x-5,y+25,Color.WHITE,3);
        line(c,x+2,y+16,x+5,y+25,Color.WHITE,3);
        circle(c,x,y-3,5,color);
    }
    void players(Canvas c) {
        int blue = Color.rgb(89,170,221), gold = Color.rgb(255,199,79);
        person(c,220,246,engine.batting ? blue : gold);
        person(c,202,329,engine.batting ? gold : blue);
        line(c,193,335,184,314,Color.rgb(222,181,123),4);
        person(c,301,229,blue); person(c,140,229,blue);
        person(c,183,203,blue); person(c,261,203,blue);
        person(c,110,150,blue); person(c,220,116,blue); person(c,330,150,blue);
        if (progress >= 0) {
            float t = progress;
            circle(c,220 - t*5,270 + t*75,4 + t*3,Color.WHITE);
        }
    }
    void hud(Canvas c) {
        String side = engine.inning + "회 " + (engine.batting ? "말  /  ATTACK" : "초  /  DEFENSE");
        label(c,engine.finished ? "FULL TIME" : side,220,402,17,Color.WHITE);
        label(c,"B " + engine.balls + "   S " + engine.strikes + "   O " + engine.outs,220,427,16,Color.rgb(105,223,195));
        String player = engine.batting ? (engine.batter+1) + "번 " + engine.player(engine.lineup[engine.batter]).name : "SP " + engine.player(engine.pitcher).name;
        label(c,player,220,451,14,Color.LTGRAY);
        if (progress >= 0) timing(c);
    }
    void timing(Canvas c) {
        paint.setColor(Color.rgb(9,20,30)); c.drawRoundRect(60,82,380,115,8,8,paint);
        float target = engine.batting ? .78f : .65f;
        paint.setColor(Color.rgb(255,202,83)); c.drawRect(70+(target-.07f)*300,87,70+(target+.07f)*300,110,paint);
        line(c,70+progress*300,84,70+progress*300,113,Color.WHITE,4);
    }
}
