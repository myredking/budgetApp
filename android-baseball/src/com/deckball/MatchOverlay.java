package com.deckball;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public final class MatchOverlay extends View {
    final GameEngine game;
    final Paint paint=new Paint(3);
    float progress=-1;
    public MatchOverlay(Context context,GameEngine game) {super(context);this.game=game;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    void box(Canvas c,float x,float y,float w,float h,int color) {paint.setColor(color);c.drawRoundRect(x,y,x+w,y+h,6,6,paint);}
    void text(Canvas c,String value,float x,float y,float size,int color) {
        paint.setColor(color);paint.setTextSize(size);paint.setTypeface(Typeface.create("sans-serif-medium",0));paint.setTextAlign(Paint.Align.LEFT);c.drawText(value,x,y,paint);
    }
    @Override protected void onDraw(Canvas c) {
        c.save();c.scale(getWidth()/960f,getHeight()/540f);
        score(c);cards(c);timing(c);feedback(c);
        box(c,260,496,440,31,Color.argb(210,10,21,31));
        String message=game.message.length()>30?game.message.substring(0,30):game.message;
        text(c,message,274,517,13,Color.WHITE);c.restore();
    }
    void score(Canvas c) {
        box(c,14,14,246,90,Color.argb(225,9,22,34));
        text(c,"VISITOR   "+game.away,27,39,17,Color.WHITE);
        text(c,"MY DECK   "+game.home,27,64,17,Color.rgb(255,215,128));
        text(c,game.finished?"FINAL":game.inning+"회 "+(game.batting?"말":"초"),190,40,17,Color.WHITE);
        text(c,"B "+game.balls+"   S "+game.strikes+"   O "+game.outs,27,90,14,Color.rgb(99,224,198));
        box(c,737,14,209,57,Color.argb(215,9,22,34));
        text(c,"2026 × LEGENDS · 3D",749,37,14,Color.WHITE);
        text(c,game.finished?"FINAL / 경기 종료":(game.batting?"BATTING / 공격":"PITCHING / 수비"),749,59,12,Color.rgb(99,224,198));
        for(int i=0;i<3;i++) box(c,278+i*18,22,12,12,game.bases[i]?Color.rgb(255,205,95):Color.argb(180,58,75,82));
    }
    void cards(Canvas c) {
        GameEngine.Player hitter=game.player(game.batting?game.lineup[game.batter]:game.opponentHitter);
        GameEngine.Player pitcher=game.player(game.batting?game.opponentPitcher:game.pitcher);
        card(c,14,182,hitter,false);card(c,800,182,pitcher,true);
    }
    void card(Canvas c,int x,int y,GameEngine.Player p,boolean pitcher) {
        int accent=p.legend?Color.rgb(255,206,116):Color.rgb(99,224,198);
        box(c,x,y,146,210,Color.argb(218,9,22,34));
        text(c,pitcher?"PITCHER":"BATTER",x+11,y+21,12,accent);
        text(c,""+(pitcher?game.controlRating(p):(game.contactRating(p)+game.powerRating(p))/2),x+11,y+57,30,Color.WHITE);
        text(c,p.position,x+111,y+52,14,accent);
        text(c,p.name,x+11,y+89,20,Color.WHITE);
        text(c,p.team+" · "+p.year,x+11,y+111,12,accent);
        text(c,p.record.split(" · ")[0],x+11,y+132,12,Color.WHITE);
        text(c,"컨택 "+game.contactRating(p)+"  파워 "+game.powerRating(p),x+11,y+153,11,Color.WHITE);
        String[] skills=PlayerSkills.describe(p).split(" / ");
        for(int i=0;i<Math.min(2,skills.length);i++) {
            String name=skills[i].split(" · ")[0];
            boolean waiting=name.equals("해결사") && !game.bases[1] && !game.bases[2];
            text(c,(waiting?"대기 · ":"")+name,x+11,y+177+i*17,11,waiting?Color.rgb(144,157,165):accent);
        }
    }
    void feedback(Canvas c) {
        if(progress>=0 || game.feedback.isEmpty()) return;
        box(c,340,88,280,42,Color.argb(220,9,22,34));
        text(c,game.feedback,357,116,21,Color.rgb(255,215,128));
        if(game.lastSkills.isEmpty() || game.lastSkills.equals("특수 능력 없음")) return;
        box(c,300,450,360,28,Color.argb(210,9,22,34));
        text(c,"직전 발동 · "+game.lastSkills,312,469,12,Color.rgb(99,224,198));
    }
    void timing(Canvas c) {
        if(progress<0 || game.finished) return;
        float target=game.batting?.78f:.65f,window=(float)game.timingWindow();
        box(c,329,88,302,29,Color.argb(220,9,22,34));
        box(c,335+(target-window)*290,94,window*580,17,Color.rgb(255,205,95));
        box(c,335+progress*290,89,3,27,Color.WHITE);
    }
}
