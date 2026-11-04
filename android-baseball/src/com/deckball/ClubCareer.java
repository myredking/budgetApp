package com.deckball;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;

public final class ClubCareer {
    public static final int SCOUT_FEE=500;
    final GameEngine game;
    public boolean active;
    public int day=1,funds=25000;
    final Set<Integer> signed=new LinkedHashSet<>();
    final Map<Integer,Integer> reports=new LinkedHashMap<>();
    public ClubCareer(GameEngine game) {
        this.game=game;
        for(int i=0;i<9;i++) signed.add(i);
        signed.add(27);
    }
    public void activate() {
        if(active) return;
        active=true;
        for(int i=0;i<9;i++) game.lineup[i]=i;
        game.pitcher=27;
    }
    public boolean owns(int id) { return signed.contains(id); }
    public boolean hasReport(int id) { return reports.containsKey(id); }
    public boolean ready(int id) { return hasReport(id) && day>=reports.get(id); }
    public int price(int id) {
        GameEngine.Player p=game.player(id);
        int rating=p.position.equals("P") ? p.control : (p.contact+p.power)/2;
        return 1500+rating*60+(p.legend?2500:0);
    }
    public void scout(int id) {
        game.player(id);
        if(!active) throw new IllegalArgumentException("구단 운영 모드를 시작하세요");
        if(owns(id) || hasReport(id)) throw new IllegalArgumentException("이미 보유 또는 조사 중인 선수입니다");
        if(funds<SCOUT_FEE) throw new IllegalArgumentException("조사 예산이 부족합니다");
        funds-=SCOUT_FEE; reports.put(id,day+2);
    }
    public void advanceDay() { if(active) day++; }
    public void sign(int id) {
        if(!active || owns(id) || !ready(id)) throw new IllegalArgumentException("완료된 조사 보고서가 필요합니다");
        int cost=price(id);
        if(funds<cost) throw new IllegalArgumentException("영입 예산이 부족합니다");
        funds-=cost; signed.add(id);
    }
    public void matchReward(boolean won) {
        if(!active) return;
        funds+=won ? 2000 : 800; day++;
    }
    public String report(int id) {
        GameEngine.Player p=game.player(id);
        int spread=ready(id) ? 4 : 15;
        String phase=ready(id) ? "조사 완료 · 추천/영입 가능" : "조사 중 · "+Math.max(0,reports.getOrDefault(id,day+2)-day)+"일 남음";
        return p.name+" · "+p.team+" · "+p.year+"\n"+phase+"\n컨택 "+range(p.contact,spread)+
            " / 파워 "+range(p.power,spread)+" / 제구 "+range(p.control,spread)+"\n예상 영입비 "+price(id)+"만원\n"+
            (ready(id)?p.record+"\n"+PlayerSkills.describe(p):"특수 능력: 조사 완료 후 공개")+
            "\n보고서는 게임용 평가 범위이며 실제 스카우트 평가가 아닙니다.";
    }
    private String range(int value,int spread) { return Math.max(1,value-spread)+"~"+Math.min(99,value+spread); }
}
