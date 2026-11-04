package com.deckball;

import android.app.AlertDialog;
import android.widget.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class ClubScreens {
    final MainActivity app;
    String team="전체";
    int role;
    public ClubScreens(MainActivity app) { this.app=app; }
    void save() { app.getPreferences(0).edit().putString(CareerSave.KEY,CareerSave.capture(app.engine.career)).apply(); }
    public void show() {
        ClubCareer club=app.engine.career;
        app.screen("CLUB OFFICE · 구단 운영","스카우트 조사 → 보고서 → 계약 → 라인업 편성");
        app.root.addView(app.text("DAY "+club.day+"  ·  예산 "+club.funds+"만원  ·  보유 "+club.signed.size()+"명",17));
        actions(club);
        ScrollView scroll=new ScrollView(app); LinearLayout list=app.column(); scroll.addView(list);
        app.root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        list.addView(app.text("조사비 500만원 / 2일 소요 · 완료 후 평가 범위와 특수 능력 공개",13));
        for(GameEngine.Player p:app.engine.roster) {
            if(!matches(p)) continue;
            String state=club.owns(p.id)?"보유":(club.ready(p.id)?"보고서 완료":(club.hasReport(p.id)?"조사 중":"미조사"));
            list.addView(app.button(p.name+" · "+p.team+" "+p.year+" · "+p.position+"  ["+state+"]",()->report(p.id)));
        }
        app.addButton("홈으로",app::home);
    }
    void actions(ClubCareer club) {
        LinearLayout row=new LinearLayout(app);
        row.addView(app.button(club.active?"다음 날":"운영 모드 시작",()->{
            if(club.active) club.advanceDay(); else club.activate();
            save(); app.persist(); show();
        }),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(app.button("구단: "+team,this::teams),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(app.button(new String[]{"전체 선수","타자","투수","역대 선수"}[role],()->{role=(role+1)%4;show();}),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(app.button("자유 편성 모드",()->{club.active=false;save();app.home();}),new LinearLayout.LayoutParams(0,-2,1));
        app.root.addView(row);
    }
    boolean matches(GameEngine.Player p) {
        if(!team.equals("전체") && !team.equals(p.team)) return false;
        if(role==1) return !p.position.equals("P");
        if(role==2) return p.position.equals("P");
        return role!=3 || p.legend;
    }
    void teams() {
        LinkedHashSet<String> values=new LinkedHashSet<>();values.add("전체");
        for(GameEngine.Player p:app.engine.roster) values.add(p.team);
        String[] labels=values.toArray(new String[0]);
        new AlertDialog.Builder(app).setTitle("기록 시즌 소속 구단")
            .setItems(labels,(d,w)->{team=labels[w];show();}).show();
    }
    void report(int id) {
        ClubCareer club=app.engine.career;
        AlertDialog.Builder builder=new AlertDialog.Builder(app).setTitle("스카우트 보고서").setMessage(club.report(id))
            .setNegativeButton("닫기",null);
        if(!club.owns(id) && !club.hasReport(id)) builder.setPositiveButton("조사 의뢰 · 500만원",(d,w)->act(id,false));
        if(!club.owns(id) && club.ready(id)) builder.setPositiveButton("영입 · "+club.price(id)+"만원",(d,w)->act(id,true));
        builder.show();
    }
    void act(int id,boolean sign) {
        try {
            if(sign) app.engine.career.sign(id); else app.engine.career.scout(id);
            save(); show();
        } catch(IllegalArgumentException e) { app.toast(e.getMessage()); }
    }
    public void skills(int id) {
        GameEngine.Player p=app.engine.player(id);
        List<Integer> values=new ArrayList<>(); List<String> names=new ArrayList<>();
        for(int i=0;i<PlayerSkills.VALUES.length;i++) {
            try { PlayerSkills.validate(p,PlayerSkills.VALUES[i]); }
            catch(IllegalArgumentException e) { continue; }
            values.add(PlayerSkills.VALUES[i]);names.add(PlayerSkills.NAMES[i]);
        }
        boolean[] selected=new boolean[values.size()];
        for(int i=0;i<selected.length;i++) selected[i]=(p.skills & values.get(i))!=0;
        AlertDialog dialog=new AlertDialog.Builder(app).setTitle(p.name+" · 게임용 특수 능력 최대 2개")
            .setMultiChoiceItems(names.toArray(new String[0]),selected,(d,w,checked)->selected[w]=checked)
            .setPositiveButton("저장",null).setNegativeButton("취소",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{
            int mask=0;for(int i=0;i<selected.length;i++) if(selected[i]) mask|=values.get(i);
            try {app.engine.assignSkills(id,mask);app.saveRatings();dialog.dismiss();app.home();}
            catch(IllegalArgumentException e) {app.toast(e.getMessage());}
        }));dialog.show();
    }
}
