package com.deckball;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public final class FieldView extends View {
    final Paint paint = new Paint(3);
    final GameEngine engine;
    float progress = -1;
    public FieldView(Context context, GameEngine engine) { super(context); this.engine = engine; }
    void rect(Canvas c,float x,float y,float w,float h,int color) {
        paint.setColor(color); c.drawRect(x,y,x+w,y+h,paint);
    }
    void line(Canvas c,float x,float y,float xx,float yy,int color,float width) {
        paint.setColor(color); paint.setStrokeWidth(width); c.drawLine(x,y,xx,yy,paint);
    }
    void circle(Canvas c,float x,float y,float r,int color) {
        paint.setColor(color); c.drawCircle(x,y,r,paint);
    }
    void label(Canvas c,String text,float x,float y,float size,int color) {
        paint.setColor(color); paint.setTextSize(size); paint.setTypeface(Typeface.create("sans-serif-medium",0));
        paint.setTextAlign(Paint.Align.CENTER); c.drawText(text,x,y,paint);
    }
    void poly(Canvas c,int color,float... xy) {
        Path path=new Path(); path.moveTo(xy[0],xy[1]);
        for (int i=2;i<xy.length;i+=2) path.lineTo(xy[i],xy[i+1]);
        path.close(); paint.setColor(color); c.drawPath(path,paint);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.save(); c.scale(getWidth()/960f,getHeight()/540f);
        backdrop(c); stadium(c); diamond(c); athletes(c); scoreboard(c);
        card(c,18,150,engine.player(engine.batting ? engine.lineup[engine.batter] : engine.opponentHitter),engine.batting ? "MY BATTER" : "OPPONENT BATTER",false);
        card(c,782,150,engine.player(engine.batting ? engine.opponentPitcher : engine.pitcher),engine.batting ? "OPPONENT PITCHER" : "MY PITCHER",true);
        label(c,engine.message,480,515,18,Color.WHITE);
        if (progress>=0) timing(c);
        c.restore();
    }
    void backdrop(Canvas c) {
        paint.setShader(new LinearGradient(0,0,0,540,Color.rgb(87,155,201),Color.rgb(220,237,231),Shader.TileMode.CLAMP));
        c.drawRect(0,0,960,540,paint); paint.setShader(null);
        circle(c,718,66,28,Color.rgb(248,235,184));
        for(int i=0;i<11;i++) {
            int x=55+i*82,h=35+(i*37)%70;
            rect(c,x,180-h,60,h,Color.rgb(116,144,156));
        }
    }
    void stadium(Canvas c) {
        poly(c,Color.rgb(30,51,67),0,130,180,160,780,160,960,130,960,258,0,258);
        for(int row=0;row<7;row++) {
            int y=167+row*10;
            line(c,0,y,960,y,Color.rgb(69,91,109),5);
            for(int col=0;col<70;col++) circle(c,col*14+(row%2)*7,y-2,1.7f,Color.rgb(150+col%60,164,174));
        }
        rect(c,0,232,960,32,Color.rgb(14,58,65));
        for(int i=0;i<6;i++) label(c,"DECKBALL  /  2026",80+i*160,253,12,Color.rgb(166,209,197));
        rect(c,383,100,194,79,Color.rgb(13,27,40));
        label(c,"2026 × LEGENDS",480,127,17,Color.rgb(255,209,105));
        label(c,"BUILD YOUR OWN HISTORY",480,150,10,Color.WHITE);
        for(int x:new int[]{212,748}) {
            line(c,x,57,x,164,Color.rgb(76,94,107),5);
            rect(c,x-29,47,58,20,Color.rgb(222,234,238));
        }
    }
    void diamond(Canvas c) {
        rect(c,0,264,960,276,Color.rgb(73,125,72));
        for(int i=0;i<6;i++) {
            int y=264+i*46;
            poly(c,Color.rgb(66+i%2*12,117+i%2*12,66),0,y,960,y,960,y+23,0,y+23);
        }
        poly(c,Color.rgb(179,140,99),480,270,730,378,649,540,280,540,232,385);
        poly(c,Color.rgb(69,121,70),480,285,626,359,480,420,337,359);
        line(c,480,475,83,264,Color.rgb(241,236,208),3);
        line(c,480,475,877,264,Color.rgb(241,236,208),3);
        paint.setColor(Color.rgb(185,150,111)); c.drawOval(451,333,509,350,paint);
        base(c,630,359,engine.bases[0]); base(c,480,285,engine.bases[1]); base(c,331,359,engine.bases[2]);
        poly(c,Color.WHITE,465,475,495,475,495,486,480,496,465,486);
        line(c,415,461,448,478,Color.WHITE,2); line(c,415,461,399,489,Color.WHITE,2);
        line(c,399,489,432,506,Color.WHITE,2); line(c,432,506,448,478,Color.WHITE,2);
    }
    void base(Canvas c,float x,float y,boolean occupied) {
        poly(c,occupied ? Color.rgb(255,205,88):Color.WHITE,x-8,y,x,y-4,x+8,y,x,y+4);
    }
    void figure(Canvas c,float x,float y,float scale,int color,boolean bat) {
        c.save(); c.translate(x,y); c.scale(scale,scale);
        paint.setColor(Color.argb(65,0,0,0)); c.drawOval(-19,91,35,100,paint);
        line(c,-6,48,-10,91,Color.rgb(231,235,237),13);
        line(c,8,48,21,89,Color.rgb(197,211,220),13);
        line(c,-17,94,-3,94,Color.rgb(20,32,48),6); line(c,16,93,29,93,Color.rgb(20,32,48),6);
        poly(c,color,-18,8,14,8,19,52,-13,55);
        line(c,-12,18,-22,34,color,13); line(c,-22,34,5,28,Color.rgb(226,185,148),9);
        line(c,12,17,28,28,color,12); line(c,28,28,6,28,Color.rgb(226,185,148),9);
        circle(c,0,-5,12,Color.rgb(225,182,143));
        paint.setColor(color); c.drawOval(-14,-22,13,-3,paint);
        line(c,-12,-8,21,-8,color,5);
        if(bat) line(c,5,28,44,-44,Color.rgb(219,180,117),7);
        else circle(c,9,27,12,Color.rgb(166,101,54));
        label(c,"D",0,32,13,Color.WHITE); c.restore();
    }
    void athletes(Canvas c) {
        int navy=Color.rgb(26,49,80), red=Color.rgb(156,46,57);
        figure(c,480,297,.42f,red,false);
        figure(c,602,294,.32f,red,false); figure(c,350,300,.32f,red,false);
        figure(c,275,263,.26f,red,false); figure(c,675,263,.26f,red,false);
        figure(c,425,334,1.55f,navy,true);
        if(progress>=0) {
            float t=progress;
            circle(c,480-14*t,325+147*t,3+t*7,Color.WHITE);
        }
    }
    void scoreboard(Canvas c) {
        rect(c,16,14,272,99,Color.argb(230,11,25,39));
        rect(c,16,14,5,99,Color.rgb(76,211,191));
        label(c,"VISITOR",84,42,17,Color.WHITE); label(c,""+engine.away,177,42,22,Color.WHITE);
        label(c,"MY DECK",84,72,17,Color.rgb(255,211,113)); label(c,""+engine.home,177,72,22,Color.WHITE);
        label(c,engine.inning+"회 "+(engine.batting?"말":"초"),246,45,21,Color.WHITE);
        label(c,"B "+engine.balls+"   S "+engine.strikes+"   O "+engine.outs,146,98,15,Color.rgb(103,224,193));
        rect(c,694,14,248,55,Color.argb(225,11,25,39));
        label(c,"2026 SEASON  /  "+new String[]{"EASY","NORMAL","HARD"}[engine.difficulty],818,38,15,Color.WHITE);
        label(c,engine.batting?"BATTING · 공격":"PITCHING · 수비",818,58,12,Color.rgb(103,224,193));
    }
    void card(Canvas c,int x,int y,GameEngine.Player p,String title,boolean pitcher) {
        int accent=p.legend?Color.rgb(242,191,93):Color.rgb(83,205,196);
        rect(c,x,y,160,306,Color.argb(240,15,31,47)); rect(c,x,y,160,4,accent);
        label(c,title,x+80,y+24,12,accent);
        label(c,""+(pitcher?p.control:Math.round((p.contact+p.power)/2f)),x+27,y+56,29,Color.WHITE);
        label(c,p.position,x+132,y+53,15,accent);
        figure(c,x+82,y+87,.87f,Color.rgb(34,63,97),!pitcher);
        label(c,p.name,x+80,y+194,23,Color.WHITE);
        label(c,p.team+" · "+p.year,x+80,y+217,13,accent);
        label(c,"컨택   파워   제구",x+80,y+245,12,Color.rgb(164,181,196));
        label(c,p.contact+"      "+p.power+"      "+p.control,x+80,y+266,17,Color.WHITE);
        String record=p.record.split(" · ")[0];
        label(c,record,x+80,y+292,13,accent);
    }
    void timing(Canvas c) {
        rect(c,315,85,330,30,Color.argb(230,11,25,39));
        float target=engine.batting?.78f:.65f,window=(float)engine.timingWindow();
        rect(c,325+(target-window)*310,91,window*620,18,Color.rgb(255,202,83));
        line(c,325+progress*310,86,325+progress*310,114,Color.WHITE,3);
    }
}
