package com.bluehorizon;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Scripted, legal full run including real voyage events and save/resume. */
public final class CampaignAudit {
    private Engine game=new Engine();
    private final boolean exploration;
    private CampaignAudit(boolean explore) { exploration=explore; }
    private int jobs,legs,events;
    private final List<String> report=new ArrayList<String>();
    private void check(boolean success) { if(!success) throw new AssertionError(game.message); }
    private void reload() { Engine restored=Engine.decode(game.encode());check(restored!=null);game=restored; }
    private void money(int amount) {
        while(game.gold<amount) { check(game.work());jobs++;if(jobs>2000) throw new AssertionError("funding loop"); }
    }
    private void sail(int target) {
        if(game.town.morale<30) { money(60);check(TownRpg.rest(game)); }
        money(game.travelCost(target,1)+game.repairCost()+1500);
        if(game.hull<game.maxHull()) check(game.repair());
        check(TravelFlow.begin(game,target,1));reload();TravelFlow.arrive(game);legs++;
        if(game.adventure.pending>=0) {
            int choice=new int[]{0,2,2,2,1,2,2,2}[game.adventure.pending];
            check(VoyageEvents.choose(game,choice));events++;
        }
        reload();
    }
    private void circuit() {
        if(game.port!=0) sail(0);
        money(game.buyPrice(0)*20);check(game.buy(0,20));sail(1);check(game.sell(0,20));sail(0);
    }
    private void record(String milestone) {
        report.add(milestone+","+game.chapter+","+game.day+","+World.PORTS[game.port]+","+game.gold+","+jobs+","+legs+","+events);
    }
    private int nextClue(int region) {
        int best=-1,cost=Integer.MAX_VALUE;
        for(int port:World.portsIn(region)) {
            if((game.expedition.clues & (1L<<port))!=0) continue;
            int price=game.travelCost(port,1);if(price<cost) { cost=price;best=port; }
        }
        return best;
    }
    private void explore() {
        if(!exploration) return;
        int origin=game.port,region=World.region(origin);
        while(Expeditions.fragments(game,region)<3) {
            if((game.expedition.clues & (1L<<game.port))==0) { money(40);check(Expeditions.clue(game)); }
            if(Expeditions.fragments(game,region)<3) sail(nextClue(region));
        }
        if(game.port!=Expeditions.site(region)) sail(Expeditions.site(region));
        money(80);check(Expeditions.dig(game));
        fightBoss(region);record("treasure-and-boss-"+region);
        if(game.port!=origin) sail(origin);
    }
    private void fightBoss(int region) {
        while(game.level<1+region/3) { money(game.upgradeCost()+1500);check(game.upgrade()); }
        money(game.repairCost());if(game.hull<game.maxHull()) check(game.repair());
        check(RegionBoss.begin(game));
        while(game.expedition.active()) { check(RegionBoss.choose(game,RegionBoss.intent(game)==0?1:0));reload(); }
        check((game.expedition.trophies & (1<<region))!=0);
    }
    private void run() throws Exception {
        report.add("milestone,chapter,day,port,gold,port_work,voyages,events");
        for(int i=0;i<9;i++) circuit();money(game.upgradeCost());check(game.upgrade());record("upgrade");explore();
        sail(2);sail(3);sail(13);check(game.invest());record("library");explore();
        if(Crew.available(2,game.chapter)) check(CrewAbilities.assign(game,0,2));
        sail(21);record("cape");explore();sail(28);explore();check(game.buy(3,5));sail(32);check(game.sell(3,5));record("monsoon");explore();
        sail(42);check(game.buy(4,5));sail(44);check(game.sell(4,5));record("east");explore();
        sail(48);sail(53);record("atlantic");explore();money(FundingPlan.required(game)+game.repairCost());sail(0);
        if(Story.ready(game)) game.advanceStory();
        check(game.chapter==8);reload();record("ending");
        if(exploration) { check(game.expedition.treasures==127 && game.expedition.trophies==127);check(jobs<=30); }
        Files.createDirectories(Paths.get("build"));Files.write(Paths.get(exploration?"build/campaign-explorer.csv":"build/campaign-audit.csv"),report,StandardCharsets.UTF_8);
        System.out.println("CAMPAIGN "+(exploration?"explorer":"direct")+": ending, "+jobs+" port work, "+legs+" voyages, "+events+" events, "+game.day+" game days");
    }
    public static void main(String[] args) throws Exception { new CampaignAudit(args.length>0 && args[0].equals("explorer")).run(); }
}
