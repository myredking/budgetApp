package com.towerfront;

import android.graphics.*;
import android.view.*;
import java.util.Locale;

public final class BattleView extends View {
    private final MainActivity host;
    private final Engine engine;
    private final BattleControls controls;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface bold=Typeface.create("sans-serif",Typeface.BOLD);
    private final BattleEffects effects=new BattleEffects();
    private final EffectRenderer renderer=new EffectRenderer();
    private final WorldRenderer world;
    private final int[] depth;
    private float scale=1,left,top,dragX,dragY;
    private boolean running=true,paused,dragging,reported;
    private int pressedButton=-1;
    private long last;
    private double sinceSave,soundCooldown;
    private String notice="아군을 선택한 뒤 목표를 터치하세요";
    private double noticeTime=5;
    private static final int WHITE=SceneStyle.INK,GRAY=0xff657469;
    public BattleView(MainActivity activity,Engine state) {
        super(activity);host=activity;engine=state;controls=new BattleControls(engine);
        world=new WorldRenderer(activity);depth=SceneStyle.towerOrder(engine);
        setFocusable(true);setContentDescription("타워 프론트 전장. 아군 선택 후 목표 터치 또는 드래그.");
    }
    public void suspend() {running=false;paused=true;controls.cancel();last=0;}
    public void wake() {running=true;last=0;invalidate();}
    public void pause() {
        paused=true;controls.cancel();setContentDescription("전투 일시정지 · 작전 "+engine.level);
        host.saveBattle(false);invalidate();
    }
    public void back() {
        if(paused || engine.result()!=0) host.showMenu();else pause();
    }
    private int color(int owner) {return SceneStyle.team(owner);}
    private void text(Canvas c,String value,float x,float y,float size,int color) {
        paint.setTypeface(bold);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(color);c.drawText(value,x,y,paint);
    }
    private void box(Canvas c,float x,float y,float width,float height,int color) {
        paint.setColor(color);c.drawRoundRect(x,y,x+width,y+height,14,14,paint);
    }
    private void line(Canvas c,float ax,float ay,float bx,float by,int color,float width) {
        paint.setColor(color);paint.setStrokeWidth(width);c.drawLine(ax,ay,bx,by,paint);
    }
    @Override protected void onDraw(Canvas real) {
        long now=System.nanoTime();double dt=last==0?0:Math.min(.05,(now-last)/1e9);last=now;
        if(running && !paused) tick(dt);
        real.drawColor(MainActivity.BG);
        scale=Math.min(getWidth()/400f,getHeight()/820f);
        left=(getWidth()-400*scale)/2;top=(getHeight()-820*scale)/2;
        real.save();real.translate(left,top);real.scale(scale,scale);
        board(real);header(real);
        for(Engine.Fleet f:engine.fleets) world.fleet(real,engine,f);
        for(int i:depth) world.tower(real,engine.towers.get(i),controls.selected==i,effects.towerScale(i),SceneStyle.buildingScale(engine),engine.elapsed);
        renderer.drawBursts(real,effects);drawDrag(real);footer(real);
        drawOverlay(real);
        real.restore();if(running) postInvalidateDelayed(paused?100:16);
    }
    private void drawOverlay(Canvas c) {
        if(paused || engine.result()!=0) overlay(c);
        if(engine.result()==1 && !paused) renderer.drawConfetti(c,effects);
    }
    private void tick(double dt) {
        engine.update(dt);controls.validate();noticeTime=Math.max(0,noticeTime-dt);
        effects.update(dt);soundCooldown=Math.max(0,soundCooldown-dt);
        processImpacts();sinceSave+=dt;
        if(sinceSave>=5) {host.saveBattle(false);sinceSave=0;}
        if(engine.result()!=0 && !reported) {
            reported=true;host.saveBattle(true);host.feedback(engine.result()==1);
            if(host.visuals && engine.result()==1) effects.celebrate();
        }
    }
    private void processImpacts() {
        int sound=-1;
        for(Engine.Impact event:engine.drainImpacts()) {
            Engine.Tower t=engine.towers.get(event.target);
            if(host.visuals) effects.impact(event,BattleControls.x(t),BattleControls.y(t));
            sound=Math.max(sound,event.kind);
            if(event.kind==Engine.CAPTURE && event.owner==1) notice("타워 점령! 전선이 확장되었습니다");
        }
        if(sound>=0 && soundCooldown==0 && engine.result()==0) {
            host.battleSound(sound);soundCooldown=.15;
        }
    }
    private void board(Canvas c) {
        world.terrain(c,new RectF(14,166,386,658));
    }
    private void header(Canvas c) {
        text(c,"TOWER / FRONT",133,40,21,WHITE);
        text(c,engine.skirmish?"자유전 · "+Skirmish.name(engine.mapId):String.format(Locale.KOREA,"%02d  %s",engine.level,Campaign.name(engine.level)),143,70,14,GRAY);
        box(c,294,23,88,48,MainActivity.CARD);text(c,"일시정지",338,53,13,WHITE);
        int[] strength=new int[12];
        for(Engine.Tower t:engine.towers) strength[t.owner]+=t.units;
        for(Engine.Fleet f:engine.fleets) strength[f.owner]+=f.units;
        text(c,"아군 "+strength[1],65,111,14,color(1));
        text(c,clock(engine.elapsed),200,111,18,WHITE);
        int total=0;for(int i=2;i<strength.length;i++) total+=strength[i];
        text(c,"적군 "+total,335,111,14,color(2));
        text(c,Campaign.difficultyName(engine.difficulty)+(engine.skirmish?" · AI "+engine.enemyCount+"진영 · 생존 "+engine.remainingEnemies():" · 미션 "+engine.level+" / 100"),200,146,11,GRAY);
    }
    private String clock(double seconds) {
        int value=(int)seconds;
        return String.format(Locale.KOREA,"%02d:%02d",value/60,value%60);
    }
    private void drawDrag(Canvas c) {
        if(!dragging || controls.selected<0) return;
        Engine.Tower t=engine.towers.get(controls.selected);
        line(c,BattleControls.x(t),BattleControls.y(t),dragX,dragY,WHITE,2);
        paint.setColor(WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
        c.drawCircle(dragX,dragY,12,paint);paint.setStyle(Paint.Style.FILL);
    }
    private void footer(Canvas c) {
        text(c,noticeTime>0?notice:"아군 선택 → 목표 터치 / 드래그",200,685,13,WHITE);
        box(c,18,708,174,50,MainActivity.CARD);text(c,"출병 "+(controls.all?"100%":"50%"),105,739,16,MainActivity.CYAN);
        box(c,208,708,174,50,canUpgrade()?MainActivity.CYAN:MainActivity.CARD);
        text(c,upgradeLabel(),295,739,14,canUpgrade()?Color.WHITE:GRAY);
        if(engine.enemyCount>1) factionLegend(c);
        else text(c,"생산 ×1.7 · 기동 속도 증가 · 방어 피해 −25%",200,789,11,GRAY);
        text(c,"중립은 생산하지 않습니다 · 점령 후 레벨 유지",200,810,10,GRAY);
    }
    private void factionLegend(Canvas c) {
        for(int team=1;team<=engine.enemyCount+1;team++) {
            float x=24+(team-1)*32;
            paint.setColor(SceneStyle.team(team));c.drawCircle(x,785,4,paint);
            text(c,team==1?"나":String.valueOf(team-1),x+10,789,10,WHITE);
        }
    }
    private boolean canUpgrade() {
        if(controls.selected<0) return false;
        Engine.Tower t=engine.towers.get(controls.selected);
        return t.owner==1 && t.rank<3 && t.units>=20*t.rank;
    }
    private String upgradeLabel() {
        if(controls.selected<0) return "아군 선택 후 강화";
        Engine.Tower t=engine.towers.get(controls.selected);
        return t.rank>=3?"최대 레벨":"강화 · 병력 "+20*t.rank;
    }
    private void overlay(Canvas c) {
        paint.setColor(Color.argb(245,245,239,223));c.drawRect(0,0,400,820,paint);
        int result=engine.result();boolean win=result==1;
        text(c,resultTitle(result),200,236,32,WHITE);
        text(c,(engine.skirmish?Skirmish.name(engine.mapId):Campaign.name(engine.level))+" · "+Campaign.difficultyName(engine.difficulty),200,275,15,GRAY);
        if(win && !engine.skirmish) text(c,"★★★".substring(0,engine.medals())+"☆☆☆".substring(engine.medals()),200,325,34,0xffb17b15);
        else text(c,overlayMessage(win),200,320,13,GRAY);
        text(c,"시간 "+clock(engine.elapsed)+"   점령 "+engine.captures+"   출병 "+engine.dispatched,200,371,13,WHITE);
        box(c,56,410,288,56,MainActivity.CYAN);
        text(c,primaryLabel(result),200,446,18,Color.WHITE);
        box(c,56,482,288,52,MainActivity.CARD);text(c,"현재 작전 다시 시작",200,515,15,WHITE);
        box(c,56,550,288,52,MainActivity.CARD);text(c,"시작 화면 · 자동 저장",200,583,15,WHITE);
        text(c,engine.skirmish?"자유전은 미션 기록에 영향을 주지 않습니다":"별은 난이도별 최고 기록으로 저장됩니다",200,654,12,GRAY);
    }
    private String resultTitle(int result) {
        if(result==0) return "전투 일시정지";
        if(result==2) return "전투 실패";
        if(engine.skirmish) return "자유전 승리!";
        return engine.level==Campaign.COUNT?"미션 100 완료!":"미션 성공";
    }
    private String overlayMessage(boolean win) {
        if(win && engine.skirmish) return "최후까지 살아남은 진영입니다!";
        return paused?"전투를 안전하게 저장했습니다":"생산 기지 확보와 증원으로 다시 도전하세요";
    }
    private String primaryLabel(int result) {
        if(result==0) return "계속하기";
        if(result==2) return "다시 도전";
        if(engine.skirmish) return "자유전 설정";
        return engine.level==Campaign.COUNT?"미션 지도":"다음 미션";
    }
    private boolean inside(float x,float y,float a,float b,float w,float h) {
        return x>=a && x<=a+w && y>=b && y<=b+h;
    }
    private int buttonAt(float x,float y,boolean overlay) {
        if(overlay) return overlayButtonAt(x,y);
        if(inside(x,y,294,23,88,48)) return 0;
        if(inside(x,y,18,708,174,50)) return 1;
        if(inside(x,y,208,708,174,50)) return 2;
        return -1;
    }
    private int overlayButtonAt(float x,float y) {
        if(inside(x,y,56,410,288,56)) return 3;
        if(inside(x,y,56,482,288,52)) return 4;
        if(inside(x,y,56,550,288,52)) return 5;
        return -1;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getPointerCount()>1) {controls.cancel();dragging=false;pressedButton=-1;return true;}
        float x=(event.getX()-left)/scale,y=(event.getY()-top)/scale;
        int action=event.getActionMasked();boolean overlay=paused || engine.result()!=0;
        if(action==MotionEvent.ACTION_CANCEL) {controls.cancel();dragging=false;pressedButton=-1;return true;}
        if(action==MotionEvent.ACTION_DOWN) touchDown(x,y,overlay);
        if(action==MotionEvent.ACTION_MOVE) {dragX=x;dragY=y;}
        if(action==MotionEvent.ACTION_UP) touchUp(x,y,overlay);
        return true;
    }
    private void touchDown(float x,float y,boolean overlay) {
        pressedButton=buttonAt(x,y,overlay);
        if(overlay || pressedButton>=0) return;
        controls.press(BattleControls.hit(engine,x,y));dragX=x;dragY=y;dragging=true;
    }
    private void touchUp(float x,float y,boolean overlay) {
        dragging=false;performClick();
        if(pressedButton>=0) {
            int action=pressedButton;pressedButton=-1;
            if(buttonAt(x,y,overlay)==action) activate(action);
            return;
        }
        if(overlay) return;
        int result=controls.release(BattleControls.hit(engine,x,y));
        if(result==1) {notice("병력 출발");signal(true);}
        if(result==-1) {notice("출병할 병력이 부족합니다");signal(false);}
    }
    @Override public boolean performClick() {super.performClick();return true;}
    private void activate(int action) {
        switch(action) {
            case 0: pause();break;
            case 1: controls.all=!controls.all;notice(controls.all?"모든 병력 출병 · 빈 타워에 주의하세요":"병력 절반 출병");break;
            case 2: upgrade();break;
            case 3: primaryAction();break;
            case 4: host.retry();break;
            case 5: host.showMenu();break;
            default: break;
        }
    }
    private void primaryAction() {
        int result=engine.result();
        if(result==0) {
            paused=false;last=0;setContentDescription("타워 프론트 전장 · 작전 "+engine.level);return;
        }
        if(result==1) host.next();else host.restartBattle();
    }
    private void upgrade() {
        boolean ok=engine.upgrade(controls.selected);
        notice(ok?"생산 강화 완료":controls.selected<0?"아군 타워를 먼저 선택하세요":"병력이 부족하거나 최대 레벨입니다");
        signal(ok);if(ok) host.saveBattle(false);
    }
    private void signal(boolean positive) {
        host.feedback(positive);
        if(host.haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }
    private void notice(String value) {notice=value;noticeTime=3;}
}
