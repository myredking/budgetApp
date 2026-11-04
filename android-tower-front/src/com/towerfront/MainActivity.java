package com.towerfront;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
public final class MainActivity extends Activity {
    private BattleView game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);game=new BattleView();setContentView(game);
    }
    @Override public void onPause() {super.onPause();game.running=false;}
    @Override public void onResume() {super.onResume();if(game!=null) {game.last=0;game.running=true;game.invalidate();}}
    private final class BattleView extends View {
        private final Paint p=new Paint(3);
        private Engine engine;
        private int selected=-1,stage;
        private boolean paused=false,running=true;
        private long last;
        private float scale=1,top=0;
        BattleView() {
            super(MainActivity.this);
            stage=getPreferences(0).getInt("stage",1);engine=new Engine(stage);
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            setContentDescription("타워 프론트 전장. 아군 타워 선택 후 목표 타워를 터치하세요.");
        }
        private int color(int owner) {return owner==1?Color.rgb(63,207,238):owner==2?Color.rgb(255,114,103):Color.rgb(129,146,164);}
        private void text(Canvas c,String s,float x,float y,float size,int color) {
            p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);
        }
        private void box(Canvas c,float x,float y,float w,float h,int color) {
            p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,18,18,p);
        }
        @Override protected void onDraw(Canvas real) {
            long now=System.nanoTime();
            if(last!=0 && running && !paused) engine.update(Math.min(.05,(now-last)/1e9));
            last=now;real.drawColor(Color.rgb(9,18,31));
            scale=Math.min(getWidth()/400f,getHeight()/800f);top=(getHeight()-800*scale)/2;
            real.save();real.translate((getWidth()-400*scale)/2,top);real.scale(scale,scale);
            drawBoard(real);drawHeader(real);
            for(Engine.Fleet f:engine.fleets) drawFleet(real,f);
            for(int i=0;i<engine.towers.size();i++) drawTower(real,i);
            drawFooter(real);
            if(paused || engine.result()!=0) drawOverlay(real);
            real.restore();if(running) postInvalidateDelayed(16);
        }
        private void drawBoard(Canvas c) {
            box(c,16,142,368,530,Color.rgb(17,34,49));
            p.setColor(Color.rgb(28,48,62));p.setStrokeWidth(1);
            for(int x=32;x<380;x+=28) c.drawLine(x,154,x,660,p);
            for(int y=154;y<670;y+=28) c.drawLine(26,y,374,y,p);
            p.setColor(Color.rgb(42,64,74));p.setStrokeWidth(2);
            c.drawLine(28,400,372,400,p);
        }
        private void drawHeader(Canvas c) {
            text(c,"TOWER / FRONT",200,43,23,Color.WHITE);
            text(c,"TACTICAL CONQUEST",200,65,10,Color.rgb(121,150,170));
            text(c,String.format("작전 %02d",stage),80,112,19,Color.WHITE);
            box(c,280,84,100,40,Color.rgb(35,53,70));
            text(c,"일시정지",330,110,13,Color.WHITE);
        }
        private float x(Engine.Tower t) {return 36+(float)t.x*328;}
        private float y(Engine.Tower t) {return 155+(float)t.y*500;}
        private void drawTower(Canvas c,int index) {
            Engine.Tower t=engine.towers.get(index);float x=x(t),y=y(t);int col=color(t.owner);
            if(selected==index) {p.setColor(Color.WHITE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(x,y,37,p);p.setStyle(Paint.Style.FILL);}
            p.setColor(Color.rgb(6,17,27));c.drawCircle(x,y+7,29,p);
            p.setColor(col);c.drawCircle(x,y,28,p);
            p.setColor(Color.argb(45,255,255,255));c.drawCircle(x-6,y-9,15,p);
            text(c,String.valueOf(t.units),x,y+7,21,Color.rgb(9,28,40));
            text(c,(t.kind==Engine.FACTORY?"생산":t.kind==Engine.RELAY?"기동":"기본")+" Lv."+t.rank,x,y+46,10,col);
        }
        private void drawFleet(Canvas c,Engine.Fleet f) {
            Engine.Tower a=engine.towers.get(f.from),b=engine.towers.get(f.to);
            float ax=x(a),ay=y(a),bx=x(b),by=y(b),v=(float)f.progress;
            p.setColor(Color.argb(60,150,190,210));p.setStrokeWidth(2);c.drawLine(ax,ay,bx,by,p);
            float fx=ax+(bx-ax)*v,fy=ay+(by-ay)*v;
            p.setColor(color(f.owner));c.drawCircle(fx,fy,12,p);
            text(c,String.valueOf(f.units),fx,fy+4,10,Color.rgb(9,28,40));
        }
        private void drawFooter(Canvas c) {
            if(selected>=0 && engine.towers.get(selected).owner==1) {
                Engine.Tower t=engine.towers.get(selected);
                box(c,40,687,320,48,Color.rgb(35,70,85));
                String label=t.rank>=3?"최대 레벨": "생산 강화 · 병력 "+(20*t.rank)+" 소모";
                text(c,label,200,718,15,Color.WHITE);
            } else text(c,"아군 선택 → 목표 터치 / 드래그",200,718,16,Color.WHITE);
            text(c,"생산 타워: 생산 ×1.7 · 기동 타워: 이동 속도 증가",200,754,11,Color.rgb(145,169,187));
            text(c,"업그레이드: 생산 +50%씩 · 최대 Lv.3",200,777,11,Color.rgb(145,169,187));
        }
        private void drawOverlay(Canvas c) {
            p.setColor(Color.argb(220,5,14,26));c.drawRect(0,0,400,800,p);
            int result=engine.result();
            text(c,paused?"작전 일시정지":result==1?"작전 성공":"작전 실패",200,335,32,Color.WHITE);
            text(c,result==1?"전장을 확보했습니다": "병력을 모아 새로운 전략을 시도하세요",200,376,13,Color.rgb(150,177,195));
            box(c,70,415,260,58,color(1));
            text(c,paused?"계속하기":result==1?"다음 작전":"다시 도전",200,451,18,Color.rgb(9,28,40));
            box(c,70,495,260,48,Color.rgb(39,58,77));text(c,"현재 작전 다시 시작",200,525,15,Color.WHITE);
        }
        private int hit(float px,float py) {
            for(int i=0;i<engine.towers.size();i++) {
                Engine.Tower t=engine.towers.get(i);
                if(Math.hypot(px-x(t),py-y(t))<36) return i;
            }
            return -1;
        }
        private void restart(boolean next) {
            if(next) stage++;
            getPreferences(0).edit().putInt("stage",stage).apply();
            engine=new Engine(stage);selected=-1;paused=false;last=0;
        }
        private void overlayTouch(float px,float py) {
            if(px<70 || px>330) return;
            if(py>=495 && py<=543) {restart(false);return;}
            if(py<415 || py>473) return;
            if(paused) {paused=false;last=0;return;}
            restart(engine.result()==1);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent event) {
            float px=(event.getX()-(getWidth()-400*scale)/2)/scale;
            float py=(event.getY()-top)/scale;
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_CANCEL) {selected=-1;return true;}
            if(paused || engine.result()!=0) {if(action==MotionEvent.ACTION_UP) overlayTouch(px,py);return true;}
            if(action==MotionEvent.ACTION_DOWN) handleDown(px,py);
            if(action==MotionEvent.ACTION_UP) handleUp(px,py);
            return true;
        }
        private boolean inside(float px,float py,float left,float top,float right,float bottom) {
            return px>=left && px<=right && py>=top && py<=bottom;
        }
        private void handleDown(float px,float py) {
            if(inside(px,py,40,687,360,735)) {
                engine.upgrade(selected);return;
            }
            if(inside(px,py,280,84,380,124)) {paused=true;return;}
            int target=hit(px,py);
            if(selected>=0 && target>=0 && selected!=target) {engine.send(selected,target);selected=-1;return;}
            if(target>=0 && engine.towers.get(target).owner==1) selected=target;
        }
        private void handleUp(float px,float py) {
            int target=hit(px,py);
            if(selected>=0 && target>=0 && target!=selected) {engine.send(selected,target);selected=-1;}
        }
    }
}
