package com.bluehorizon;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public final class RivalTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    private static void waitForContact(Engine e,int rival) { while(e.day-e.rivals.lastNegotiation[rival]<7) e.work(); }
    private static void diplomacy() {
        Engine e=new Engine();e.fame=15;
        check(Rivals.negotiate(e,0,0) && e.rivals.relations[0]==12,"gift raises relationship");
        int gold=e.gold;
        check(!Rivals.negotiate(e,0,0) && e.gold==gold,"contact cooldown prevents repeat spending");
        check(!Rivals.formAlliance(e,0),"alliance relationship threshold");
        for(int i=0;i<3;i++) { waitForContact(e,0);check(Rivals.negotiate(e,0,0),"build diplomatic trust"); }
        check(Rivals.formAlliance(e,0) && Rivals.allied(e,0),"form alliance");
        gold=e.gold;check(!Rivals.formAlliance(e,0) && e.gold==gold,"alliance charged once");
        check(Rivals.endAlliance(e,0) && !Rivals.allied(e,0),"end alliance voluntarily");
        e=new Engine();e.adventure.deliveries=1;
        check(Rivals.negotiate(e,0,1) && e.rivals.letters[0]==1,"completed cargo contract supports letter");
        waitForContact(e,0);check(!Rivals.negotiate(e,0,1),"same delivery cannot fund endless diplomatic letters");
        e.adventure.pending=0;
        check(!Rivals.negotiate(e,1,0) && !TradeRace.start(e),"pending event locks diplomacy and race");
    }
    private static void marketCompetition() {
        Engine e=new Engine();int base=e.buyPrice(5);e.rivals.pressure[0]=70;
        check(e.buyPrice(5)>base,"strong rival applies published buying surcharge");
        int pressure=e.rivals.pressure[0];e.day+=21;Rivals.advance(e);
        check(e.rivals.pressure[0]>pressure,"rivals advance with game time");
        pressure=e.rivals.pressure[0];Rivals.onSale(e,10);
        check(e.rivals.pressure[0]<pressure,"trade contests regional influence");
        e.rivals.allies=1;e.rivals.relations[0]=50;
        check(Rivals.buyFee(e,0)==0 && Rivals.saleBonus(e,0)>=2,"alliance trade benefits");
        e.chapter=5;e.crew.second=5;e.rivals.allies=7;
        for(int port=0;port<World.COUNT;port++) {
            e.port=port;e.visited|=1L<<port;e.influence[port]=3;e.rivals.pressure[World.region(port)]=0;
            for(int good=0;good<6;good++) check(e.sellPrice(good)<e.buyPrice(good),"all bonuses preserve a positive same-port spread");
        }
    }
    private static void races() {
        Engine e=new Engine();e.gold=6000;Contract offer=TradeRace.offer(e);
        check(e.buy(offer.good,offer.amount) && TradeRace.start(e),"prepare and enter trade race");
        check(!TradeRace.start(e),"only one race active");
        check(e.sail(offer.target,1),"race voyage");int gold=e.gold;
        check(TradeRace.complete(e) && e.gold==gold+offer.reward,"win race pays quoted reward");
        check(e.rivals.wins[0]==1 && e.cargo()==0,"race records win and consumes shipment");
        check(!TradeRace.complete(e),"race reward cannot repeat");
        e=new Engine();check(TradeRace.start(e) && TradeRace.abandon(e),"race may be forfeited");
        check(!TradeRace.start(e),"seven-day race entry cooldown");
        e=new Engine();offer=TradeRace.offer(e);e.buy(offer.good,offer.amount);TradeRace.start(e);
        gold=e.gold;e.day=e.rivals.raceDeadline+1;
        check(TradeRace.complete(e) && e.rivals.wins[0]==0,"expired race settles as loss");
        check(e.gold==gold && e.cargo()==offer.amount,"loss keeps cargo and never refunds entry fee");
    }
    private static void serialization() throws Exception {
        Engine e=new Engine();TradeRace.start(e);Rivals.negotiate(e,0,0);
        Engine restored=Engine.decode(e.encode());
        check(restored!=null && restored.rivals.raceTarget==e.rivals.raceTarget,"active race reloads");
        check(restored.rivals.relations[0]==12 && restored.encode().equals(e.encode()),"diplomacy exact roundtrip");
        e.rivals.relations[0]=101;check(Engine.decode(e.encode())==null,"invalid relationship rejected");
        e=new Engine();e.rivals.lastNegotiation[0]=1000;
        check(Engine.decode(e.encode())==null,"future cooldown date rejected");
        String old=new String(Files.readAllBytes(Paths.get("tests/fixtures/v5-save.txt")),StandardCharsets.US_ASCII);
        restored=Engine.decode(old);
        check(restored!=null && restored.refits.levels[0]==1 && restored.crew.first==2,"legacy refits and crew retained");
        check(restored.adventure.battleHP==38 && restored.rivals.raceTarget==-1,"legacy battle retained with clean rival system");
        check(restored.rivals.lastDay==restored.day,"legacy time does not simulate rivals retroactively");
    }
    private static void randomRivalActions() {
        Engine e=new Engine();e.chapter=5;e.gold=20000;java.util.Random rng=new java.util.Random(596);
        for(int step=0;step<1200;step++) {
            if(e.adventure.pending>=0) VoyageEvents.choose(e,2);else randomAction(e,rng);
            check(Engine.decode(e.encode())!=null,"random rival state reloads "+step);
            check(e.gold>=0 && e.cargo()<=e.capacity(),"random rival invariants "+step);
        }
    }
    private static void randomAction(Engine e,java.util.Random rng) {
        switch(rng.nextInt(8)) {
            case 0:e.work();break;
            case 1:e.buy(rng.nextInt(6),5);break;
            case 2:e.sell(rng.nextInt(6),5);break;
            case 3:Adventure.sail(e,rng.nextInt(World.COUNT),rng.nextInt(2));break;
            case 4:Rivals.negotiate(e,rng.nextInt(3),rng.nextInt(3));break;
            case 5:Rivals.formAlliance(e,rng.nextInt(3));break;
            case 6:TradeRace.start(e);break;
            default:TradeRace.complete(e);
        }
    }
    private static void combinedSystems() {
        Engine e=new Engine();e.chapter=5;e.fame=20;e.gold=30000;e.rivals.relations[0]=50;
        check(CrewAbilities.assign(e,0,2) && CrewAbilities.assign(e,1,5),"navigation and merchant specialists assigned");
        check(Refits.install(e,0) && Refits.install(e,2) && Refits.install(e,3),"custom expedition-trader build");
        check(Rivals.formAlliance(e,0),"trade alliance alongside specialists and refits");
        Contract race=TradeRace.offer(e);
        check(e.buy(race.good,race.amount) && TradeRace.start(e),"joint-system race prepared");
        check(Adventure.sail(e,race.target,1),"joint-system actual voyage");
        if(e.adventure.pending>=0) check(VoyageEvents.choose(e,2),"race arrival encounter");
        check(TradeRace.complete(e),"custom build completes race within original-hull deadline");
        Engine saved=Engine.decode(e.encode());
        check(saved!=null && saved.crew.first==2 && saved.refits.used()==3,"combined crew/refit state restored");
        check(Rivals.allied(saved,0) && saved.rivals.wins[0]==1,"combined alliance/race state restored");
    }
    private static void crossAssignmentSpread() {
        Engine e=new Engine();e.chapter=5;e.crew.first=2;e.crew.second=5;e.rivals.allies=7;
        for(int port=0;port<World.COUNT;port++) {
            e.influence[port]=3;e.rivals.pressure[World.region(port)]=0;
            for(int good=0;good<6;good++) {
                int cheapest=World.price(port,good)*95/100;
                check(e.sellPriceAt(port,good)<cheapest,"cannot buy with Ian then swap specialists for cost-free resale");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        diplomacy();marketCompetition();races();serialization();randomRivalActions();combinedSystems();crossAssignmentSpread();System.out.println("PASS: "+checks+" rival guild checks");
    }
}
