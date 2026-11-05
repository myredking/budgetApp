package com.bluehorizon;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public final class TownTest {
    private static int checks;
    private static void check(boolean value,String label) { checks++;if(!value) throw new AssertionError(label); }
    private static void scenesAndLocations() {
        check(TownLayout.townCell(1,887,1774)[0]>=518,"Mediterranean crop excludes adjacent first-row scene");
        float size=ShipMotion.spriteSize(360,500);
        check(size<=175,"compact sailing viewport keeps ship below route overlay");
        check(TownRpg.style(0)==1 && TownRpg.style(1)==1,"opening Iberian ports use warm Mediterranean scene");
        for(int port=0;port<World.COUNT;port++) check(TownRpg.style(port)>=0 && TownRpg.style(port)<7,"regional town art available");
        for(int scene=0;scene<8;scene++) {
            int[] r=TownLayout.cell(scene,887,1774);
            check(r[0]>=0 && r[2]<=887 && r[1]>=0 && r[3]<=1774,"odd-sized atlas crop in bounds");
        }
        for(int location=0;location<4;location++) {
            float[] p=TownLayout.point(location);
            check(TownLayout.hit(p[0],p[1])==location,"building label selects matching facility");
        }
        check(TownLayout.hit(.99f,.01f)==-1,"empty sky does not trigger a building");
    }
    private static void voyageTransaction() {
        Engine e=new Engine();int gold=e.gold,day=e.day,cost=e.travelCost(1,1);
        check(TravelFlow.begin(e,1,1),"begin cinematic voyage");
        check(e.port==1 && e.town.moving() && e.gold==gold-cost,"destination and cost committed once before animation");
        check(e.day>day && e.town.from==0,"departure and arrival days retained");
        check(!TravelFlow.begin(e,2,1) && !e.work() && !e.buy(0,1),"cannot act or charge twice while sailing");
        Engine loaded=Engine.decode(e.encode());
        check(loaded!=null && loaded.town.moving() && loaded.town.from==0,"in-transit animation intent restores");
        finishTransaction(loaded);
    }
    private static void finishTransaction(Engine loaded) {
        int gold=loaded.gold,day=loaded.day;long random=loaded.adventure.rng;
        check(TravelFlow.arrive(loaded)==0 && !loaded.town.moving(),"skip or completion enters town");
        check(TravelFlow.arrive(loaded)==-1,"arrival callback idempotent");
        check(loaded.gold==gold && loaded.day==day && loaded.adventure.rng==random,"resume/skip cannot re-charge or reroll");
        check(TravelFlow.begin(loaded,0,1),"second voyage with pending encounter");
        check(loaded.adventure.pending>=0 && !VoyageEvents.choose(loaded,2),"encounter waits until sailing scene ends");
        TravelFlow.arrive(loaded);check(VoyageEvents.choose(loaded,2),"arrival continues saved encounter");
    }
    private static void tavernAndRank() {
        Engine e=new Engine();e.town.morale=20;int cost=e.travelCost(28,0),day=e.day,gold=e.gold;
        check(TownRpg.rest(e),"rest in tavern");
        check(e.town.morale==45 && e.gold==gold-60 && e.day==day+1,"rest consumes money/time and restores morale");
        check(e.travelCost(28,0)<cost,"low morale supply penalty clears after rest");
        e.town.morale=100;gold=e.gold;
        check(!TownRpg.rest(e) && e.gold==gold,"full morale cannot consume rest fee");
        check(TownRpg.attackBonus(e)==2,"high morale combat bonus");
        int fame=e.fame,xp=TownRpg.experience(e);
        check(TownRpg.hearRumor(e) && e.fame==fame+1,"first local rumor earns one fame");
        fame=e.fame;TownRpg.hearRumor(e);
        check(e.fame==fame && TownRpg.experience(e)>xp,"repeat dialogue cannot farm fame");
        check(TownRpg.rank(e)>=1 && TownRpg.rumor(e).contains("항구"),"town rank and actionable rumor");
        check(Engine.decode(e.encode())!=null,"tavern progress saves");
    }
    private static void routeMotion() {
        for(int[] route:new int[][]{{0,1},{0,44},{55,5}}) {
            ShipMotion motion=new ShipMotion(route[0],route[1]);
            float[] start=motion.position(0),end=motion.position(1);
            check(Math.abs(start[0]-World.LON[route[0]])<.01,"ship starts at departure harbor");
            check(Math.abs(end[1]-World.LAT[route[1]])<.01,"ship ends at target harbor");
            for(int step=0;step<=100;step++) {
                float[] point=motion.position(step/100.0);
                check(point[0]>=-180 && point[0]<=180 && point[1]>=-90 && point[1]<=90,"animation stays on world route");
            }
        }
    }
    private static void legacyAndMalformed() throws Exception {
        String old=new String(Files.readAllBytes(Paths.get("tests/fixtures/v6-save.txt")),StandardCharsets.US_ASCII);
        Engine e=Engine.decode(old);
        check(e!=null && e.adventure.battleHP==38 && e.rivals.raceTarget==4,"old battle and race retained");
        check(e.crew.first==2 && e.refits.used()==2 && e.town.morale==70,"old crew/refits retained with town defaults");
        e.town.morale=101;check(Engine.decode(e.encode())==null,"invalid morale rejected");
        e=new Engine();e.town.from=0;e.town.startDay=1;e.town.previousChapter=0;
        check(Engine.decode(e.encode())==null,"journey cannot depart and arrive at same port");
        e=new Engine();e.town.rumors=1L<<60;
        check(Engine.decode(e.encode())==null,"unknown rumor bits rejected");
    }
    public static void main(String[] args) throws Exception {
        scenesAndLocations();voyageTransaction();tavernAndRank();routeMotion();legacyAndMalformed();
        System.out.println("PASS: "+checks+" town and sailing checks");
    }
}
