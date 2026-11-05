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
    private final TacticsPainter tactics=new TacticsPainter();
    private final WorldRenderer world;
    private final int[] depth;
    private float scale=1,left,top,dragX,dragY,downX,downY,lastX,lastY;
    private ScreenLayout screen=new ScreenLayout(400,820,1,1);
    private BattleCamera camera=new BattleCamera(screen);
    private boolean panCandidate,mapPanning;
    private boolean running=true,paused,dragging,reported;
    private int pressedButton=-1;
    private int downTarget=-1;
    private int previewTarget=-1;
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
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) {
        super.onSizeChanged(w,h,oldW,oldH);
        screen=new ScreenLayout(w,h,getResources().getDisplayMetrics().density,getResources().getConfiguration().fontScale);
        camera=new BattleCamera(screen);scale=screen.scale;left=screen.left;top=screen.top;cancelTouch();
        if(screen.compact) notice("지도 +로 확대 · 빈 땅을 드래그해 이동");
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
        paint.setTypeface(bold);paint.setTextSize(screen.font(size));paint.setTextAlign(Paint.Align.CENTER);
        float limit=Math.min(368,2*Math.min(x-8,392-x));
        if(paint.measureText(value)>limit) {
            int end=paint.breakText(value,true,Math.max(0,limit-paint.measureText("…")),null);value=value.substring(0,end)+"…";
        }
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
        real.save();real.translate(left,top);real.scale(scale,scale);
        drawWorld(real);header(real);footer(real);drawMapZoom(real);
        drawOverlay(real);
        real.restore();if(running) postInvalidateDelayed(paused?100:16);
    }
    private void drawWorld(Canvas c) {
        box(c,screen.boardLeft,screen.boardTop,screen.boardRight-screen.boardLeft,screen.boardBottom-screen.boardTop,MainActivity.CARD);
        c.save();c.clipRect(screen.boardLeft,screen.boardTop,screen.boardRight,screen.boardBottom);
        c.translate(camera.screenX(0),camera.screenY(0));c.scale(camera.zoom,camera.zoom);
        board(c);tactics.ground(c,engine);
        for(Engine.Fleet f:engine.fleets) world.fleet(c,engine,f);
        for(int i:depth) world.tower(c,engine.towers.get(i),controls.selected==i,effects.towerScale(i),SceneStyle.buildingScale(engine),engine.elapsed);
        tactics.buffs(c,engine);renderer.drawBursts(c,effects);drawDrag(c);c.restore();
    }
    private void drawMapZoom(Canvas c) {
        box(c,screen.zoomLeft(),screen.zoomTop(),screen.buttonHeight,screen.buttonHeight,MainActivity.CARD);
        float size=camera.overview()?24:12;
        text(c,camera.overview()?"+":"전체",screen.zoomLeft()+screen.buttonHeight/2,
            screen.zoomTop()+screen.buttonHeight/2+screen.font(size)/3,size,WHITE);
    }
    private void drawOverlay(Canvas c) {
        if(paused || engine.result()!=0) overlay(c);
        if(engine.result()==1 && !paused) renderer.drawConfetti(c,effects,screen.overlayPrimaryTop-12);
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
        boolean compact=screen.compact;
        text(c,"TOWER / FRONT",133,screen.titleY(),compact?18:21,WHITE);
        String title=engine.skirmish?"자유전 · "+Skirmish.name(engine.mapId)+" · AI"+engine.enemyCount:String.format(Locale.KOREA,"%02d  %s",engine.level,Campaign.name(engine.level));
        text(c,title,143,screen.nameY(),12,GRAY);
        float pauseTop=screen.pauseTop();
        box(c,294,pauseTop,88,screen.buttonHeight,MainActivity.CARD);
        text(c,"일시정지",338,pauseTop+screen.buttonHeight/2+screen.font(12)/3,12,WHITE);
        strength(c,screen.statusY());
        text(c,engine.rules.objectiveText(),200,screen.objectiveY(),10,WHITE);
        if(!compact) headerDetails(c);
    }
    private void strength(Canvas c,float baseline) {
        int[] strength=new int[12];
        for(Engine.Tower t:engine.towers) strength[t.owner]+=t.units;
        for(Engine.Fleet f:engine.fleets) strength[f.owner]+=f.units;
        text(c,(screen.compact?"나 ":"아군 ")+strength[1],screen.compact?48:65,baseline,14,color(1));
        text(c,clock(engine.elapsed),screen.compact?145:200,baseline,screen.compact?14:18,WHITE);
        int total=0;for(int i=2;i<strength.length;i++) total+=strength[i];
        text(c,(screen.compact?"적 ":"적군 ")+total,screen.compact?242:335,baseline,14,color(2));
    }
    private void headerDetails(Canvas c) {
        text(c,Campaign.difficultyName(engine.difficulty)+(engine.skirmish?" · 생존 "+engine.remainingEnemies()+"진영":" · 미션 "+engine.level+" / 100"),200,screen.detailsY(),10,GRAY);
        if(engine.enemyCount>1) factionLegend(c);
    }
    private String clock(double seconds) {
        int value=(int)seconds;
        return String.format(Locale.KOREA,"%02d:%02d",value/60,value%60);
    }
    private void drawDrag(Canvas c) {
        if(!dragging || controls.selected<0) return;
        Engine.Tower t=engine.towers.get(controls.selected);
        int count=controls.previewUnits(previewTarget);
        float x=dragX,y=dragY;
        if(count>0) {Engine.Tower target=engine.towers.get(previewTarget);x=BattleControls.x(target);y=BattleControls.y(target);}
        int tint=count>0?MainActivity.CYAN:GRAY;
        line(c,BattleControls.x(t),BattleControls.y(t),x,y,tint,3/camera.zoom);
        paint.setColor(tint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3/camera.zoom);
        if(count>0) c.drawRoundRect(x-48,y-72,x+48,y+48,12,12,paint);
        else c.drawCircle(x,y,12/camera.zoom,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private String previewLabel() {
        int count=controls.previewUnits(previewTarget);
        if(count>0) return (engine.towers.get(previewTarget).owner==1?"지원 ":"공격 ")+count+"명 · 손을 떼면 출병";
        if(previewTarget==controls.selected) return "목표 타워로 끌어주세요";
        if(previewTarget<0) return "출병 취소 · 목표가 없습니다";
        return "출병 불가 · 병력이 부족합니다";
    }
    private void footer(Canvas c) {
        String hint=noticeTime>0?notice:"아군 선택 → 출병 / 전술 스킬";
        if(dragging && controls.selected>=0) hint=previewLabel();
        text(c,hint,200,screen.noticeY,12,WHITE);
        box(c,18,screen.commandTop,174,screen.buttonHeight,MainActivity.CARD);
        text(c,"출병 "+(controls.all?"100%":"50%"),105,screen.commandTop+screen.buttonHeight/2+screen.font(16)/3,16,MainActivity.CYAN);
        box(c,208,screen.commandTop,174,screen.buttonHeight,canUpgrade()?MainActivity.CYAN:MainActivity.CARD);
        text(c,upgradeLabel(),295,screen.commandTop+screen.buttonHeight/2+screen.font(14)/3,14,canUpgrade()?Color.WHITE:GRAY);
        tactics.controls(c,engine,controls.selected,screen);
    }
    private void factionLegend(Canvas c) {
        for(int team=1;team<=engine.enemyCount+1;team++) {
            float x=24+(team-1)*32;
            paint.setColor(SceneStyle.team(team));c.drawCircle(x,screen.legendY()-4,3,paint);
            text(c,team==1?"나":String.valueOf(team-1),x+9,screen.legendY(),9,WHITE);
        }
    }
    private boolean canUpgrade() {
        if(controls.selected<0) return false;
        Engine.Tower t=engine.towers.get(controls.selected);
        return t.owner==1 && t.rank<3 && t.units>=20*t.rank;
    }
    private String upgradeLabel() {
        if(controls.selected<0) return "타워 선택";
        Engine.Tower t=engine.towers.get(controls.selected);
        return t.rank>=3?"최대 레벨":"강화 · 병력 "+20*t.rank;
    }
    private void overlay(Canvas c) {
        paint.setColor(Color.argb(245,245,239,223));c.drawRect(0,0,400,screen.height,paint);
        int result=engine.result();boolean win=result==1;float y=screen.overlayTitle;
        text(c,resultTitle(result),200,y,32,WHITE);
        if(!screen.panel.hideSubtitle) text(c,(engine.skirmish?Skirmish.name(engine.mapId):Campaign.name(engine.level))+" · "+Campaign.difficultyName(engine.difficulty),200,screen.panel.subtitle,15,GRAY);
        if(win && !engine.skirmish) text(c,"★★★".substring(0,engine.medals())+"☆☆☆".substring(engine.medals()),200,screen.panel.stars,34,0xffb17b15);
        else text(c,overlayMessage(win),200,screen.panel.stars,13,GRAY);
        text(c,"시간 "+clock(engine.elapsed)+"   점령 "+engine.captures+"   출병 "+engine.dispatched,200,screen.panel.stats,13,WHITE);
        text(c,engine.rules.objectiveText(),200,screen.panel.goal,10,GRAY);
        overlayButton(c,screen.overlayPrimaryTop,primaryLabel(result),true);
        overlayButton(c,screen.overlayRetryTop,"현재 작전 다시 시작",false);
        overlayButton(c,screen.overlayMenuTop,"시작 화면 · 자동 저장",false);
        if(screen.panel.footerVisible) text(c,engine.skirmish?"자유전은 미션 기록에 영향을 주지 않습니다":"별은 난이도별 최고 기록으로 저장됩니다",200,screen.panel.footer,12,GRAY);
    }
    private void overlayButton(Canvas c,float y,String title,boolean primary) {
        box(c,56,y,288,screen.buttonHeight,primary?MainActivity.CYAN:MainActivity.CARD);
        text(c,title,200,y+screen.buttonHeight/2+screen.font(16)/3,16,primary?Color.WHITE:WHITE);
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
        if(inside(x,y,294,screen.pauseTop(),88,screen.buttonHeight)) return 0;
        if(inside(x,y,18,screen.commandTop,174,screen.buttonHeight)) return 1;
        if(inside(x,y,208,screen.commandTop,174,screen.buttonHeight)) return 2;
        for(int skill=0;skill<3;skill++) if(inside(x,y,18+skill*124,screen.skillTop,116,screen.buttonHeight)) return 6+skill;
        if(inside(x,y,screen.zoomLeft(),screen.zoomTop(),screen.buttonHeight,screen.buttonHeight)) return 9;
        return -1;
    }
    private int overlayButtonAt(float x,float y) {
        if(inside(x,y,56,screen.overlayPrimaryTop,288,screen.buttonHeight)) return 3;
        if(inside(x,y,56,screen.overlayRetryTop,288,screen.buttonHeight)) return 4;
        if(inside(x,y,56,screen.overlayMenuTop,288,screen.buttonHeight)) return 5;
        return -1;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getPointerCount()>1) {cancelTouch();return true;}
        float x=(event.getX()-left)/scale,y=(event.getY()-top)/scale;
        int action=event.getActionMasked();boolean overlay=paused || engine.result()!=0;
        if(action==MotionEvent.ACTION_CANCEL) {cancelTouch();return true;}
        if(action==MotionEvent.ACTION_DOWN) touchDown(x,y,overlay);
        if(action==MotionEvent.ACTION_MOVE) touchMove(x,y);
        if(action==MotionEvent.ACTION_UP) touchUp(x,y,overlay);
        return true;
    }
    private void cancelTouch() {
        controls.cancel();dragging=false;pressedButton=-1;downTarget=-1;previewTarget=-1;panCandidate=false;mapPanning=false;
    }
    private int hitWorld(float x,float y) {
        if(!inside(x,y,screen.boardLeft,screen.boardTop,screen.boardRight-screen.boardLeft,screen.boardBottom-screen.boardTop)) return -1;
        if(inside(x,y,screen.zoomLeft(),screen.zoomTop(),screen.buttonHeight,screen.buttonHeight)) return -1;
        float radius=24*screen.pixelDensity/(screen.scale*camera.zoom);
        return BattleControls.hitTouch(engine,camera.worldX(x),camera.worldY(y),radius);
    }
    private void touchDown(float x,float y,boolean overlay) {
        pressedButton=buttonAt(x,y,overlay);panCandidate=false;mapPanning=false;dragging=false;
        if(overlay || pressedButton>=0) return;
        if(!inside(x,y,screen.boardLeft,screen.boardTop,screen.boardRight-screen.boardLeft,screen.boardBottom-screen.boardTop)) return;
        int target=hitWorld(x,y);downTarget=target;panCandidate=target<0;downX=x;downY=y;lastX=x;lastY=y;
        if(panCandidate) return;
        int previous=controls.selected;
        controls.press(target);dragX=camera.worldX(x);dragY=camera.worldY(y);dragging=true;previewTarget=target;
        if(previous!=controls.selected && host.haptics) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }
    private void touchMove(float x,float y) {
        if(panCandidate) {
            if(Math.hypot(x-downX,y-downY)>8*screen.pixelDensity/screen.scale) mapPanning=true;
            if(mapPanning) camera.pan(x-lastX,y-lastY);
            lastX=x;lastY=y;return;
        }
        dragX=camera.worldX(x);dragY=camera.worldY(y);
        previewTarget=gestureTarget(x,y);
    }
    private int gestureTarget(float x,float y) {
        return BattleControls.releaseTarget(downTarget,hitWorld(x,y),Math.hypot(x-downX,y-downY),8*screen.pixelDensity/screen.scale);
    }
    private void touchUp(float x,float y,boolean overlay) {
        dragging=false;performClick();
        if(pressedButton>=0) {
            int action=pressedButton;pressedButton=-1;
            if(buttonAt(x,y,overlay)==action) activate(action);
            return;
        }
        if(overlay) return;
        if(panCandidate) {
            if(!mapPanning) controls.cancel();panCandidate=false;mapPanning=false;return;
        }
        int target=gestureTarget(x,y);
        int result=controls.release(target);
        if(result==1) {notice("병력 출발");signal(true);}
        if(result==-1) {notice("출병할 병력이 부족합니다");signal(false);}
    }
    @Override public boolean performClick() {super.performClick();return true;}
    private void activate(int action) {
        if(action>=6 && action<=8) {useSkill(action-6);return;}
        switch(action) {
            case 0: pause();break;
            case 1: toggleDispatch();break;
            case 2: upgrade();break;
            case 3: primaryAction();break;
            case 4: host.retry();break;
            case 5: host.showMenu();break;
            case 9: zoomMap();break;
            default: break;
        }
    }
    private void zoomMap() {
        int focus=controls.selected;
        if(focus<0) for(int i=0;i<engine.towers.size();i++) if(engine.towers.get(i).owner==1) {focus=i;break;}
        float x=focus<0?200:BattleControls.x(engine.towers.get(focus));
        float y=focus<0?412:BattleControls.y(engine.towers.get(focus));camera.toggle(x,y);
        notice(camera.overview()?"전체 전장 보기":"확대됨 · 빈 땅을 드래그해 이동");
    }
    private void toggleDispatch() {
        controls.all=!controls.all;notice(controls.all?"모든 병력 출병 · 빈 타워에 주의하세요":"병력 절반 출병");
    }
    private void useSkill(int type) {
        boolean used=engine.useSkill(type,controls.selected);
        notice(used?BattleRules.skillName(type)+" 사용!":skillFailure(type));signal(used);
        if(used) host.saveBattle(false);
    }
    private String skillFailure(int type) {
        if(controls.selected<0) return "아군 건물을 먼저 선택하세요";
        if(engine.rules.cooldowns[type]>0) return "재사용까지 "+(int)Math.ceil(engine.rules.cooldowns[type])+"초";
        if(engine.rules.energy<BattleRules.cost(type)) return "전술 에너지가 부족합니다";
        return "병력이 가득 찼거나 효과가 적용 중입니다";
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
