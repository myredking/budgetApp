package com.bluehorizon;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Scripted, legal full run including real voyage events and save/resume. */
public final class CampaignAudit {
    private Engine game=new Engine();
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
    private void run() throws Exception {
        report.add("milestone,chapter,day,port,gold,port_work,voyages,events");
        for(int i=0;i<9;i++) circuit();money(game.upgradeCost());check(game.upgrade());record("upgrade");
        sail(2);sail(3);sail(13);check(game.invest());record("library");
        if(Crew.available(2,game.chapter)) check(CrewAbilities.assign(game,0,2));
        sail(21);record("cape");sail(28);check(game.buy(3,5));sail(32);check(game.sell(3,5));record("monsoon");
        sail(42);check(game.buy(4,5));sail(44);check(game.sell(4,5));record("east");
        sail(48);sail(53);record("atlantic");money(game.travelCost(0,1)+game.repairCost()+13500);sail(0);
        if(Story.ready(game)) game.advanceStory();
        check(game.chapter==8);reload();record("ending");
        Files.createDirectories(Paths.get("build"));Files.write(Paths.get("build/campaign-audit.csv"),report,StandardCharsets.UTF_8);
        System.out.println("CAMPAIGN: ending, "+jobs+" port work, "+legs+" voyages, "+events+" events, "+game.day+" game days");
    }
    public static void main(String[] args) throws Exception { new CampaignAudit().run(); }
}
