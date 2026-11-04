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
    static final int BG=Color.rgb(9,18,31),CARD=Color.rgb(24,42,59);
    static final int CYAN=Color.rgb(63,207,238),MUTED=Color.rgb(153,178,196);
    private FrameLayout root;
    private SharedPreferences preferences;
    private ToneGenerator tone;
    private BattleView battle;
    private boolean campaignPage;
    Engine engine;
    Profile profile;
    int mode=1;
    boolean sound=true,haptics=true,visuals=true;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        preferences=getPreferences(0);
        profile=Profile.decode(preferences.getString("profile",""));
        mode=preferences.getInt("mode",1);mode=Math.max(0,Math.min(2,mode));
        sound=preferences.getBoolean("sound",true);haptics=preferences.getBoolean("haptics",true);
        visuals=preferences.getBoolean("visuals",true);
        migrateProgress();
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
        if(engine!=null && engine.result()==1) profile.record(engine.level,engine.difficulty,engine.medals());
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
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(16));
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
        TextView heading=label(title,30,Color.WHITE);heading.setTypeface(null,Typeface.BOLD);
        layout.addView(heading);layout.addView(label(subtitle,13,MUTED));
        return layout;
    }
    private Button button(LinearLayout parent,String title,boolean primary,Runnable action) {
        Button button=new Button(this);button.setText(title);button.setAllCaps(false);button.setTextSize(16);
        button.setTextColor(primary?BG:Color.WHITE);button.setBackground(surface(primary?CYAN:CARD));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(58));
        params.topMargin=dp(12);parent.addView(button,params);
        button.setOnClickListener(view->action.run());return button;
    }
    void showMenu() {
        campaignPage=false;
        saveBattle(false);
        LinearLayout panel=page("TOWER FRONT","타워 프론트 · 오프라인 전략 캠페인");
        panel.addView(label("점령하고, 성장하고, 전선을 넓히세요",18,Color.WHITE));
        panel.addView(label("12개 작전  /  3가지 난이도  /  전투 자동 저장",12,MUTED));
        if(engine!=null) button(panel,engine.result()==0?"작전 "+engine.level+" 이어하기":"지난 전투 결과",true,()->openBattle(engine));
        button(panel,"캠페인 · 작전 선택",engine==null,()->showCampaign());
        button(panel,"플레이 방법",false,()->showHelp());
        button(panel,"소리·진동·연출 설정",false,()->showSettings());
        panel.addView(label("별 기록  ·  "+Campaign.difficultyName(mode)+"  "+profile.total(mode)+" / 36",16,CYAN));
        panel.addView(label("v3.1 · 광고와 네트워크 연결 없이 플레이",12,MUTED));
    }
    void showCampaign() {
        campaignPage=true;
        saveBattle(false);
        LinearLayout panel=page("작전 지도", "90초 이내 ★★★  ·  180초 이내 ★★  ·  승리 ★");
        addDifficulties(panel);
        for(int stage=1;stage<=Campaign.COUNT;stage++) addOperation(panel,stage);
        button(panel,"시작 화면",false,()->showMenu());
    }
    private void addDifficulties(LinearLayout panel) {
        LinearLayout row=new LinearLayout(this);panel.addView(row);
        for(int i=0;i<3;i++) {
            final int choice=i;Button b=new Button(this);
            b.setText(Campaign.difficultyName(i));b.setTextColor(i==mode?BG:Color.WHITE);
            b.setBackground(surface(i==mode?CYAN:CARD));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(52),1);
            params.setMargins(dp(3),dp(8),dp(3),dp(8));row.addView(b,params);
            b.setOnClickListener(view->{mode=choice;preferences.edit().putInt("mode",mode).apply();showCampaign();});
        }
    }
    private void addOperation(LinearLayout panel,int stage) {
        boolean open=profile.available(stage,mode);int score=profile.stars(stage,mode);
        String stars="★★★".substring(0,score)+"☆☆☆".substring(score);
        String title=String.format("%02d  %s   %s",stage,Campaign.name(stage),open?stars:"잠김");
        Button b=button(panel,title,false,()->chooseStage(stage));b.setEnabled(open);b.setAlpha(open?1:.38f);
    }
    private void chooseStage(int stage) {
        if(engine!=null && engine.result()==0) {
            new AlertDialog.Builder(this).setTitle("새 작전을 시작할까요?")
                .setMessage("저장된 전투를 새 작전으로 바꿉니다. 별 기록은 유지됩니다.")
                .setNegativeButton("취소",null).setPositiveButton("시작",(dialog,which)->start(stage)).show();
        } else start(stage);
    }
    void start(int stage) {
        engine=new Engine(stage,mode);saveBattle(true);openBattle(engine);
        if(stage==1) message("아군을 선택한 뒤 목표를 터치하거나 드래그하세요");
    }
    void openBattle(Engine state) {
        engine=state;mode=state.difficulty;
        if(battle!=null) battle.suspend();
        root.removeAllViews();battle=new BattleView(this,state);root.addView(battle);
    }
    void retry() {
        new AlertDialog.Builder(this).setTitle("현재 작전 다시 시작")
            .setMessage("이번 전투를 처음부터 시작합니다.").setNegativeButton("취소",null)
            .setPositiveButton("다시 시작",(dialog,which)->start(engine.level)).show();
    }
    void next() {
        if(engine.level>=Campaign.COUNT) {showCampaign();return;}
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
                +"④ 선택한 아군 타워는 병력 20 / 40을 사용해 강화할 수 있습니다. 생산량은 레벨마다 50% 증가합니다.\n\n"
                +"⑤ 적 타워와 이동 병력을 모두 제거하면 승리합니다. 90초 / 180초 이내 승리로 별을 모으세요.\n\n"
                +"⑥ 전투는 자동 저장됩니다. 홈으로 돌아가거나 앱을 닫아도 이어갈 수 있습니다.")
            .setPositiveButton("알겠습니다",null).show();
    }
}
