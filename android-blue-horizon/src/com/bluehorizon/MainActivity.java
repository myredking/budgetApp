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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    static final int NAVY=0xff101f30, PANEL=0xff1a3041, GOLD=0xffe4bf7b;
    static final int INK=0xffecede5, MUTED=0xff9eb7bd, TEAL=0xff65c9b5;
    private Engine game;
    private int tab=0, quantity=5;
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
        if(state!=null) { tab=state.getInt("tab",0); quantity=state.getInt("quantity",5); }
        save(); render();
        if(fresh) dialog("푸른 수평선", Story.introduction());
        if(recovered) dialog("저장 복원", "최근 저장을 읽을 수 없어 이전 자동 저장을 복원했습니다.");
    }
    @Override protected void onPause() { super.onPause(); if(game!=null) save(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putInt("tab",tab); out.putInt("quantity",quantity);
    }
    private void save() {
        String previous=prefs.getString("save",null);
        SharedPreferences.Editor edit=prefs.edit();
        if(Engine.decode(previous)!=null) edit.putString("backup",previous);
        saved=edit.putString("save",game.encode()).commit();
        if(!saved) Toast.makeText(this,"저장 실패: 저장 공간을 확인하고 백업해 주세요.",Toast.LENGTH_LONG).show();
    }
    private void changed(boolean success) {
        int previousChapter=shownChapter;
        save(); render();
        Toast.makeText(this,game.message,Toast.LENGTH_LONG).show();
        if(success && game.chapter==4 && previousChapter<4) {
            dialog("푸른 수평선 너머로",Story.REVEALS[3]+"\n\n이제 자유롭게 항해할 수 있습니다.");
        }
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
        if(tab==3) journal();
        navigation();
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
    private void harbor() {
        label(content,"THE AZURE ARCHIPELAGO",11,TEAL);
        label(content,Engine.PORTS[game.port]+" 항구",27,INK);
        label(content,"지도에서 항구를 누르면 항로를 정할 수 있어요.",12,MUTED);
        OceanView ocean=new OceanView(this,game, target -> voyage(target));
        content.addView(ocean,new LinearLayout.LayoutParams(-1,dp(300)));
        LinearLayout quest=card(content);
        label(quest,Story.TITLES[game.chapter],17,GOLD);
        label(quest,Story.GOALS[game.chapter],14,INK);
        LinearLayout actions=row(); content.addView(actions);
        action(actions,"교역소",()-> {tab=1;render();},true);
        action(actions,"조선소",()-> {tab=2;render();},false);
        LinearLayout ports=card(content); label(ports,"출항할 항구 선택",16,GOLD);
        for(int i=0;i<5;i++) addDestination(ports,i);
        label(content,game.message,13,MUTED);
    }
    private void addDestination(LinearLayout box,int target) {
        if(target==game.port) return;
        Button button=button(Engine.PORTS[target]+"  ·  "+game.travelDays(target)+"일  ·  "+game.travelCost(target,0)+" G부터",false);
        button.setOnClickListener(v->voyage(target)); box.addView(button,buttonParams());
    }
    private void voyage(int target) {
        if(target==game.port) { Toast.makeText(this,"현재 머무는 항구입니다.",Toast.LENGTH_SHORT).show(); return; }
        String info=Engine.PORTS[game.port]+" → "+Engine.PORTS[target]+"\n"+game.travelDays(target)+"일 항해\n\n일반 항로: 보급 "+game.travelCost(target,0)+" G, 선체 5~18 소모\n안전 항로: 보급 "+game.travelCost(target,1)+" G, 선체 2 소모\n\n선체 25 이상이면 출항할 수 있습니다.";
        new AlertDialog.Builder(this).setTitle("출항 준비").setMessage(info)
            .setPositiveButton("일반 항로",(d,w)->changed(game.sail(target,0)))
            .setNeutralButton("안전 항로",(d,w)->changed(game.sail(target,1)))
            .setNegativeButton("취소",null).show();
    }
    private void market() {
        label(content,"PORT MARKET",11,TEAL); label(content,Engine.PORTS[game.port]+" 교역소",26,INK);
        label(content,"가격은 1개 기준 · 같은 항구에서는 판매가가 더 낮아요.",12,MUTED);
        LinearLayout quantities=row(); content.addView(quantities);
        for(int amount:new int[]{1,5,10}) action(quantities,amount+"개씩",()->{quantity=amount;render();},quantity==amount);
        for(int good=0;good<6;good++) goodsCard(good);
        LinearLayout tip=card(content); label(tip,"초보 선장의 무역 수첩",16,GOLD);
        label(tip,"목재: 루미아 → 베르단\n직물: 베르단 → 사피라\n도자기: 사피라 → 나마르\n향신료: 나마르 → 하란\n차·은: 하란 → 루미아\n\n출항 보급비를 남겨 두세요. 금화가 부족하면 조선소의 항구 일거리로 다시 시작할 수 있어요.",14,INK);
    }
    private void goodsCard(int good) {
        LinearLayout box=card(content);
        label(box,Engine.GOODS[good]+"     보유 "+game.inventory[good]+"개",18,INK);
        label(box,"매입 "+game.buyPrice(good)+" G   /   판매 "+game.sellPrice(good)+" G",14,GOLD);
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
        fullAction(ship,upgrade,()->changed(game.upgrade()),true);
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
        return "1. 교역소에서 목재 5개를 매입하세요.\n2. 지도에서 베르단을 누르고 출항하세요.\n3. 도착 후 목재 5개를 한 번에 판매하면 첫 이야기가 열립니다.\n\n항해일지에 다음 목표가 표시됩니다. 판매와 투자로 명성을 높이고 조선소에서 배를 키우세요.\n\n초기에는 5개 단위 거래가 편리합니다. 작은 거래는 1개씩 선택할 수 있어요.\n\n가격은 항구마다 고정되어 있습니다. 보급비·수리비를 고려해 거래하세요.\n\n전투·해적·보물 탐색은 이번 시제품에 포함되지 않았습니다.";
    }
    private void navigation() {
        status=text(saved?"● 기기에 자동 저장됨 · 오프라인":"저장 실패 · 백업 필요",11,saved?TEAL:GOLD);
        status.setPadding(0,dp(5),0,dp(3)); root.addView(status);
        LinearLayout nav=row(); root.addView(nav);
        String[] names={"항해 지도","교역소","선박","일지"};
        for(int i=0;i<4;i++) { final int index=i; action(nav,names[i],()->{tab=index;render();},tab==i); }
    }
    private void newGame() {
        new AlertDialog.Builder(this).setTitle("새 항해를 시작할까요?")
            .setMessage("현재 진행이 초기화됩니다. 필요하면 먼저 저장 파일을 내보내세요.")
            .setPositiveButton("새로 시작",(d,w)->{
                game=new Engine(); tab=0; prefs.edit().remove("backup").remove("save").remove("ending").commit();
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
        game=restored;tab=0;save();render();dialog("불러오기 완료",game.day+"일째 항해를 복원했습니다.");
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
        button.setTextColor(active?NAVY:INK);button.setMinHeight(dp(48));button.setMinimumWidth(0);
        button.setPadding(dp(6),dp(6),dp(6),dp(6));
        button.setBackground(background(active?GOLD:0xff234151,active?GOLD:0xff3c5b68));
        return button;
    }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(dp(3),dp(5),dp(3),dp(3));return params;
    }
    private void action(LinearLayout parent,String value,Runnable run,boolean active) {
        Button button=button(value,active);button.setOnClickListener(v->run.run());
        LinearLayout.LayoutParams params=buttonParams();params.width=0;params.weight=1;parent.addView(button,params);
    }
    private void fullAction(LinearLayout parent,String value,Runnable run,boolean active) {
        Button button=button(value,active);button.setOnClickListener(v->run.run());parent.addView(button,buttonParams());
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
