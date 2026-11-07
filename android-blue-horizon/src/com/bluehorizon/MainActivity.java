package com.bluehorizon;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    static final int NAVY=0xff101f30, PANEL=0xff1a3041, GOLD=0xffe4bf7b;
    static final int INK=0xffecede5, MUTED=0xff9eb7bd, TEAL=0xff65c9b5;
    private Engine game;
    private AudioController audio;
    private AlertDialog eventDialog,dialogueDialog,storyDialog;
    private String chapterReply;
    private Toast notice;
    private int tab=7, quantity=5, mapRegion=-1, selectedGood=0;
    private SailingView sailing;
    private TownView townView;
    private BattleFrame battleFrame;
    private SharedPreferences prefs;
    private LinearLayout root, content;
    private TextView status;
    private boolean saved=true;
    private int shownChapter=0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY); getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        prefs=getSharedPreferences("blue_horizon_v1", MODE_PRIVATE);
        game=Engine.decode(prefs.getString("save", null));
        boolean recovered=false;
        if(game==null) { game=Engine.decode(prefs.getString("backup",null)); recovered=game!=null; }
        boolean fresh=game==null;
        if(fresh) game=new Engine();
        audio=new AudioController(this,prefs);
        if(state!=null) { tab=state.getInt("tab",7); quantity=state.getInt("quantity",5);mapRegion=state.getInt("region",-1); }
        save(); render();
        if(game.town.moving()) { showSailing();return; }
        if(fresh) dialog("푸른 수평선", Story.introduction());
        if(recovered) dialog("저장 복원", "최근 저장을 읽을 수 없어 이전 자동 저장을 복원했습니다.");
        if(game.message.startsWith("세계 확장 버전")) dialog("세계 항해 버전으로 이전",game.message);
        if(game.expedition.active()) root.post(()->showBoss());
        else if(game.adventure.pending>=0) root.post(()->showEvent());
    }
    @Override protected void onResume() { super.onResume();if(audio!=null) audio.resume();if(sailing!=null) sailing.resume(); }
    @Override protected void onPause() { if(townView!=null) townView.cancelWalk();if(sailing!=null) sailing.pause();if(audio!=null) audio.pause();super.onPause();if(game!=null) save(); }
    @Override protected void onDestroy() {
        if(sailing!=null) sailing.stop();if(storyDialog!=null) storyDialog.dismiss();if(dialogueDialog!=null) dialogueDialog.dismiss();if(eventDialog!=null) eventDialog.dismiss();if(audio!=null) audio.release();super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putInt("tab",tab); out.putInt("quantity",quantity);
        out.putInt("region",mapRegion);
    }
    private void save() {
        String previous=prefs.getString("save",null);
        SharedPreferences.Editor edit=prefs.edit();
        if(Engine.decode(previous)!=null) edit.putString("backup",previous);
        saved=edit.putString("save",game.encode()).commit();
        if(!saved) Toast.makeText(this,"저장 실패: 저장 공간을 확인하고 백업해 주세요.",Toast.LENGTH_LONG).show();
    }
    private void changed(boolean success) {
        changed(success,"trade");
    }
    private void changed(boolean success,String effect) {
        int previousChapter=shownChapter;
        save(); render();
        String sound=success?effect:"error";
        if(game.chapter>previousChapter) sound="chapter";
        audio.play(sound);
        announce(game.message);
        if(success && game.chapter>previousChapter) storyReveal(previousChapter);
        else if(game.expedition.active()) showBoss();
        else if(game.adventure.pending>=0) showEvent();
    }
    private void announce(String message) {
        if(notice!=null) notice.cancel();notice=Toast.makeText(this,message,Toast.LENGTH_LONG);notice.show();
    }
    private void render() {
        shownChapter=game.chapter;
        root=column(); root.setBackgroundColor(NAVY); root.setPadding(dp(12),dp(4),dp(12),dp(4));
        applyInsets(root);
        setContentView(root); header();
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column(); content.setPadding(0,dp(6),0,dp(12)); scroll.addView(content);
        renderContent();
        navigation();
        audio.scene(tab==0?"sea":"harbor");
    }
    private void renderContent() {
        switch(tab) {
            case 0: harbor();break;
            case 1: market();break;
            case 2: shipyard();break;
            case 3: crewPage();break;
            case 4: journal();break;
            case 5: adventurePage();break;
            case 6: rivalPage();break;
            case 8: tavern();break;
            case 9: expeditionPage();break;
            default: town();break;
        }
    }
    private void scene(String asset,int cell) {
        content.addView(new SceneView(this,asset,cell),new LinearLayout.LayoutParams(-1,dp(265)));
    }
    private void town() {
        label(content,World.PORTS[game.port]+" · 항구 마을",26,GOLD);
        townView=new TownView(this,TownRpg.style(game.port),location->enterBuilding(location));
        content.addView(townView,new LinearLayout.LayoutParams(-1,dp(275)));
        label(content,TownRpg.FLAVORS[TownRpg.style(game.port)],12,MUTED);
        for(int line=0;line<2;line++) {
            LinearLayout buildings=row();content.addView(buildings);
            for(int col=0;col<2;col++) {
                final int location=line*2+col;
                Button entry=button(TownLayout.NAMES[location],false);icon(entry,new int[]{GameIcon.CARGO,GameIcon.TOOLS,GameIcon.CUP,GameIcon.COMPASS}[location],30,false);
                entry.setOnClickListener(v->{audio.play("click");townView.walkTo(location);});
                LinearLayout.LayoutParams params=buttonParams();params.width=0;params.weight=1;buildings.addView(entry,params);
            }
        }
        planCard();localLifeCard();
        LinearLayout progress=card(content);label(progress,"선장 등급 "+TownRpg.rank(game)+" · 사기 "+game.town.morale+" / 100",16,TEAL);
        growthBar(progress);
        label(progress,"방문 "+game.visitedCount()+" / 56 · 이야기 "+game.chapter+" / 8 · 의뢰 "+game.adventure.deliveries+"회",13,INK);
        fullAction(progress,"보물지도 · 동료의 인연 · 해역 강적",()->{tab=9;render();},true);
        fullAction(progress,"항구 의뢰 · 세 가지 제안 살펴보기",()->{tab=5;render();},true);
        fullAction(progress,"출항 지도",()->{tab=0;render();},false);
    }
    private void growthBar(LinearLayout parent) {
        int rank=TownRpg.rank(game),xp=TownRpg.experience(game);
        android.widget.ProgressBar bar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(80);bar.setProgress(rank==20?80:xp%80);parent.addView(bar,new LinearLayout.LayoutParams(-1,dp(10)));
        label(parent,rank==20?"최고 등급 · 숙련 포격 +3":"다음 등급까지 "+(80-xp%80)+" 경험 · 숙련 포격 +"+TownRpg.rankBonus(game),12,GOLD);
        label(parent,"항구 방문·의뢰·사건 해결로 성장 · 등급 5/10/15에서 포격 +1씩",12,MUTED);
    }
    private void continueEncounter() {
        if(game.town.moving()) showSailing();
        else if(game.expedition.active()) showBoss();else showEvent();
    }
    private void planCard() {
        CaptainPlan plan=CaptainPlan.next(game);LinearLayout box=card(content);
        label(box,"선장의 다음 한 걸음",11,TEAL);label(box,plan.body,12,MUTED);
        fullAction(box,plan.title,()->actPlan(plan),true);
    }
    private void actPlan(CaptainPlan plan) {
        if(plan.continueStory) { game.advanceStory();changed(true,"chapter");return; }
        if(plan.event) { continueEncounter();return; }
        if(plan.achievement>=0) { changed(Achievements.claim(game,plan.achievement),"discovery");return; }
        if(plan.deliver) { changed(Contracts.deliver(game),"discovery");return; }
        if(plan.good>=0) {
            selectedGood=plan.good;quantity=plan.amount;tab=1;render();openTrade(plan.good,plan.selling);return;
        }
        if(plan.target>=0) { voyage(plan.target);return; }
        tab=plan.page;render();
    }
    private void enterBuilding(int location) {
        audio.play("click");
        if(location==3) { townSquare();return; }
        int[] pages={1,2,8};tab=pages[location];render();
    }
    private void townSquare() {
        new AlertDialog.Builder(this).setTitle(World.PORTS[game.port]+" · 항구 관리인")
            .setMessage("먼 바다에서 왔군요, 선장님. 의뢰를 수행하고 상단과 인연을 쌓아 보세요.\n\n현재 목표: "+Story.GOALS[game.chapter])
            .setPositiveButton(getString(R.string.ui_contract_board),(d,w)->{tab=5;render();})
            .setNeutralButton(getString(R.string.ui_meet_guild),(d,w)->{tab=6;render();}).setNegativeButton(getString(R.string.ui_go_back),null).show();
    }
    private void localLifeCard() {
        LinearLayout box=card(content);label(box,"항구의 일상 · "+Long.bitCount(game.life.events)+" / 56",16,GOLD);
        label(box,PortLife.title(game),13,INK);
        if(PortLife.done(game)) label(box,(game.life.generous & (1L<<game.port))!=0?"함께 손을 보탠 하루가 기억에 남아 있습니다.":"약속한 일을 마친 선장을 사람들이 기억합니다.",12,TEAL);
        else fullAction(box,"항구의 작은 이야기",()->localStory(),true);
    }
    private void localStory() {
        int port=game.port;
        lifeDialog(PortLife.title(game),PortLife.story(game)+"\n\n두 선택 모두 하루가 흐릅니다. 의뢰·경주 기한도 진행됩니다. 항구마다 한 번만 보상받습니다.",
            "무상으로 돕기 · 40 G / 명성 +2 / 사기 +8",()->finishLocalStory(port,0),
            "품삯 받고 돕기 · +90 G / 명성 +1 / 사기 +2",()->finishLocalStory(port,1));
    }
    private void lifeDialog(String title,String words,String first,Runnable left,String second,Runnable right) {
        LinearLayout box=column();box.setPadding(dp(14),dp(12),dp(14),dp(12));box.setBackgroundColor(NAVY);
        label(box,words,15,INK);fullAction(box,first,()->lifeChoice(left),true);fullAction(box,second,()->lifeChoice(right),false);
        ScrollView scroll=new ScrollView(this);scroll.setTag("port-life-scroll");scroll.addView(box);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton(getString(R.string.ui_another_time),null).create();
        dialogueDialog=dialog;dialog.setOnDismissListener(d->{if(dialogueDialog==dialog) dialogueDialog=null;});dialog.show();
    }
    private void lifeChoice(Runnable action) {
        AlertDialog previous=dialogueDialog;dialogueDialog=null;if(previous!=null) previous.dismiss();action.run();
    }
    private void finishLocalStory(int port,int choice) {
        if(port!=game.port) return;
        changed(PortLife.choose(game,choice),"discovery");
    }
    private void tavern() {
        label(content,World.PORTS[game.port]+" · 바람의 주점",25,GOLD);
        scene("facility-scenes-v1.png",7);
        npc("주점 주인","어서 와요, 선장님. 따뜻한 식사와 먼 항구의 소문이 기다리고 있어요.");
        label(content,"일행 사기 "+game.town.morale+" / 100",16,TEAL);
        fullAction(content,"식사와 휴식 · 60 G / 하루 / 사기 +25",()->changed(TownRpg.rest(game),"rest"),true);
        fullAction(content,"현지 소문 듣기 · 첫 방문 명성 +1",()->{
            boolean success=TownRpg.hearRumor(game);changed(success);
            if(success) dialog("주점에서 들은 이야기",game.message);
        },false);
        adventureShortcut(content);
        fullAction(content,"마을로 나가기",()->{tab=7;render();},false);
    }
    private void npc(String name,String words) {
        LinearLayout box=card(content);box.setBackgroundColor(0xffeee0bf);
        label(box,name,18,0xff5a3823);label(box,"“"+words+"”",14,0xff302d28);
    }
    private void depart(int target,int route) {
        if(!TravelFlow.begin(game,target,route)) { changed(false);return; }
        save();audio.play("depart");showSailing();
    }
    private void showSailing() {
        root=column();root.setBackgroundColor(NAVY);applyInsets(root);setContentView(root);
        label(root,World.PORTS[game.town.from]+" → "+World.PORTS[game.port],24,GOLD);
        label(root,"항해 중 · "+Engine.SHIPS[game.level-1]+" · 파도를 넘어 새로운 항구로",14,INK);
        sailing=new SailingView(this,game.level,game.town.from,game.port,()->finishSailing());
        voyageSpeedChoices();
        root.addView(sailing,new LinearLayout.LayoutParams(-1,0,1));
        fullAction(root,"항해 연출 건너뛰기 · 입항",()->finishSailing(),false);audio.scene("sea");
    }
    private void voyageSpeedChoices() {
        label(root,"연출 속도 · 비용·항해 일수·사건 결과는 동일",11,MUTED);
        LinearLayout choices=row();choices.setTag("voyage-speed-choices");root.addView(choices);
        int stored=prefs.getInt("voyage_speed",1);if(stored!=2 && stored!=4) stored=1;sailing.speed(stored);
        for(int speed:new int[]{1,2,4}) action(choices,speed+"×",()->setVoyageSpeed(choices,speed),stored==speed);
    }
    private void setVoyageSpeed(LinearLayout choices,int speed) {
        if(sailing==null) return;
        sailing.speed(speed);prefs.edit().putInt("voyage_speed",speed).apply();
        for(int i=0;i<choices.getChildCount();i++) {
            Button choice=(Button)choices.getChildAt(i);boolean selected=(1<<i)==speed;
            choice.setTextColor(selected?NAVY:INK);choice.setBackground(background(selected?GOLD:0xff234151,selected?GOLD:0xff3c5b68));
        }
    }
    private void finishSailing() {
        if(!game.town.moving()) return;
        if(sailing!=null) sailing.stop();sailing=null;
        int previous=TravelFlow.arrive(game);tab=7;save();render();audio.play("arrival");
        if(game.chapter>previous) storyReveal(previous);
        else if(game.adventure.pending>=0) showEvent();
    }
    private void applyInsets(LinearLayout layout) {
        layout.setOnApplyWindowInsetsListener((view,insets)->{
            view.setPadding(dp(16)+insets.getSystemWindowInsetLeft(),dp(8)+insets.getSystemWindowInsetTop(),
                dp(16)+insets.getSystemWindowInsetRight(),dp(4)+insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        layout.requestApplyInsets();
    }
    private void header() {
        LinearLayout title=row(); root.addView(title);
        TextView logo=text("푸른 수평선",23,GOLD); logo.setTypeface(Typeface.SERIF,Typeface.BOLD);
        icon(logo,GameIcon.COMPASS,32,false);logo.setGravity(Gravity.CENTER_VERTICAL);
        title.addView(logo,new LinearLayout.LayoutParams(0,dp(42),1));
        TextView day=text("항해 "+game.day+"일",13,MUTED); day.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        title.addView(day,new LinearLayout.LayoutParams(dp(95),dp(42)));
        LinearLayout stats=row();stats.setPadding(0,dp(3),0,dp(5));root.addView(stats);
        hud(stats,String.format(java.util.Locale.KOREA,"%,d G",game.gold),"gold",GameIcon.COIN,"금화");
        hud(stats,game.cargo()+"/"+game.capacity(),"cargo",GameIcon.CARGO,"화물");
        hud(stats,String.valueOf(game.fame),"fame",GameIcon.STAR,"명성");
    }
    private void icon(TextView view,int kind,int size,boolean above) {
        GameIcon art=new GameIcon(kind);art.setBounds(0,0,dp(size),dp(size));
        view.setCompoundDrawables(above?null:art,above?art:null,null,null);view.setCompoundDrawablePadding(dp(4));
    }
    private void hud(LinearLayout parent,String value,String tag,int kind,String name) {
        TextView item=text(value,13,INK);item.setTag("hud-"+tag);item.setContentDescription(name+" "+value);
        item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(5),dp(4),dp(3),dp(4));icon(item,kind,24,false);
        item.setBackground(new GameSurface(false));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
        p.setMargins(dp(2),0,dp(2),0);parent.addView(item,p);
    }
    private void guideCard() {
        int person=Crew.guidePerson(game.chapter);LinearLayout box=card(content),line=row();box.addView(line);
        line.addView(new PortraitView(this,person),new LinearLayout.LayoutParams(dp(80),dp(80)));
        LinearLayout words=column();words.setPadding(dp(12),0,0,0);
        line.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        label(words,Crew.NAMES[person],17,GOLD);label(words,Crew.GUIDES[game.chapter],13,INK);
    }
    private void goalShortcut(LinearLayout parent) {
        fullAction(parent,game.chapter==Story.REVEALS.length?"세계 지도 보기":"다음 목표 해역 보기",()->{
            tab=0;mapRegion=Crew.guideRegion(game);render();
        },false);
    }
    private void crewPage() {
        label(content,"COMPANIONS OF THE SEVEN SEAS",11,TEAL);label(content,"함께 만드는 세계 해도",25,INK);
        label(content,"이야기가 진행되면 새로운 동료가 합류합니다. 초상화와 소개를 확인해 보세요.",13,MUTED);
        LinearLayout duty=card(content);label(duty,"전문 담당 · 두 명까지 배치",18,GOLD);
        label(duty,"서린의 첫 방문 명성 보너스는 항상 적용됩니다. 다른 동료의 능력은 담당으로 배치했을 때 적용됩니다.",13,INK);
        for(int slot=0;slot<2;slot++) {
            final int index=slot;int person=slot==0?game.crew.first:game.crew.second;
            fullAction(duty,"담당 "+(slot+1)+": "+(person<0?"비어 있음":Crew.NAMES[person])+" · 변경",()->assignCrew(index),false);
        }
        for(int person=0;person<Crew.NAMES.length;person++) crewCard(person);
    }
    private void crewCard(int person) {
        boolean aboard=Crew.available(person,game.chapter);
        LinearLayout box=card(content),line=row();box.addView(line);
        PortraitView portrait=new PortraitView(this,person);portrait.setAlpha(aboard?1f:.4f);
        line.addView(portrait,new LinearLayout.LayoutParams(dp(112),dp(112)));
        LinearLayout info=column();info.setPadding(dp(12),0,0,0);
        line.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        label(info,Crew.NAMES[person],22,aboard?GOLD:MUTED);label(info,Crew.ROLES[person],13,INK);
        String joined=aboard?"● 함께 항해 중":Crew.JOINS[person]+"장 완료 후 합류";
        label(info,joined,12,aboard?TEAL:MUTED);label(box,Crew.BIO[person],14,INK);
        label(box,(CrewAbilities.active(game,person)?"◆ 적용 중 · ":"능력 · ")+CrewAbilities.EFFECTS[person],13,TEAL);
        crewStory(box,person);
        if(person>0 && aboard) { conversationCard(box,person);regionalTalkCard(box,person); }
    }
    private void regionalTalkCard(LinearLayout parent,int person) {
        label(parent,World.REGIONS[World.region(game.port)]+"에서 나누는 이야기",14,GOLD);
        fullAction(parent,PortLife.talked(game,person)?"이 해역의 대화 다시 읽기":"동료와 항구 산책",()->regionalTalk(person),false);
    }
    private void regionalTalk(int person) {
        String words=PortLife.topic(game,person);
        if(PortLife.talked(game,person)) { regionalResponse(person,words+"\n\n“"+PortLife.reply(person,PortLife.recorded(game,person)?1:0)+"”");return; }
        int port=game.port;
        lifeDialog(Crew.NAMES[person]+" · "+World.PORTS[port],words+"\n\n동료마다 해역당 한 번 선택합니다. 시간은 흐르지 않습니다.",
            "이야기 듣기 · 사기 +5",()->finishRegionalTalk(port,person,0),"함께 기록하기 · 명성 +1",()->finishRegionalTalk(port,person,1));
    }
    private void finishRegionalTalk(int port,int person,int choice) {
        if(port!=game.port) return;
        int chapter=game.chapter;boolean success=PortLife.talk(game,person,choice);
        String answer=Crew.NAMES[person]+": “"+PortLife.reply(person,choice)+"”";
        if(success) chapterReply=answer;changed(success,"rest");chapterReply=null;
        if(success && game.chapter==chapter) regionalResponse(person,answer+"\n\n"+game.message);
    }
    private void regionalResponse(int person,String words) {
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(Crew.NAMES[person]).setMessage(words).setPositiveButton(getString(R.string.ui_ok),null).create();
        dialogueDialog=dialog;dialog.setOnDismissListener(d->{if(dialogueDialog==dialog) dialogueDialog=null;});dialog.show();
    }
    private void assignCrew(int slot) {
        java.util.ArrayList<String> names=new java.util.ArrayList<String>();java.util.ArrayList<Integer> people=new java.util.ArrayList<Integer>();
        names.add("비워 두기");people.add(-1);
        for(int person=1;person<6;person++) {
            if(!Crew.available(person,game.chapter)) continue;
            names.add(Crew.NAMES[person]+" · "+CrewAbilities.EFFECTS[person]);people.add(person);
        }
        new AlertDialog.Builder(this).setTitle("전문 담당 "+(slot+1)+" 선택")
            .setItems(names.toArray(new String[0]),(d,index)->changed(CrewAbilities.assign(game,slot,people.get(index))))
            .setNegativeButton(getString(R.string.ui_close),null).show();
    }
    private void crewStory(LinearLayout box,int person) {
        label(box,"개인 기록: "+CrewAbilities.GOALS[person],12,MUTED);
        if(CrewAbilities.storyClaimed(game,person)) {
            fullAction(box,"열어 본 동료 기록 다시 읽기",()->dialog(Crew.NAMES[person]+"의 기록",CrewAbilities.RECORDS[person]),false);return;
        }
        Button button=button("개인 기록 열기 · "+(250+person*100)+" G + 명성 2",true);
        button.setEnabled(CrewAbilities.storyReady(game,person));
        button.setOnClickListener(v->{
            boolean success=CrewAbilities.claimStory(game,person);changed(success,"chapter");
            if(success) dialog(Crew.NAMES[person]+"의 기록",CrewAbilities.RECORDS[person]);
        });box.addView(button,buttonParams());
    }
    private void storyReveal(int previous) {
        LinearLayout panel=column();panel.setPadding(dp(20),dp(12),dp(20),dp(12));
        panel.setBackgroundColor(NAVY);int person=Crew.guidePerson(game.chapter);
        panel.addView(new PortraitView(this,person),new LinearLayout.LayoutParams(dp(144),dp(144)));
        label(panel,Crew.NAMES[person]+" · "+Crew.ROLES[person],16,GOLD);
        if(chapterReply!=null) label(panel,chapterReply,15,GOLD);
        for(int index=previous;index<game.chapter;index++) label(panel,Story.REVEALS[index],15,INK);
        label(panel,"다음 목표: "+Story.GOALS[game.chapter],14,TEAL);
        ScrollView scroll=new ScrollView(this);scroll.addView(panel);
        storyDialog=new AlertDialog.Builder(this).setTitle(game.chapter==Story.REVEALS.length?"세계 해도의 완성":"새로운 항해 기록")
            .setCancelable(game.adventure.pending<0)
            .setView(scroll).setPositiveButton(getString(R.string.ui_continue_voyage),(dialog,which)->{if(game.adventure.pending>=0) showEvent();}).create();
        storyDialog.setOnDismissListener(d->storyDialog=null);storyDialog.show();
    }
    private void expeditionPage() {
        int region=World.region(game.port);
        label(content,"잊힌 항로의 기록",26,GOLD);
        label(content,World.REGIONS[region]+" · 지도 조각 → 보물 → 해역 강적",13,TEAL);
        content.addView(new TreasureMapView(this,game),new LinearLayout.LayoutParams(-1,dp(220)));
        treasureCard(region);bossCard(region);
        LinearLayout links=card(content);label(links,"동료와 만드는 항해",19,GOLD);
        label(links,"선택형 대화에서 신뢰를 쌓으세요. 신뢰 4마다 강적전 포격 +1, 최대 +3. 현재 +"+CrewConversations.support(game),13,INK);
        fullAction(links,"동료와 이야기하기",()->{tab=3;render();},true);
        for(int r=0;r<7;r++) expeditionRecord(links,r);
    }
    private void treasureCard(int region) {
        LinearLayout box=card(content);int count=Expeditions.fragments(game,region),site=Expeditions.site(region);
        boolean found=(game.expedition.treasures & (1<<region))!=0,clue=(game.expedition.clues & (1L<<game.port))!=0;
        label(box,Expeditions.TREASURES[region]+(found?" · 발견 완료":" · 지도 "+Math.min(3,count)+" / 3"),20,GOLD);
        label(box,"서로 다른 항구 세 곳을 조사하면 "+World.PORTS[site]+"의 탐사 지점이 드러납니다.",13,INK);
        Button collect=button(clue?"이 항구의 단서 기록 완료":"항구의 오래된 기록 조사 · 40 G / 하루",!clue);
        collect.setEnabled(!clue);collect.setOnClickListener(v->changed(Expeditions.clue(game),"discovery"));box.addView(collect,buttonParams());
        if(count>=3 && !found) {
            fullAction(box,"보물 탐사 · 80 G / 하루 / 보상 "+(900+region*150)+" G",()->changed(Expeditions.dig(game),"discovery"),true);
            if(game.port!=site) fullAction(box,World.PORTS[site]+" 탐사 항구로",()->voyage(site),false);
        }
        if(count<3) clueRoutes(box,region);
    }
    private void clueRoutes(LinearLayout box,int region) {
        java.util.ArrayList<Integer> ports=new java.util.ArrayList<Integer>();java.util.ArrayList<String> names=new java.util.ArrayList<String>();
        int nearest=-1,cost=Integer.MAX_VALUE;
        for(int port:World.portsIn(region)) {
            if((game.expedition.clues & (1L<<port))!=0 || port==game.port) continue;
            ports.add(port);names.add(World.PORTS[port]+" · 약 "+game.travelDays(port)+"일");
            int price=game.travelCost(port,1);if(price<cost) { cost=price;nearest=port; }
        }
        if(nearest<0) return;
        final int target=nearest;
        fullAction(box,"가까운 미조사 항구 · "+World.PORTS[target],()->voyage(target),true);
        fullAction(box,"다른 단서 항구 선택",()->new AlertDialog.Builder(this).setTitle(getString(R.string.ui_clue_dock_first))
            .setItems(names.toArray(new String[0]),(d,which)->voyage(ports.get(which))).setNegativeButton(getString(R.string.ui_close),null).show(),false);
    }
    private void expeditionRecord(LinearLayout box,int region) {
        String map=(game.expedition.treasures & (1<<region))!=0?"보물 발견":"지도 "+Math.min(3,Expeditions.fragments(game,region))+"/3";
        String boss=(game.expedition.trophies & (1<<region))!=0?" · 훈장 획득":"";
        label(box,World.REGIONS[region]+" · "+map+boss,12,MUTED);
    }
    private void conversationCard(LinearLayout box,int person) {
        label(box,"인연 · 신뢰 "+game.expedition.trust[person-1]+" / 4",14,TEAL);
        int stage=CrewConversations.chosen(game,person,0)?1:0;
        boolean finished=CrewConversations.chosen(game,person,stage);
        if(finished) { label(box,"두 번의 선택을 모두 기록했습니다.",12,MUTED);return; }
        Button talk=button("선택형 대화 "+(stage+1)+" · 마음과 계획",true);
        talk.setEnabled(CrewConversations.unlocked(game,person,stage));
        talk.setOnClickListener(v->talkChoice(person,stage));box.addView(talk,buttonParams());
        if(stage==1) label(box,"보물 하나 발견 + 합류 이후 이야기 한 장 진행 시 다음 대화가 열립니다.",12,MUTED);
    }
    private void talkChoice(int person,int stage) {
        LinearLayout box=column();box.setPadding(dp(18),dp(12),dp(18),dp(12));box.setBackgroundColor(NAVY);
        box.addView(new PortraitView(this,person),new LinearLayout.LayoutParams(dp(110),dp(110)));
        label(box,CrewConversations.TOPICS[person-1][stage],16,INK);
        fullAction(box,"함께 마음을 나눈다 · 신뢰 +2 / 사기 +10",()->finishTalk(person,stage,0),true);
        fullAction(box,"실행 계획을 세운다 · 신뢰 +1 / 명성 +1",()->finishTalk(person,stage,1),false);
        ScrollView scroll=new ScrollView(this);scroll.addView(box);
        dialogueDialog=new AlertDialog.Builder(this).setTitle(Crew.NAMES[person]+" · 남겨질 선택").setView(scroll)
            .setNegativeButton(getString(R.string.ui_later),null).create();dialogueDialog.show();
    }
    private void finishTalk(int person,int stage,int choice) {
        if(dialogueDialog!=null) { dialogueDialog.dismiss();dialogueDialog=null; }
        changed(CrewConversations.choose(game,person,stage,choice),"chapter");
    }
    private void bossCard(int region) {
        LinearLayout box=card(content);boolean cleared=(game.expedition.trophies & (1<<region))!=0;
        label(box,RegionBoss.NAMES[region],20,GOLD);
        label(box,cleared?"해역 훈장 획득 · 항구 사람들이 승리를 기억합니다.":"보물을 찾으면 도전 가능 · 선체 40 / 선박 등급 "+(1+region/3)+" 이상",13,INK);
        label(box,"승리: "+(1200+region*300)+" G + 명성 5 + 고유 훈장\n적 선체 "+RegionBoss.maxHP(region)+" · 적의 다음 행동이 공개됩니다. 후퇴할 수 있습니다.",13,TEAL);
        Button start=button(cleared?"강적 격파 완료":"해역 강적 도전",true);start.setEnabled(!cleared);
        start.setOnClickListener(v->changed(RegionBoss.begin(game),"cannon"));box.addView(start,buttonParams());
    }
    private void showBoss() {
        if(isFinishing() || isDestroyed() || !game.expedition.active()) return;
        if(eventDialog!=null && eventDialog.isShowing()) return;
        if(notice!=null) { notice.cancel();notice=null; }
        int region=game.expedition.bossRegion,turn=game.expedition.bossTurn;
        LinearLayout box=column();box.setPadding(dp(14),dp(10),dp(14),dp(12));box.setBackgroundColor(NAVY);
        battleScene(box,game.expedition.bossHP,RegionBoss.maxHP(region));
        label(box,"우리 선체 "+game.hull+" / "+game.maxHull()+"   적 선체 "+game.expedition.bossHP+" / "+RegionBoss.maxHP(region),16,GOLD);
        label(box,RegionBoss.INTENTS[RegionBoss.intent(game)],14,TEAL);
        label(box,"동료 지원 +"+CrewConversations.support(game)+" · 숙련 +"+TownRpg.rankBonus(game)+" · "+game.expedition.bossTurn+"턴 경과",12,MUTED);
        if(turn>0) label(box,game.message,12,MUTED);
        fullAction(box,"집중 포격 · 높은 피해",()->chooseBossTurn(0,turn),true);
        fullAction(box,"방어 사격 · 주는 피해 감소 / 받는 피해 ⅓",()->chooseBossTurn(1,turn),false);
        fullAction(box,"후퇴 · 선체 −4 / 사기 −5 / 하루",()->chooseBossTurn(2,turn),false);
        box.getChildAt(box.getChildCount()-1).setTag("boss-retreat");
        ScrollView scroll=new ScrollView(this);scroll.setTag("boss-scroll");scroll.addView(box);
        eventDialog=new AlertDialog.Builder(this).setTitle(RegionBoss.NAMES[region]).setView(scroll).setCancelable(false).create();
        eventDialog.getWindow().setWindowAnimations(0);eventDialog.show();
    }
    private void chooseBoss(int choice) {
        if(eventDialog!=null) { eventDialog.dismiss();eventDialog=null; }
        int chapter=game.chapter,battles=game.adventure.battles,ourHP=game.hull,enemyHP=game.expedition.bossHP;
        int enemyMax=RegionBoss.maxHP(game.expedition.bossRegion);
        boolean success=RegionBoss.choose(game,choice);
        if(success) battleFrame=new BattleFrame(choice,ourHP,game.hull,game.maxHull(),enemyHP,game.battleEnemyAfter,enemyMax);
        changed(success,GameFeedback.battle(success,choice,game.expedition.active(),game.adventure.battles>battles));
        if(success && !game.expedition.active() && game.chapter==chapter) battleResult("해역 전투 결과");
        battleFrame=null;
    }
    private void battleScene(LinearLayout parent,int enemyHP,int enemyMax) {
        BattleFrame snapshot=battleFrame;
        if(snapshot==null) snapshot=new BattleFrame(-1,game.hull,game.hull,game.maxHull(),enemyHP,enemyHP,enemyMax);
        battleFrame=null;parent.addView(new BattleSceneView(this,game.level,snapshot),new LinearLayout.LayoutParams(-1,dp(185)));
    }
    private void battleResult(String title) {
        LinearLayout box=column();box.setPadding(dp(14),dp(10),dp(14),dp(12));box.setBackgroundColor(NAVY);
        battleScene(box,0,1);label(box,game.message,15,INK);
        ScrollView scroll=new ScrollView(this);scroll.addView(box);
        eventDialog=new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton(getString(R.string.ui_resume_voyage),(d,w)->eventDialog=null).create();
        eventDialog.setOnDismissListener(d->eventDialog=null);eventDialog.getWindow().setWindowAnimations(0);eventDialog.show();
    }
    private void chooseBossTurn(int choice,int expected) {
        if(!game.expedition.active() || game.expedition.bossTurn!=expected) return;
        chooseBoss(choice);
    }
    private void audioSettings() {
        LinearLayout box=card(content);label(box,"음악과 효과음",18,GOLD);
        label(box,"항해 지도와 항구 화면에서 서로 다른 창작 연주곡이 재생됩니다. 앱을 벗어나면 소리가 멈춥니다.",13,INK);
        soundSwitch(box,"배경음악",audio.musicEnabled(),true);
        volumeSlider(box,true);
        soundSwitch(box,"효과음",audio.effectsEnabled(),false);
        volumeSlider(box,false);
        label(box,"미리 듣기도 효과음 켜기와 볼륨 설정을 따릅니다. 휴대폰 미디어 음량도 확인하세요.",12,MUTED);
        LinearLayout previews=row();box.addView(previews);
        action(previews,"입항 종소리 듣기",()->audio.play("arrival"),false);
        action(previews,"포격음 듣기",()->audio.play("cannon"),false);
    }
    private void volumeSlider(LinearLayout parent,boolean music) {
        int value=music?audio.musicVolume():audio.effectsVolume();
        TextView caption=text((music?"음악":"효과음")+" 볼륨 · "+value+"%",13,GOLD);parent.addView(caption);
        android.widget.SeekBar slider=new android.widget.SeekBar(this);slider.setMax(100);slider.setProgress(value);
        slider.setTag(music?"music-volume":"effects-volume");slider.setContentDescription(music?"배경음악 볼륨":"효과음 볼륨");
        slider.setMinimumHeight(dp(48));parent.addView(slider,new LinearLayout.LayoutParams(-1,dp(48)));
        slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar bar,int progress,boolean user) {
                if(music) audio.musicVolume(progress);else audio.effectsVolume(progress);
                caption.setText((music?"음악":"효과음")+" 볼륨 · "+progress+"%");
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar bar) { }
            @Override public void onStopTrackingTouch(android.widget.SeekBar bar) { if(!music) audio.play("trade"); }
        });
    }
    private void soundSwitch(LinearLayout parent,String title,boolean checked,boolean music) {
        Switch toggle=new Switch(this);toggle.setText(title);toggle.setTextSize(16);toggle.setTextColor(INK);
        toggle.setChecked(checked);toggle.setMinHeight(dp(48));toggle.setSoundEffectsEnabled(false);
        toggle.setOnCheckedChangeListener((view,value)->{if(music) audio.musicEnabled(value);else audio.effectsEnabled(value);});
        parent.addView(toggle,new LinearLayout.LayoutParams(-1,-2));
    }
    private void tradeHint(LinearLayout box,int good) {
        TradeHint hint=TradeHint.best(game,good,quantity);if(hint==null) return;
        if(hint.net<=0) { label(box,quantity+"개 기준, 보급비를 넘는 추천 거래가 없습니다.",12,MUTED);return; }
        label(box,"추천 판매처: "+World.PORTS[hint.port]+" · 예상 차익 +"+hint.net+" G",13,TEAL);
        label(box,"현재 시세·담당·개조 반영 / 도착 시 시세 변동 가능\n"+quantity+"개·일반항로 편도 기준 / 수리·귀환비 별도\n매입+보급 필요 "+hint.cost+" G",11,MUTED);
    }
    private void adventureShortcut(LinearLayout parent) {
        fullAction(parent,"잊힌 항로 · 보물지도와 해역 강적",()->{tab=9;render();},true);
        fullAction(parent,"의뢰 · 탐험 · 달성 기록",()->{tab=5;render();},false);
        fullAction(parent,"라이벌 상단 · 동맹과 경주",()->{tab=6;render();},false);
    }
    private void rivalPage() {
        label(content,"RIVAL TRADING GUILDS",11,TEAL);label(content,"경쟁자와 동반자",25,INK);
        fullAction(content,"항해 지도로 돌아가기",()->{tab=0;render();},false);
        LinearLayout market=card(content);label(market,"일곱 해역의 상권",18,GOLD);
        label(market,"7일마다 비동맹 상단의 압력은 높아지고 동맹은 낮아집니다. 긴 항해 한 번의 변화는 최대 8입니다. 판매·항구 투자·경주 승리로 현지 압력을 낮출 수 있습니다.",13,INK);
        for(int region=0;region<7;region++) {
            int port=region*8;
            label(market,World.REGIONS[region]+" · "+Rivals.NAMES[Rivals.owner(port)]+"\n경쟁 압력 "+game.rivals.pressure[region]+" / 80",13,MUTED);
        }
        label(market,"비동맹 압력 60 이상: 매입 수수료 3%. 동맹: 판매 비율 +2%p. 압력 0: 추가 +2%p. 모든 판매 비율의 총 상한은 94%입니다.",12,TEAL);
        raceCard();
        for(int rival=0;rival<3;rival++) guildCard(rival);
    }
    private void guildCard(int rival) {
        LinearLayout box=card(content);boolean allied=Rivals.allied(game,rival);
        label(box,Rivals.NAMES[rival]+" · "+Rivals.LEADERS[rival],19,GOLD);
        label(box,"기반 해역: "+Rivals.REGIONS[rival],12,MUTED);
        label(box,"관계 "+game.rivals.relations[rival]+" (-50~100) · "+(allied?"교역 동맹":"경쟁 중")+"\n납품 경주 "+game.rivals.wins[rival]+"승",14,TEAL);
        label(box,"“"+Rivals.dialogue(game,rival)+"”",14,INK);
        int wait=Math.max(0,7-(game.day-game.rivals.lastNegotiation[rival]));
        label(box,wait==0?"서신 교류 가능 · 성공 시 하루 경과":"다음 서신 교류까지 "+wait+"일",12,MUTED);
        fullAction(box,"서신 교류 · 선물 / 실적 / 경쟁",()->negotiateGuild(rival),false);
        if(allied) fullAction(box,"동맹 종료 · 관계 −20, 하루",()->new AlertDialog.Builder(this)
            .setTitle(getString(R.string.ui_end_alliance_title)).setMessage(getString(R.string.ui_end_alliance_warning))
            .setPositiveButton(getString(R.string.ui_end),(d,w)->changed(Rivals.endAlliance(game,rival))).setNegativeButton(getString(R.string.ui_keep),null).show(),false);
        else fullAction(box,"동맹 제안 · 500 G / 관계 40·명성 15 필요",()->changed(Rivals.formAlliance(game,rival)),true);
    }
    private void negotiateGuild(int rival) {
        String[] options={"교역 선물 · 150 G / 관계 +12","새 납품 실적 공유 · 관계 +10 / 실적 1회당 상단별 한 번","현지 공개 경쟁 · 명성 20 필요 / 압력 −8·관계 −8"};
        new AlertDialog.Builder(this).setTitle(Rivals.LEADERS[rival]+"에게 보낼 서신")
            .setItems(options,(d,choice)->changed(Rivals.negotiate(game,rival,choice)))
            .setNegativeButton(getString(R.string.ui_close),null).show();
    }
    private void raceCard() {
        LinearLayout box=card(content);label(box,"상단 납품 경주",18,GOLD);RivalState s=game.rivals;
        if(s.raceTarget<0) {
            Contract offer=TradeRace.offer(game);
            label(box,Rivals.NAMES[Rivals.owner(game.port)]+"와 경쟁\n"+World.PORTS[game.port]+" → "+World.PORTS[offer.target],16,INK);
            label(box,Engine.GOODS[offer.good]+" "+offer.amount+"개 · "+offer.deadline+"일째까지 납품\n승리 보상 "+offer.reward+" G · 명성 +3 · 관계 +8",13,TEAL);
            label(box,"참가비 100 G. 화물은 직접 준비하며 납품 시 소모됩니다. 준비와 보급비를 확인하세요. 참가 후 7일 간격으로 열립니다. 기한은 개조 전 항해 일수 기준이라 빠른 배가 유리합니다.",12,MUTED);
            fullAction(box,"경주 참가 · 100 G",()->changed(TradeRace.start(game)),true);return;
        }
        label(box,Rivals.NAMES[s.raceRival]+"와 경쟁\n"+World.PORTS[s.raceFrom]+" → "+World.PORTS[s.raceTarget],16,INK);
        label(box,Engine.GOODS[s.raceGood]+" "+s.raceAmount+"개 · 보유 "+game.inventory[s.raceGood]+"개\n보상 "+s.raceReward+" G · 명성 +3",14,TEAL);
        label(box,GameClock.pastDeadline(game,s.raceDeadline)?"상대가 먼저 납품했습니다. 결과를 확인하세요.":"기한 "+s.raceDeadline+"일째 (당일 납품 인정) · 남은 기간 "+(s.raceDeadline-game.day)+"일",13,GOLD);
        fullAction(box,"납품 / 경주 결과 확인",()->changed(TradeRace.complete(game)),true);
        fullAction(box,"경주 목적지 항로 보기",()->voyage(s.raceTarget),false);
        fullAction(box,"경주 포기 · 참가비 반환 없음",()->new AlertDialog.Builder(this).setTitle(getString(R.string.ui_abandon_race_title))
            .setMessage(getString(R.string.ui_abandon_race_warning))
            .setPositiveButton(getString(R.string.ui_abandon),(d,w)->changed(TradeRace.abandon(game))).setNegativeButton(getString(R.string.ui_continue),null).show(),false);
    }
    private void showEvent() {
        if(isFinishing() || isDestroyed()) return;
        if(game.adventure.pending<0 || game.town.moving()) return;
        if(eventDialog!=null && eventDialog.isShowing()) return;
        LinearLayout box=column();box.setPadding(dp(18),dp(12),dp(18),dp(12));box.setBackgroundColor(NAVY);
        if(game.adventure.battleHP>0) battleScene(box,game.adventure.battleHP,30+game.level*8+World.region(game.port)*2);
        int person=Crew.guidePerson(game.chapter);
        box.addView(new PortraitView(this,person),new LinearLayout.LayoutParams(dp(92),dp(92)));
        label(box,Crew.NAMES[person]+"와 함께 결정하세요",14,GOLD);
        label(box,EventCatalog.body(game),15,INK);
        ScrollView scroll=new ScrollView(this);scroll.addView(box);
        final AlertDialog dialog=new AlertDialog.Builder(this).setTitle(EventCatalog.title(game)).setView(scroll).create();
        dialog.setCancelable(false);eventDialog=dialog;
        dialog.setOnDismissListener(d->{if(eventDialog==dialog) eventDialog=null;});
        for(int index=0;index<3;index++) {
            final int choice=index;
            fullAction(box,EventCatalog.option(game,choice),()->{dialog.dismiss();root.post(()->resolveEvent(choice));},index==0);
        }
        showEventWindow(dialog);
    }
    private void showEventWindow(AlertDialog dialog) {
        if(game.adventure.battleHP>0) dialog.getWindow().setWindowAnimations(0);
        dialog.show();
    }
    private void resolveEvent(int choice) {
        if(isFinishing() || isDestroyed()) return;
        int chapter=game.chapter,souvenirs=game.adventure.souvenirs,battles=game.adventure.battles;
        boolean battle=game.adventure.battleHP>0;int ourHP=game.hull,enemyHP=game.adventure.battleHP;
        boolean success=VoyageEvents.choose(game,choice);
        if(battle && success) eventBattleFrame(choice,ourHP,enemyHP);
        String sound=battle?GameFeedback.battle(success,choice,game.adventure.battleHP>0,game.adventure.battles>battles):"trade";
        if(game.adventure.souvenirs!=souvenirs) sound="discovery";
        changed(success,sound);
        eventResult(success,chapter,battle);
        battleFrame=null;
    }
    private void eventResult(boolean success,int chapter,boolean battle) {
        if(!success || game.adventure.pending>=0 || game.chapter!=chapter) return;
        if(battle) battleResult("해상 전투 결과");else dialog("항해 사건 결과",game.message);
    }
    private void eventBattleFrame(int choice,int ourHP,int enemyHP) {
        battleFrame=new BattleFrame(choice,ourHP,game.hull,game.maxHull(),enemyHP,game.battleEnemyAfter,30+game.level*8+World.region(game.port)*2);
    }
    private void adventurePage() {
        label(content,"ADVENTURE & COMMISSIONS",11,TEAL);label(content,"선장의 모험 수첩",25,INK);
        fullAction(content,"항해 지도로 돌아가기",()->{tab=0;render();},false);
        LinearLayout stats=card(content);label(stats,"지금까지의 항해",18,GOLD);
        AdventureState a=game.adventure;
        label(stats,"출항 "+a.voyages+"회 · 사건 해결 "+a.resolved+"회\n해상전 승리 "+a.battles+"회 · 구조 "+a.rescues+"회\n의뢰 완료 "+a.deliveries+"회 · 해류 관측 "+a.surveys+"회",14,INK);
        label(stats,"첫 출항에는 사건이 없습니다. 이후 일반 항로는 매회, 안전 항로는 두 번에 한 번 사건이 발생합니다.",12,MUTED);
        contractCard();collectionCard();
        for(int index=0;index<Achievements.NAMES.length;index++) achievementCard(index);
    }
    private void contractCard() {
        LinearLayout box=card(content);label(box,"항구 납품 의뢰",18,GOLD);AdventureState a=game.adventure;
        if(a.target<0) {
            label(box,"이동 거리·기한·화물 비용을 비교해 하나를 선택하세요. 완료하면 새 제안으로 바뀝니다.",13,MUTED);
            Contract[] offers=Contracts.offers(game);
            for(int choice=0;choice<offers.length;choice++) contractOffer(box,offers[choice],choice);
            return;
        }
        label(box,World.PORTS[a.origin]+" → "+World.PORTS[a.target],17,INK);
        label(box,Engine.GOODS[a.good]+" "+a.amount+"개 납품 · 보유 "+game.inventory[a.good]+"개\n보상 "+a.reward+" G + 명성 3",14,TEAL);
        label(box,GameClock.pastDeadline(game,a.deadline)?"납품 기한이 지났습니다. 위약금 없이 취소할 수 있습니다.":"납기 "+a.deadline+"일째 · 남은 기간 "+(a.deadline-game.day)+"일",13,GOLD);
        fullAction(box,"목적지에서 납품하기",()->changed(Contracts.deliver(game)),true);
        fullAction(box,"의뢰 목적지 항로 보기",()->voyage(a.target),false);
        fullAction(box,"의뢰 취소 · 위약금 없음",()->new AlertDialog.Builder(this).setTitle(getString(R.string.ui_cancel_contract_title))
            .setMessage(getString(R.string.ui_cancel_contract_cargo)).setPositiveButton(getString(R.string.ui_cancel_contract),(d,w)->changed(Contracts.cancel(game))).setNegativeButton(getString(R.string.ui_keep),null).show(),false);
    }
    private void contractOffer(LinearLayout parent,Contract offer,int choice) {
        LinearLayout box=card(parent);
        label(box,Contracts.TYPES[choice]+" · "+World.PORTS[offer.target],18,GOLD);
        int cargo=game.buyPrice(offer.good)*offer.amount,supplies=game.travelCost(offer.target,1);
        label(box,Engine.GOODS[offer.good]+" "+offer.amount+"개 · 약 "+game.travelDays(offer.target)+"일 항해",14,INK);
        label(box,"기한 "+offer.deadline+"일째 ("+(offer.deadline-game.day)+"일 남음) · 보상 "+offer.reward+" G + 명성 3",13,TEAL);
        label(box,"현지 매입 "+cargo+" G + 안전항로 보급 "+supplies+" G\n예상 잔여 "+(offer.reward-cargo-supplies)+" G · 수리·귀환·사건 비용 별도",12,MUTED);
        fullAction(box,"이 의뢰 수락",()->changed(Contracts.accept(game,choice)),true);
    }
    private void collectionCard() {
        LinearLayout box=card(content);label(box,"일곱 해역의 기념품",18,GOLD);
        label(box,"난파선과 작은 섬에서 해역별 기념품을 발견할 수 있습니다. 같은 해역의 기념품은 하나로 기록됩니다.",13,INK);
        for(int region=0;region<World.REGIONS.length;region++) {
            boolean found=(game.adventure.souvenirs & (1<<region))!=0;
            label(box,(found?"◆ ":"◇ ")+World.REGIONS[region]+(found?" · 수집 완료":" · 미발견"),14,found?TEAL:MUTED);
        }
    }
    private void achievementCard(int index) {
        LinearLayout box=card(content);boolean claimed=Achievements.claimed(game,index);
        label(box,Achievements.NAMES[index],18,GOLD);label(box,Achievements.GOALS[index],13,INK);
        label(box,"보상 "+Achievements.REWARDS[index]+" G + 명성 1",13,TEAL);
        Button claim=button(claimed?"보상 수령 완료":"달성 보상 받기",!claimed);
        claim.setEnabled(!claimed && Achievements.ready(game,index));
        claim.setOnClickListener(v->changed(Achievements.claim(game,index)));box.addView(claim,buttonParams());
    }
    private void harbor() {
        label(content,"WORLD ATLAS · 7 SEAS · 56 PORTS",11,TEAL);
        label(content,Engine.PORTS[game.port]+" 항구",27,INK);
        label(content,World.REGIONS[World.region(game.port)]+" · 방문 "+game.visitedCount()+" / "+World.COUNT+"개 항구",12,MUTED);
        guideCard();
        regionSelector();
        label(content,mapRegion<0?"세계 지도 · 해역을 선택하면 항구가 확대됩니다.":World.REGIONS[mapRegion]+" · 지도 또는 아래 목록에서 출항",12,MUTED);
        OceanView ocean=new OceanView(this,game,mapRegion, target -> voyage(target));
        content.addView(ocean,new LinearLayout.LayoutParams(-1,dp(300)));
        LinearLayout quest=card(content);
        label(quest,Story.TITLES[game.chapter],17,GOLD);
        label(quest,Story.GOALS[game.chapter],14,INK);
        goalShortcut(quest);
        adventureShortcut(quest);
        LinearLayout actions=row(); content.addView(actions);
        action(actions,"교역소",()-> {tab=1;render();},true);
        action(actions,"조선소",()-> {tab=2;render();},false);
        LinearLayout ports=card(content); label(ports,mapRegion<0?"전 세계 출항 항구":"출항할 항구 · "+World.REGIONS[mapRegion],16,GOLD);
        addPortList(ports);
        label(content,game.message,13,MUTED);
    }
    private void regionSelector() {
        HorizontalScrollView selector=new HorizontalScrollView(this);selector.setHorizontalScrollBarEnabled(false);
        LinearLayout regions=row();selector.addView(regions);content.addView(selector);
        regionButton(regions,-1,"세계 전체");
        for(int i=0;i<World.REGIONS.length;i++) regionButton(regions,i,World.REGIONS[i]);
        selector.post(()->selector.scrollTo(Math.max(0,regions.getChildAt(mapRegion+1).getLeft()-dp(12)),0));
        fullAction(content,"현재 항구 해역 보기",()->{mapRegion=World.region(game.port);render();},false);
    }
    private void regionButton(LinearLayout row,int region,String name) {
        Button button=button(name,mapRegion==region);
        LinearLayout.LayoutParams params=buttonParams();params.width=-2;
        button.setPadding(dp(14),dp(8),dp(14),dp(8));row.addView(button,params);
        button.setOnClickListener(v->{mapRegion=region;render();});
    }
    private void addPortList(LinearLayout ports) {
        for(int target:World.portsIn(mapRegion)) {
            if(mapRegion<0 && target%8==0) label(ports,World.REGIONS[World.region(target)],15,TEAL);
            addDestination(ports,target);
        }
    }
    private void addDestination(LinearLayout box,int target) {
        if(target==game.port) { label(box,"⚑ "+Engine.PORTS[target]+" · 현재 항구",15,GOLD);return; }
        String visited=game.hasVisited(target)?"✓ ":"";
        Button button=button(visited+Engine.PORTS[target]+"  ·  "+game.travelDays(target)+"일  ·  "+game.travelCost(target,0)+" G부터",false);
        button.setOnClickListener(v->voyage(target)); box.addView(button,buttonParams());
    }
    private void voyage(int target) {
        if(target==game.port) { Toast.makeText(this,"현재 머무는 항구입니다.",Toast.LENGTH_SHORT).show(); return; }
        Route route=WorldRoutes.between(game.port,target);
        String info=route.description()+"\n\n약 "+Math.round(route.miles)+"해리 · "+game.travelDays(target)+"일 항해 (개조 반영)\n\n일반 항로: "+game.travelCost(target,0)+" G · 선체 −"+game.travelDamage(target,0)+"\n안전 항로: "+game.travelCost(target,1)+" G · 선체 −"+game.travelDamage(target,1)+"\n\n선체 25 이상이면 출항할 수 있습니다. 경유 항구는 정박하지 않으며, 방문 기록은 목적지에만 남습니다.";
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_departure_title)).setMessage(info)
            .setPositiveButton(getString(R.string.ui_normal_route),(d,w)->depart(target,0))
            .setNeutralButton(getString(R.string.ui_safe_route),(d,w)->depart(target,1))
            .setNegativeButton(getString(R.string.ui_cancel),null).show();
    }
    private void market() {
        label(content,Engine.PORTS[game.port]+" 교역소",26,GOLD);
        content.addView(new SceneView(this,"facility-scenes-v1.png",TownRpg.style(game.port)),new LinearLayout.LayoutParams(-1,dp(130)));
        label(content,"상인 · 물건을 골라 주문서를 확인하세요.",13,MUTED);
        label(content,"매입 수수료 "+Rivals.buyFee(game,game.port)+"% · 판매 우대 +"+Rivals.saleBonus(game,game.port)+"%p · 화물 여유 "+(game.capacity()-game.cargo())+"개",12,TEAL);
        for(int line=0;line<2;line++) {
            LinearLayout shelf=row();content.addView(shelf);
            for(int col=0;col<3;col++) {
                final int good=line*3+col;
                String name=Engine.GOODS[good]+"  "+game.buyPrice(good)+" G";
                action(shelf,name,()->{selectedGood=good;render();},selectedGood==good);
            }
        }
        goodsCard(selectedGood);
        LinearLayout tip=card(content);label(tip,"현지 상인의 조언",16,GOLD);label(tip,World.marketTip(game.port),13,INK);
        fullAction(tip,"라이벌 상단과 현지 상권",()->{tab=6;render();},false);
    }
    private void goodsCard(int good) {
        LinearLayout box=card(content);
        label(box,"◆  MERCHANT'S SELECTION",11,TEAL);
        label(box,Engine.GOODS[good]+"     보유 "+game.inventory[good]+"개",23,GOLD);
        int protectedCargo=MissionCargo.held(game,good);
        if(protectedCargo>0) label(box,"◆ 목표용 "+protectedCargo+"개 · 판매 주문에서 기본 보관",13,TEAL);
        label(box,GoodsCatalog.description(good),14,INK);
        label(box,"오늘의 단가  ·  매입 "+game.buyPrice(good)+" G  /  판매 "+game.sellPrice(good)+" G",15,GOLD);
        label(box,"주문서에서 수량·예산·최대 수량을 선택하고 결제 내역을 확인하세요.",12,MUTED);
        tradeHint(box,good);
        LinearLayout buttons=row(); box.addView(buttons);
        action(buttons,"매입 주문",()->openTrade(good,false),true);
        action(buttons,"판매 주문",()->openTrade(good,true),false);
    }
    private void openTrade(int good,boolean selling) {
        new TradeDialog(this,game,good,selling,quantity,success->changed(success)).show();
    }
    private void shipyard() {
        if(game.gold<Math.max(FundingPlan.recoveryCost(game),game.hull<40?game.repairCost():40)) recoveryWork();
        scene("town-scenes-v1.png",7);
        npc("조선소 장인","돛과 선체를 살펴보겠소. 먼바다에 나가기 전엔 수리부터 챙기시오.");
        label(content,"FLEET & HARBOR",11,TEAL); label(content,"선박과 항구",26,INK);
        LinearLayout ship=card(content); label(ship,Engine.SHIPS[game.level-1],21,GOLD);
        label(ship,ShipPerformance.description(game.level),12,TEAL);
        label(ship,"선체 "+game.hull+" / "+game.maxHull()+"   ·   적재량 "+game.capacity()+"개",15,INK);
        label(ship,"항해마다 선체가 소모됩니다. 25 미만이면 수리가 필요해요.",13,MUTED);
        fullAction(ship,"선체 수리 · "+game.repairCost()+" G",()->changed(game.repair(),"repair"),false);
        String upgrade=game.level==3?"최고 등급 선박":Engine.SHIPS[game.level]+" 구입 · "+game.upgradeCost()+" G";
        fullAction(ship,upgrade,()->changed(game.upgrade(),"upgrade"),true);
        refitCards();
        LinearLayout ally=card(content); label(ally,Engine.PORTS[game.port]+" 교역 동맹",18,GOLD);
        label(ally,"동맹 "+game.influence[game.port]+" / 3 · 단계마다 판매가 +2%, 명성 +3",13,INK);
        String invest=game.influence[game.port]==3?"동맹 완성":"교역소 투자 · "+((game.influence[game.port]+1)*300)+" G";
        fullAction(ally,invest,()->changed(game.invest()),false);
        LinearLayout work=card(content); label(work,"항구 일거리",18,TEAL);
        label(work,"부두에서 하루 일하면 120 G를 받습니다. 보급비와 수리비가 부족할 때 이용하세요.",14,INK);
        fullAction(work,"하루 일하기 · +120 G",()->changed(game.work()),true);
    }
    private void recoveryWork() {
        LinearLayout work=card(content);label(work,"다시 출항할 준비",19,GOLD);
        label(work,"자금이 부족할 때 이용할 수 있는 항구 일거리입니다. 하루 경과 · +120 G. 의뢰와 경주 기한도 흐릅니다.",13,INK);
        Button earn=button("하루 일하기 · +120 G",true);earn.setTag("funding-recovery");
        earn.setOnClickListener(v->changed(game.work()));work.addView(earn,buttonParams());
    }
    private void refitCards() {
        LinearLayout summary=card(content);label(summary,"선박 개조 · 슬롯 "+game.refits.used()+" / "+Refits.slots(game),18,GOLD);
        label(summary,"부품은 각각 3단계까지 장착합니다. 단계마다 슬롯 1개를 사용하며, 큰 배로 바꿔도 유지됩니다. 해제하면 해당 단계 비용의 절반을 돌려받습니다.",13,INK);
        for(int part=0;part<4;part++) refitCard(part);
    }
    private void refitCard(int part) {
        LinearLayout box=card(content);int tier=game.refits.levels[part];
        label(box,Refits.NAMES[part]+" · "+tier+" / 3단계",17,GOLD);label(box,Refits.EFFECTS[part],13,INK);
        Button install=button(tier==3?"최고 단계":"한 단계 장착 · "+Refits.cost(game,part)+" G",true);
        install.setEnabled(tier<3 && game.refits.used()<Refits.slots(game));
        install.setOnClickListener(v->changed(Refits.install(game,part),"upgrade"));box.addView(install,buttonParams());
        Button remove=button("한 단계 해제 · "+Refits.refund(game,part)+" G 환급",false);remove.setEnabled(tier>0);
        remove.setOnClickListener(v->changed(Refits.remove(game,part)));box.addView(remove,buttonParams());
    }
    private void journal() {
        label(content,"CAPTAIN'S JOURNAL",11,TEAL); label(content,"서린의 항해일지",26,INK);
        LinearLayout quest=card(content); label(quest,Story.TITLES[game.chapter],18,GOLD);
        label(quest,Story.GOALS[game.chapter],14,INK);
        goalShortcut(quest);audioSettings();
        adventureShortcut(quest);
        for(int i=0;i<game.chapter;i++) { LinearLayout story=card(content); label(story,Story.REVEALS[i],14,INK); }
        LinearLayout entries=card(content); label(entries,"최근 항해 기록",17,TEAL);
        for(int i=game.log.size()-1;i>=0;i--) label(entries,game.log.get(i),13,INK);
        LinearLayout backup=card(content); label(backup,"자동 저장과 백업",18,GOLD);
        label(backup,"거래와 항해 후 기기에 자동 저장됩니다. 앱 삭제 전에는 파일로 백업하세요. 불러오면 현재 진행을 교체합니다.",13,INK);
        fullAction(backup,"저장 파일 내보내기",()->exportSave(),true);
        fullAction(backup,"저장 파일 불러오기",()->importSave(),false);
        fullAction(backup,"플레이 방법",()->dialog("선장 안내서",advancedHelp()+help()),false);
        fullAction(backup,"새 항해 시작",()->newGame(),false);
    }
    private String help() {
        return "1. 리스본에서 목재 5개를 매입하세요.\n2. 항해 지도에서 세비야를 선택해 출항하세요.\n3. 도착 후 목재 5개를 한 번에 판매하면 첫 장이 열립니다.\n\n세계 지도와 위쪽 해역 버튼을 이용해 7해역·56개 항구를 탐험하세요. 해역 버튼은 좌우로 밀 수 있습니다.\n\n먼 항해는 해상 경유 항구를 거치며 보급비가 증가합니다. 방문 기록은 목적지에 직접 정박해야 남습니다.\n\n첫 출항 이후 항해 사건이 발생합니다. 선택 전 비용과 위험을 확인하세요. 해상전은 포격·회피 사격·도주로 진행합니다.\n\n항해 화면이나 일지에서 ‘의뢰 · 탐험 · 달성 기록’을 열어 납품 의뢰, 해역 기념품, 달성 보상을 확인하세요. 화물 유실·기증 시 목록의 첫 보유 상품부터 소모됩니다.\n\n동료는 이야기 진행에 맞춰 합류합니다. 일지에서 BGM과 효과음을 따로 끌 수 있습니다.\n\n금화가 부족하면 조선소에서 하루 일해 보급·수리비를 마련할 수 있습니다.\n\n원작과 별개의 창작 이야기입니다. 지도는 Natural Earth 공개 데이터를 사용하며 좌표·항로는 게임용 근사치입니다.";
    }
    private String advancedHelp() {
        return "새로운 성장과 경쟁\n\n동료 메뉴에서 전문 담당 두 명을 배치하세요. 적용 중인 능력과 개인 기록 목표가 표시됩니다.\n\n조선소에서 돛·대포·선체·창고를 개조하세요. 개조 슬롯은 배 등급에 따라 3/5/7개이며, 새 배에도 부품이 유지됩니다.\n\n항해 화면의 ‘라이벌 상단’에서 관계를 쌓거나 현지 상권을 두고 경쟁할 수 있습니다. 관계 40·명성 15·500 G로 교역 동맹을 맺습니다.\n\n납품 경주는 참가비와 화물·보급비가 필요합니다. 빠른 배와 사건에서의 선택으로 기한을 지켜 보세요.\n\n기본 안내\n\n";
    }
    private void navigation() {
        status=text(saved?"● 기기에 자동 저장됨 · 오프라인":"저장 실패 · 백업 필요",11,saved?TEAL:GOLD);
        status.setTextSize(10);status.setPadding(0,dp(3),0,dp(2)); root.addView(status);
        LinearLayout nav=row(); root.addView(nav);
        String[] names={getString(R.string.ui_nav_town),getString(R.string.ui_nav_sail),getString(R.string.ui_nav_market),getString(R.string.ui_nav_crew),getString(R.string.ui_nav_journal)};int[] pages={7,0,1,3,4};
        for(int i=0;i<5;i++) navigationItem(nav,names[i],pages[i],i);
    }
    private void navigationItem(LinearLayout nav,String name,int page,int kind) {
        Button item=button(name,tab==page);item.setTag("nav-"+page);item.setTextSize(11);
        icon(item,kind,30,true);item.setPadding(dp(3),dp(5),dp(3),dp(5));
        item.setOnClickListener(v->{audio.play("click");tab=page;render();});
        LinearLayout.LayoutParams p=buttonParams();p.width=0;p.weight=1;p.setMargins(dp(2),dp(3),dp(2),0);nav.addView(item,p);
    }
    private void newGame() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_new_game_title))
            .setMessage(getString(R.string.ui_new_game_warning))
            .setPositiveButton(getString(R.string.ui_start_new),(d,w)->{
                game=new Engine(); tab=7;mapRegion=-1; prefs.edit().remove("backup").remove("save").remove("ending").commit();
                save();render();dialog("새로운 항해",Story.introduction());
            }).setNegativeButton(getString(R.string.ui_cancel),null).show();
    }
    private void exportSave() {
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE,"BlueHorizon-day"+game.day+".blue.txt");
        startActivityForResult(intent,10);
    }
    private void importSave() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.ui_import_save_title))
            .setMessage(getString(R.string.ui_import_save_warning))
            .setPositiveButton(getString(R.string.ui_choose_file),(d,w)->{
                Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("text/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,11);
            }).setNegativeButton(getString(R.string.ui_cancel),null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null) return;
        try {
            if(request==10) {
                try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"wt")) {
                    if(out==null) throw new java.io.IOException();
                    out.write(game.encode().getBytes(StandardCharsets.UTF_8));
                }
                dialog("백업 완료","저장 파일을 내보냈습니다. 다른 기기에서 불러올 수 있어요.");
            }
            if(request==11) readBackup(data);
        } catch(Exception ex) { dialog("파일 처리 실패","파일을 읽거나 쓸 수 없습니다. 다른 파일 또는 위치를 선택하세요."); }
    }
    private void readBackup(Intent data) throws java.io.IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream in=getContentResolver().openInputStream(data.getData())) {
            if(in==null) throw new java.io.IOException();
            byte[] buffer=new byte[2048]; int size;
            while((size=in.read(buffer))!=-1) {
                if(bytes.size()+size>30000) throw new java.io.IOException("Too large");
                bytes.write(buffer,0,size);
            }
        }
        Engine restored=Engine.decode(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
        if(restored==null) { dialog("불러오기 실패","올바른 푸른 수평선 저장 파일이 아닙니다. 현재 진행은 유지됩니다.");return; }
          game=restored;tab=7;mapRegion=-1;save();render();
          if(game.town.moving()) { showSailing();return; }
        if(game.expedition.active()) { showBoss();return; }
        String summary=game.day+"일째 항해를 복원했습니다.";
        if(game.message.startsWith("세계 확장 버전")) summary+="\n\n"+game.message;
        dialog("불러오기 완료",summary);
    }
    private void dialog(String title,String body) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setCancelable(game.adventure.pending<0)
            .setPositiveButton(getString(R.string.ui_ok),(d,w)->{if(game.adventure.pending>=0) showEvent();}).show();
    }
    private LinearLayout column() { LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);return box; }
    private LinearLayout row() { LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.HORIZONTAL);return box; }
    private TextView text(String value,int size,int color) {
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(color);
        view.setFontFeatureSettings("kern");view.setLineSpacing(dp(3),1);return view;
    }
    private void label(LinearLayout box,String value,int size,int color) {
        TextView view=text(value,size,color);view.setPadding(0,dp(4),0,dp(5));box.addView(view);
    }
    private GradientDrawable background(int color,int stroke) {
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(12));
        shape.setStroke(dp(1),stroke);return shape;
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout box=column();box.setPadding(dp(14),dp(10),dp(14),dp(12));
        box.setBackground(new GameSurface(false));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(10),0,dp(2));
        parent.addView(box,params);return box;
    }
    private Button button(String value,boolean active) {
        Button button=new Button(this);button.setText(value);button.setTextSize(13);button.setAllCaps(false);
        button.setSoundEffectsEnabled(false);
        button.setTextColor(active?NAVY:INK);button.setMinHeight(dp(48));button.setMinimumWidth(0);
        button.setPadding(dp(6),dp(6),dp(6),dp(6));
        button.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x4477c9c0),new GameSurface(active),null));
        return button;
    }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(dp(3),dp(5),dp(3),dp(3));return params;
    }
    private void action(LinearLayout parent,String value,Runnable run,boolean active) {
        Button button=button(value,active);button.setOnClickListener(v->{audio.play("click");run.run();});
        LinearLayout.LayoutParams params=buttonParams();params.width=0;params.weight=1;parent.addView(button,params);
    }
    private void fullAction(LinearLayout parent,String value,Runnable run,boolean active) {
        Button button=button(value,active);button.setOnClickListener(v->{audio.play("click");run.run();});parent.addView(button,buttonParams());
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
