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
    private int tab=0, quantity=5, mapRegion=-1;
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
        if(state!=null) { tab=state.getInt("tab",0); quantity=state.getInt("quantity",5);mapRegion=state.getInt("region",-1); }
        save(); render();
        if(fresh) dialog("푸른 수평선", Story.introduction());
        if(recovered) dialog("저장 복원", "최근 저장을 읽을 수 없어 이전 자동 저장을 복원했습니다.");
        if(game.message.startsWith("세계 확장 버전")) dialog("세계 항해 버전으로 이전",game.message);
    }
    @Override protected void onResume() { super.onResume();if(audio!=null) audio.resume(); }
    @Override protected void onPause() { if(audio!=null) audio.pause();super.onPause();if(game!=null) save(); }
    @Override protected void onDestroy() { if(audio!=null) audio.release();super.onDestroy(); }
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
        Toast.makeText(this,game.message,Toast.LENGTH_LONG).show();
        if(success && game.chapter>previousChapter) storyReveal(previousChapter);
    }
    private void render() {
        shownChapter=game.chapter;
        root=column(); root.setBackgroundColor(NAVY); root.setPadding(dp(16),dp(8),dp(16),dp(4));
        applyInsets(root);
        setContentView(root); header();
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=column(); content.setPadding(0,dp(6),0,dp(12)); scroll.addView(content);
        if(tab==0) harbor();
        if(tab==1) market();
        if(tab==2) shipyard();
        if(tab==3) crewPage();
        if(tab==4) journal();
        navigation();
        audio.scene(tab==0?"sea":"harbor");
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
        title.addView(logo,new LinearLayout.LayoutParams(0,dp(42),1));
        TextView day=text("항해 "+game.day+"일",13,MUTED); day.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        title.addView(day,new LinearLayout.LayoutParams(dp(95),dp(42)));
        TextView stats=text(String.format(java.util.Locale.KOREA,"%,d G   ·   화물 %d/%d   ·   명성 %d",game.gold,game.cargo(),game.capacity(),game.fame),14,INK);
        stats.setPadding(0,dp(4),0,dp(9)); root.addView(stats);
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
    }
    private void storyReveal(int previous) {
        LinearLayout panel=column();panel.setPadding(dp(20),dp(12),dp(20),dp(12));
        panel.setBackgroundColor(NAVY);int person=Crew.guidePerson(game.chapter);
        panel.addView(new PortraitView(this,person),new LinearLayout.LayoutParams(dp(144),dp(144)));
        label(panel,Crew.NAMES[person]+" · "+Crew.ROLES[person],16,GOLD);
        for(int index=previous;index<game.chapter;index++) label(panel,Story.REVEALS[index],15,INK);
        label(panel,"다음 목표: "+Story.GOALS[game.chapter],14,TEAL);
        ScrollView scroll=new ScrollView(this);scroll.addView(panel);
        new AlertDialog.Builder(this).setTitle(game.chapter==Story.REVEALS.length?"세계 해도의 완성":"새로운 항해 기록")
            .setView(scroll).setPositiveButton("계속 항해하기",null).show();
    }
    private void audioSettings() {
        LinearLayout box=card(content);label(box,"음악과 효과음",18,GOLD);
        label(box,"항해 지도와 항구 화면에서 서로 다른 창작 연주곡이 재생됩니다. 앱을 벗어나면 소리가 멈춥니다.",13,INK);
        soundSwitch(box,"배경음악",audio.musicEnabled(),true);
        soundSwitch(box,"효과음",audio.effectsEnabled(),false);
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
        label(box,"현재 매입가·"+quantity+"개·일반항로 편도 기준 / 수리·귀환비 별도\n매입+보급 필요 "+hint.cost+" G",11,MUTED);
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
        String info=route.description()+"\n\n약 "+Math.round(route.miles)+"해리 · "+route.days()+"일 항해\n\n일반 항로: "+game.travelCost(target,0)+" G · 선체 −"+game.travelDamage(target,0)+"\n안전 항로: "+game.travelCost(target,1)+" G · 선체 −"+game.travelDamage(target,1)+"\n\n선체 25 이상이면 출항할 수 있습니다. 경유 항구는 정박하지 않으며, 방문 기록은 목적지에만 남습니다.";
        new AlertDialog.Builder(this).setTitle("출항 준비").setMessage(info)
            .setPositiveButton("일반 항로",(d,w)->changed(game.sail(target,0),"depart"))
            .setNeutralButton("안전 항로",(d,w)->changed(game.sail(target,1),"depart"))
            .setNegativeButton("취소",null).show();
    }
    private void market() {
        label(content,"PORT MARKET",11,TEAL); label(content,Engine.PORTS[game.port]+" 교역소",26,INK);
        label(content,"가격은 1개 기준 · 같은 항구에서는 판매가가 더 낮아요.",12,MUTED);
        LinearLayout quantities=row(); content.addView(quantities);
        for(int amount:new int[]{1,5,10}) action(quantities,amount+"개씩",()->{quantity=amount;render();},quantity==amount);
        for(int good=0;good<6;good++) goodsCard(good);
        LinearLayout tip=card(content); label(tip,"초보 선장의 무역 수첩",16,GOLD);
        label(tip,World.marketTip(game.port),14,INK);
    }
    private void goodsCard(int good) {
        LinearLayout box=card(content);
        label(box,Engine.GOODS[good]+"     보유 "+game.inventory[good]+"개",18,INK);
        label(box,"매입 "+game.buyPrice(good)+" G   /   판매 "+game.sellPrice(good)+" G",14,GOLD);
        tradeHint(box,good);
        LinearLayout buttons=row(); box.addView(buttons);
        action(buttons,quantity+"개 매입",()->changed(game.buy(good,quantity)),true);
        action(buttons,quantity+"개 판매",()->changed(game.sell(good,quantity)),false);
    }
    private void shipyard() {
        label(content,"FLEET & HARBOR",11,TEAL); label(content,"선박과 항구",26,INK);
        LinearLayout ship=card(content); label(ship,Engine.SHIPS[game.level-1],21,GOLD);
        label(ship,"선체 "+game.hull+" / "+game.maxHull()+"   ·   적재량 "+game.capacity()+"개",15,INK);
        label(ship,"항해마다 선체가 소모됩니다. 25 미만이면 수리가 필요해요.",13,MUTED);
        fullAction(ship,"선체 수리 · "+game.repairCost()+" G",()->changed(game.repair()),false);
        String upgrade=game.level==3?"최고 등급 선박":Engine.SHIPS[game.level]+" 구입 · "+game.upgradeCost()+" G";
        fullAction(ship,upgrade,()->changed(game.upgrade(),"upgrade"),true);
        LinearLayout ally=card(content); label(ally,Engine.PORTS[game.port]+" 교역 동맹",18,GOLD);
        label(ally,"동맹 "+game.influence[game.port]+" / 3 · 단계마다 판매가 +2%, 명성 +3",13,INK);
        String invest=game.influence[game.port]==3?"동맹 완성":"교역소 투자 · "+((game.influence[game.port]+1)*300)+" G";
        fullAction(ally,invest,()->changed(game.invest()),false);
        LinearLayout work=card(content); label(work,"항구 일거리",18,TEAL);
        label(work,"부두에서 하루 일하면 120 G를 받습니다. 보급비와 수리비가 부족할 때 이용하세요.",14,INK);
        fullAction(work,"하루 일하기 · +120 G",()->changed(game.work()),true);
    }
    private void journal() {
        label(content,"CAPTAIN'S JOURNAL",11,TEAL); label(content,"서린의 항해일지",26,INK);
        LinearLayout quest=card(content); label(quest,Story.TITLES[game.chapter],18,GOLD);
        label(quest,Story.GOALS[game.chapter],14,INK);
        goalShortcut(quest);audioSettings();
        for(int i=0;i<game.chapter;i++) { LinearLayout story=card(content); label(story,Story.REVEALS[i],14,INK); }
        LinearLayout entries=card(content); label(entries,"최근 항해 기록",17,TEAL);
        for(int i=game.log.size()-1;i>=0;i--) label(entries,game.log.get(i),13,INK);
        LinearLayout backup=card(content); label(backup,"자동 저장과 백업",18,GOLD);
        label(backup,"거래와 항해 후 기기에 자동 저장됩니다. 앱 삭제 전에는 파일로 백업하세요. 불러오면 현재 진행을 교체합니다.",13,INK);
        fullAction(backup,"저장 파일 내보내기",()->exportSave(),true);
        fullAction(backup,"저장 파일 불러오기",()->importSave(),false);
        fullAction(backup,"플레이 방법",()->dialog("선장 안내서",help()),false);
        fullAction(backup,"새 항해 시작",()->newGame(),false);
    }
    private String help() {
        return "1. 리스본에서 목재 5개를 매입하세요.\n2. 항해 지도에서 세비야를 선택해 출항하세요.\n3. 도착 후 목재 5개를 한 번에 판매하면 첫 장이 열립니다.\n\n세계 지도와 위쪽 해역 버튼을 이용해 7해역·56개 항구를 탐험하세요. 해역 버튼은 좌우로 밀 수 있습니다.\n\n먼 항해는 해상 경유 항구를 거치며 보급비가 증가합니다. 경유만으로는 방문 기록이 남지 않으니 목표 항구에 직접 정박하세요.\n\n항해일지의 8장 목표를 따라가세요. 금화가 부족하면 조선소에서 항구 일거리로 보급·수리비를 마련할 수 있습니다.\n\n원작과 별개의 창작 이야기입니다. 전투·해적·보물 탐색은 아직 포함되지 않았습니다.\n\n지도: Natural Earth (공개 지도 데이터). 항구 좌표와 항로는 게임용 근사치입니다.";
    }
    private void navigation() {
        status=text(saved?"● 기기에 자동 저장됨 · 오프라인":"저장 실패 · 백업 필요",11,saved?TEAL:GOLD);
        status.setPadding(0,dp(5),0,dp(3)); root.addView(status);
        LinearLayout nav=row(); root.addView(nav);
        String[] names={"항해","교역소","선박","동료","일지"};
        for(int i=0;i<5;i++) { final int index=i; action(nav,names[i],()->{tab=index;render();},tab==i); }
    }
    private void newGame() {
        new AlertDialog.Builder(this).setTitle("새 항해를 시작할까요?")
            .setMessage("현재 진행이 초기화됩니다. 필요하면 먼저 저장 파일을 내보내세요.")
            .setPositiveButton("새로 시작",(d,w)->{
                game=new Engine(); tab=0;mapRegion=-1; prefs.edit().remove("backup").remove("save").remove("ending").commit();
                save();render();dialog("새로운 항해",Story.introduction());
            }).setNegativeButton("취소",null).show();
    }
    private void exportSave() {
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE,"BlueHorizon-day"+game.day+".blue.txt");
        startActivityForResult(intent,10);
    }
    private void importSave() {
        new AlertDialog.Builder(this).setTitle("저장 파일 불러오기")
            .setMessage("불러오면 현재 진행을 교체합니다. 먼저 내보내기로 백업해 두세요.")
            .setPositiveButton("파일 선택",(d,w)->{
                Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("text/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,11);
            }).setNegativeButton("취소",null).show();
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
        game=restored;tab=0;mapRegion=-1;save();render();
        String summary=game.day+"일째 항해를 복원했습니다.";
        if(game.message.startsWith("세계 확장 버전")) summary+="\n\n"+game.message;
        dialog("불러오기 완료",summary);
    }
    private void dialog(String title,String body) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("확인",null).show();
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
        box.setBackground(background(PANEL,0xff2b4655));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(10),0,dp(2));
        parent.addView(box,params);return box;
    }
    private Button button(String value,boolean active) {
        Button button=new Button(this);button.setText(value);button.setTextSize(13);button.setAllCaps(false);
        button.setSoundEffectsEnabled(false);
        button.setTextColor(active?NAVY:INK);button.setMinHeight(dp(48));button.setMinimumWidth(0);
        button.setPadding(dp(6),dp(6),dp(6),dp(6));
        button.setBackground(background(active?GOLD:0xff234151,active?GOLD:0xff3c5b68));
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
