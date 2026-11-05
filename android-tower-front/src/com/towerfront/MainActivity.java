package com.towerfront;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;

public final class MainActivity extends Activity {
    static final int BG=SceneStyle.PAPER,CARD=0xfffffcf3;
    static final int CYAN=SceneStyle.BLUE,MUTED=0xff657469;
    private FrameLayout root;
    private SharedPreferences preferences;
    private ToneGenerator tone;
    private BattleView battle;
    private boolean campaignPage;
    private int missionChapter,customMap,customEnemies=3,customDifficulty=1;
    Engine engine;
    Profile profile;
    int mode=1;
    boolean sound=true,haptics=true,visuals=true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        preferences=getPreferences(0);
        profile=Profile.decode(preferences.getString("profile",""));
        mode=preferences.getInt("mode",1);mode=Math.max(0,Math.min(2,mode));
        sound=preferences.getBoolean("sound",true);haptics=preferences.getBoolean("haptics",true);
        visuals=preferences.getBoolean("visuals",true);
        migrateProgress();
        customMap=Math.max(0,Math.min(5,preferences.getInt("customMap",0)));
        customEnemies=Math.max(1,Math.min(10,preferences.getInt("customEnemies",3)));
        customDifficulty=Math.max(0,Math.min(2,preferences.getInt("customDifficulty",1)));
        engine=SaveCodec.decode(preferences.getString("battle",""));
        try {tone=new ToneGenerator(AudioManager.STREAM_MUSIC,45);} catch(RuntimeException ex) {tone=null;}
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        root=new FrameLayout(this);root.setBackgroundColor(BG);setContentView(root);
        root.setOnApplyWindowInsetsListener((view,insets)->applyInsets(view,insets));
        root.requestApplyInsets();showMenu();
    }
    private WindowInsets applyInsets(View view,WindowInsets insets) {
        if(Build.VERSION.SDK_INT>=30) {
            android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            view.setPadding(safe.left,safe.top,safe.right,safe.bottom);
        } else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
            insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
        return insets;
    }
    private void migrateProgress() {
        if(preferences.contains("profile")) return;
        int old=Math.min(Campaign.COUNT,preferences.getInt("stage",1));
        for(int i=1;i<old;i++) profile.record(i,1,1);
        preferences.edit().putString("profile",profile.encode()).apply();
    }
    @Override public void onPause() {
        if(battle!=null) battle.suspend();
        saveBattle(true);super.onPause();
    }
    @Override public void onResume() {super.onResume();if(battle!=null) battle.wake();}
    @Override public void onDestroy() {
        if(tone!=null) tone.release();super.onDestroy();
    }
    @Override public void onBackPressed() {
        if(battle!=null) {battle.back();return;}
        if(campaignPage) {showMenu();return;}
        super.onBackPressed();
    }
    void saveBattle(boolean immediate) {
        if(engine!=null) profile.recordBattle(engine);
        SharedPreferences.Editor edit=preferences.edit().putString("profile",profile.encode());
        if(engine!=null) edit.putString("battle",SaveCodec.encode(engine));
        if(immediate) edit.commit();else edit.apply();
    }
    void feedback(boolean positive) {
        if(!sound || tone==null) return;
        tone.startTone(positive?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_NACK,90);
    }
    void battleSound(int kind) {
        if(!sound || tone==null) return;
        int[] tones={ToneGenerator.TONE_PROP_BEEP,ToneGenerator.TONE_PROP_BEEP2,ToneGenerator.TONE_PROP_ACK};
        tone.startTone(tones[kind],kind==Engine.CAPTURE?140:65);
    }
    void message(String text) {Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}
    private int dp(int value) {return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable surface(int color) {
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(16));shape.setStroke(dp(1),0x183c563c);
        return shape;
    }
    private TextView label(String text,int size,int color) {
        TextView view=new TextView(this);view.setText(text);view.setTextSize(size);view.setTextColor(color);
        view.setPadding(0,dp(8),0,dp(8));view.setGravity(Gravity.CENTER);
        return view;
    }
    private LinearLayout page(String title,String subtitle) {
        if(battle!=null) battle.suspend();battle=null;
        root.removeAllViews();
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24),dp(20),dp(24),dp(28));
        scroll.addView(layout);root.addView(scroll);
        TextView heading=label(title,30,SceneStyle.INK);heading.setTypeface(null,Typeface.BOLD);
        layout.addView(heading);layout.addView(label(subtitle,13,MUTED));
        return layout;
    }
    private Button button(LinearLayout parent,String title,boolean primary,Runnable action) {
        Button button=new Button(this);button.setText(title);button.setAllCaps(false);button.setTextSize(16);
        button.setTextColor(primary?0xffffffff:SceneStyle.INK);button.setBackground(surface(primary?CYAN:CARD));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        button.setMinHeight(dp(58));button.setPadding(dp(12),dp(14),dp(12),dp(14));
        params.topMargin=dp(12);parent.addView(button,params);
        button.setElevation(dp(2));button.setOnClickListener(view->action.run());return button;
    }
    void showMenu() {
        campaignPage=false;
        saveBattle(false);
        LinearLayout panel=page("TOWER FRONT","타워 프론트 · 오프라인 전략 캠페인");
        panel.addView(new MenuHero(this),new LinearLayout.LayoutParams(-1,dp(Math.min(185,Math.max(110,getResources().getConfiguration().screenHeightDp/5)))));
        panel.addView(label("섬을 지휘하고, 전선을 넓히세요",18,SceneStyle.INK));
        panel.addView(label("미션 100단계  /  자유전 AI 최대 10진영",12,MUTED));
        if(engine!=null) button(panel,engine.result()==0?(engine.skirmish?"자유전 이어하기":"미션 "+engine.level+" 이어하기"):"지난 전투 결과",true,()->openBattle(engine));
        button(panel,"미션 모드 · 100스테이지",engine==null,()->openMissions());
        button(panel,"자유전 · 무작위 게임",false,()->showSkirmish());
        button(panel,"플레이 방법",false,()->showHelp());
        button(panel,"소리·진동·연출 설정",false,()->showSettings());
        panel.addView(label("별 기록  ·  "+Campaign.difficultyName(mode)+"  "+profile.total(mode)+" / 300",16,CYAN));
        panel.addView(label("v6.2 · 광고와 네트워크 연결 없이 플레이",12,MUTED));
    }
    private void openMissions() {
        missionChapter=(profile.unlocked(mode)-1)/10;showCampaign();
    }
    void showCampaign() {
        campaignPage=true;saveBattle(false);
        LinearLayout panel=page("미션 지도", "100스테이지 · 목표별 전술 미션 · 보급 생산 +35%");
        addDifficulties(panel);addChapters(panel);
        int first=missionChapter*10+1;
        for(int stage=first;stage<first+10;stage++) addOperation(panel,stage);
        button(panel,"시작 화면",false,()->showMenu());
    }
    private void addChapters(LinearLayout panel) {
        panel.addView(label((missionChapter+1)+"구역 · 미션 "+(missionChapter*10+1)+"–"+(missionChapter*10+10),18,SceneStyle.INK));
        LinearLayout row=new LinearLayout(this);panel.addView(row);
        chapterButton(row,"← 이전 구역",-1);chapterButton(row,"다음 구역 →",1);
    }
    private void chapterButton(LinearLayout row,String title,int direction) {
        Button b=new Button(this);b.setText(title);b.setTextColor(SceneStyle.INK);b.setBackground(surface(CARD));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,android.view.ViewGroup.LayoutParams.WRAP_CONTENT,1);params.setMargins(dp(3),dp(2),dp(3),dp(6));row.addView(b,params);
        b.setMinHeight(dp(48));b.setTextSize(14);b.setPadding(dp(8),dp(12),dp(8),dp(12));
        b.setEnabled(missionChapter+direction>=0 && missionChapter+direction<10);
        b.setOnClickListener(view->{missionChapter+=direction;showCampaign();});
    }
    void showSkirmish() {
        campaignPage=true;saveBattle(false);
        LinearLayout panel=page("자유전", "최대 10개 AI 진영과 생존 경쟁 · 서로 공격합니다");
        String[] maps=new String[Skirmish.MAPS+1];maps[0]="무작위 맵";
        for(int i=0;i<Skirmish.MAPS;i++) maps[i+1]=Skirmish.name(i);
        String[] enemies=new String[10];for(int i=0;i<10;i++) enemies[i]="AI "+(i+1)+"개 진영";
        Spinner map=choice(panel,"전장 선택 ▼",maps,customMap);
        Spinner count=choice(panel,"적 진영 수 ▼",enemies,customEnemies-1);
        Spinner difficulty=choice(panel,"난이도 ▼",new String[]{"쉬움","보통","어려움"},customDifficulty);
        panel.addView(label("중앙 보급: 생산 +35% · 세 가지 전술 스킬을 활용하세요",12,MUTED));
        button(panel,"전투 시작",true,()->{
            customMap=map.getSelectedItemPosition();customEnemies=count.getSelectedItemPosition()+1;
            customDifficulty=difficulty.getSelectedItemPosition();confirmFree();
        });
        button(panel,"시작 화면",false,()->showMenu());
    }
    private Spinner choice(LinearLayout panel,String title,String[] items,int selected) {
        panel.addView(label(title,15,SceneStyle.INK));
        Spinner spinner=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,items) {
            @Override public View getView(int position,View old,android.view.ViewGroup parent) {
                TextView item=(TextView)super.getView(position,old,parent);
                item.setTextColor(SceneStyle.INK);item.setTextSize(17);item.setSingleLine(false);item.setMaxLines(2);
                item.setMinHeight(dp(56));item.setPadding(dp(8),dp(10),dp(8),dp(10));return item;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);spinner.setSelection(selected);spinner.setBackground(surface(CARD));
        spinner.setPadding(dp(16),0,dp(16),0);spinner.setMinimumHeight(dp(56));panel.addView(spinner,new LinearLayout.LayoutParams(-1,android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        return spinner;
    }
    private void confirmFree() {
        if(engine==null || engine.result()!=0) {launchFree();return;}
        new AlertDialog.Builder(this).setTitle("새 자유전을 시작할까요?")
            .setMessage("저장된 전투가 새 자유전으로 바뀝니다. 미션 별 기록은 유지됩니다.")
            .setNegativeButton("취소",null).setPositiveButton("시작",(dialog,which)->launchFree()).show();
    }
    private void launchFree() {
        preferences.edit().putInt("customMap",customMap).putInt("customEnemies",customEnemies)
            .putInt("customDifficulty",customDifficulty).apply();
        java.util.Random random=new java.util.Random();
        int map=customMap==0?random.nextInt(Skirmish.MAPS):customMap-1;
        engine=Skirmish.create(map,customEnemies,customDifficulty,random.nextLong());
        saveBattle(true);openBattle(engine);
    }
    private void addDifficulties(LinearLayout panel) {
        LinearLayout row=new LinearLayout(this);panel.addView(row);
        for(int i=0;i<3;i++) {
            final int choice=i;Button b=new Button(this);
            b.setText(Campaign.difficultyName(i));b.setTextColor(i==mode?0xffffffff:SceneStyle.INK);
            b.setBackground(surface(i==mode?CYAN:CARD));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,android.view.ViewGroup.LayoutParams.WRAP_CONTENT,1);
            b.setMinHeight(dp(52));b.setTextSize(14);b.setPadding(dp(4),dp(10),dp(4),dp(10));
            params.setMargins(dp(3),dp(8),dp(3),dp(8));row.addView(b,params);
            b.setOnClickListener(view->{mode=choice;missionChapter=(profile.unlocked(mode)-1)/10;preferences.edit().putInt("mode",mode).apply();showCampaign();});
        }
    }
    private void addOperation(LinearLayout panel,int stage) {
        boolean open=profile.available(stage,mode);int score=profile.stars(stage,mode);
        String stars="★★★".substring(0,score)+"☆☆☆".substring(score);
        String title=String.format("%02d  %s\n%s · %s",stage,Campaign.name(stage),BattleRules.missionLabel(stage),open?stars:"잠김");
        Button b=button(panel,title,false,()->chooseStage(stage));b.setTextSize(14);
        b.setMinHeight(dp(72));
        b.setEnabled(open);b.setAlpha(open?1:.38f);
    }
    private void chooseStage(int stage) {
        String message=BattleRules.briefing(stage)+"\n\n보급 거점은 생산 +35%이며 스킬 에너지도 빨리 회복합니다.";
        if(engine!=null && engine.result()==0) message+="\n\n저장된 전투는 새 미션으로 바뀝니다. 별 기록은 유지됩니다.";
        new AlertDialog.Builder(this).setTitle("미션 "+stage+" · "+BattleRules.missionLabel(stage))
            .setMessage(message).setNegativeButton("취소",null)
            .setPositiveButton("시작",(dialog,which)->start(stage)).show();
    }
    void start(int stage) {
        engine=new Engine(stage,mode);saveBattle(true);openBattle(engine);
        if(stage==1) message("아군을 선택한 뒤 목표를 터치하거나 드래그하세요");
    }
    void openBattle(Engine state) {
        engine=state;if(!state.skirmish) mode=state.difficulty;
        if(battle!=null) battle.suspend();
        root.removeAllViews();battle=new BattleView(this,state);root.addView(battle);
    }
    void retry() {
        new AlertDialog.Builder(this).setTitle("현재 전투 다시 시작")
            .setMessage("이번 전투를 처음부터 시작합니다.").setNegativeButton("취소",null)
            .setPositiveButton("다시 시작",(dialog,which)->restartBattle()).show();
    }
    void restartBattle() {
        engine=engine.skirmish?Skirmish.create(engine.mapId,engine.enemyCount,engine.difficulty,engine.seed):new Engine(engine.level,engine.difficulty);
        saveBattle(true);openBattle(engine);
    }
    void next() {
        if(engine.skirmish) {showSkirmish();return;}
        if(engine.level>=Campaign.COUNT) {openMissions();return;}
        start(Campaign.next(engine.level));
    }
    private void showSettings() {
        String[] names={"효과음","터치 진동","전투 연출"};boolean[] checked={sound,haptics,visuals};
        new AlertDialog.Builder(this).setTitle("소리·진동·연출")
            .setMultiChoiceItems(names,checked,(dialog,which,value)->{
                if(which==0) sound=value;
                if(which==1) haptics=value;
                if(which==2) visuals=value;
                preferences.edit().putBoolean("sound",sound).putBoolean("haptics",haptics)
                    .putBoolean("visuals",visuals).apply();
            }).setPositiveButton("완료",null).show();
    }
    void showHelp() {
        new AlertDialog.Builder(this).setTitle("지휘관 안내서")
            .setMessage("① 아군 타워를 터치한 뒤 목표를 터치하거나 드래그해 출병하세요.\n\n"
                +"② 하단에서 50% / 100% 출병을 전환합니다. 아군 타워로 보내면 증원합니다.\n\n"
                +"③ 생산 타워는 생산량 1.7배, 기동 타워는 이동 시간이 40% 짧습니다. 중립은 생산하지 않습니다.\n\n"
                +"④ 방어 타워는 받는 공격 피해를 25% 줄입니다(소수점 올림). 아군 증원은 그대로 받습니다. 4번째 작전부터 등장합니다.\n\n"
                +"⑤ 선택한 아군 타워는 병력 20 / 40을 사용해 강화할 수 있습니다. 생산량은 레벨마다 50% 증가합니다.\n\n"
                +"⑥ 적 타워와 이동 병력을 모두 제거하면 승리합니다. 90초 / 180초 이내 승리로 별을 모으세요.\n\n"
                +"⑦ 미션은 100단계입니다. 자유전에서는 맵과 AI 1~10진영을 고르며, AI끼리도 전투합니다. 자유전은 미션 기록에 영향을 주지 않습니다.\n\n"
                +"⑧ 아군 선택 후 하단 스킬을 사용하세요. 증원은 35에너지로20명, 보호막은30에너지로6초 피해절반, 가속은25에너지로6초 이동1.75배입니다.\n\n"
                +"⑨ 보급 거점을 가진 진영은 전체 생산+35%입니다. 플레이어는 에너지 회복도3→5/초가 됩니다. 미션은 전멸·30초사수·시간생존·3거점12초유지로 나뉩니다. 생존 별점은 종료 시 3거점 이상3개, 2거점2개, 1거점1개입니다.\n\n"
                +"⑩ 작은 화면에서는 전장 오른쪽 +로 확대하고 빈 땅을 드래그해 이동하세요. 전체 버튼으로 전체 지도로 돌아옵니다. 빈 땅을 가볍게 누르면 선택이 해제됩니다.\n\n"
                +"⑪ 전투는 자동 저장됩니다. 홈으로 돌아가거나 앱을 닫아도 이어갈 수 있습니다.")
            .setPositiveButton("알겠습니다",null).show();
    }
}
