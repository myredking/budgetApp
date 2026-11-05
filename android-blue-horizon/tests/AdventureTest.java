package com.bluehorizon;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class AdventureTest {
    private static int checks;
    private static void check(boolean value,String label) { checks++;if(!value) throw new AssertionError(label); }
    private static Engine encounter(int kind,int roll) {
        Engine e=new Engine();e.inventory[0]=10;
        e.adventure.pending=kind;e.adventure.roll=roll;e.adventure.voyages=2;
        return e;
    }
    private static void choicesAndPersistence() {
        for(int kind=0;kind<8;kind++) {
            for(int choice=0;choice<3;choice++) {
                for(int roll:new int[]{10,90}) {
                    Engine e=encounter(kind,roll);String before=e.encode();
                    Engine resumed=Engine.decode(before);
                    check(resumed!=null && resumed.adventure.pending==kind,"pending event survives reload");
                    check(resumed.adventure.roll==roll,"outcome roll cannot change on reload");
                    check(VoyageEvents.choose(e,choice),"all 24 event choices playable");
                    check(VoyageEvents.choose(resumed,choice),"resume same choice");
                    check(resumed.encode().equals(e.encode()),"reloaded outcome deterministic");
                    check(e.gold>=0 && e.hull>=1 && e.cargo()<=e.capacity(),"event resource invariants");
                    check(Engine.decode(e.encode())!=null,"result state valid");
                }
            }
        }
        Engine e=encounter(5,40);e.gold=0;
        check(!VoyageEvents.choose(e,0) && e.adventure.pending==5,"unaffordable choice does not resolve event");
        check(VoyageEvents.choose(e,2) && e.adventure.pending==-1,"free alternative prevents softlock");
        int gold=e.gold;check(!VoyageEvents.choose(e,2) && e.gold==gold,"event rewards cannot be claimed twice");
    }
    private static void piracy() {
        Engine e=encounter(1,20);e.level=3;e.hull=e.maxHull();int gold=e.gold;
        check(VoyageEvents.choose(e,0) && e.adventure.battleHP>0,"choose turn-based battle");
        int turns=0;
        while(e.adventure.pending>=0 && turns++<15) check(VoyageEvents.choose(e,0),"cannon turn");
        check(e.adventure.pending==-1 && e.adventure.battles==1 && e.gold>gold,"battle win gives one reward");
        e=encounter(1,20);VoyageEvents.choose(e,0);
        check(VoyageEvents.choose(e,2) && e.adventure.pending==-1,"battle escape always available");
        e=encounter(1,20);e.hull=1;VoyageEvents.choose(e,0);VoyageEvents.choose(e,0);
        check(e.hull>=1 && e.adventure.pending==-1,"defeat cannot strand player in dead state");
    }
    private static void departAndLocks() {
        Engine e=new Engine();
        check(Adventure.sail(e,1,1) && e.adventure.pending==-1,"first tutorial trip quiet");
        check(Adventure.sail(e,0,1) && e.adventure.pending>=0,"second journey has event");
        check(!e.buy(0,1) && !e.work() && !e.sail(1,1),"pending event locks ordinary actions");
        check(!e.repair() && !e.invest() && !e.upgrade(),"cannot bypass pending encounter");
        check(!VoyageEvents.choose(e,7),"invalid choice rejected");
        check(Engine.decode(e.encode())!=null,"locked state reloadable");
    }
    private static void contractsAndAwards() {
        Engine e=new Engine();Contract offer=Contracts.offer(e);
        check(Contracts.accept(e),"accept local cargo contract");
        Engine restored=Engine.decode(e.encode());
        check(restored!=null && restored.adventure.target==offer.target && restored.adventure.reward==offer.reward,"active contract survives save");
        check(!Contracts.accept(e),"only one active contract");
        check(e.buy(offer.good,offer.amount) && e.sail(offer.target,1),"acquire and transport required cargo");
        int gold=e.gold;
        check(Contracts.deliver(e) && e.gold==gold+offer.reward,"complete contract pays quoted reward");
        check(e.adventure.deliveries==1 && !Contracts.deliver(e),"contract delivery once only");
        check(e.cargo()==0,"delivered cargo consumed");
        check(Contracts.accept(e),"another contract available");
        e.day=e.adventure.deadline+1;
        check(!Contracts.deliver(e) && Contracts.cancel(e),"expired contract can be canceled");
        e.profit=100;gold=e.gold;
        check(Achievements.claim(e,0) && e.gold>gold,"earned achievement pays once");
        check(!Achievements.claim(e,0),"achievement cannot be farmed");
        check(!Achievements.claim(e,6),"all-port reward stays locked");
    }
    private static void newSaveAndLegacy() throws Exception {
        Engine e=encounter(1,30);VoyageEvents.choose(e,0);Contracts.offer(e);
        check(e.encode().startsWith(SaveCodec.PREFIX),"new format includes adventure state");
        Engine resumed=Engine.decode(e.encode());
        check(resumed!=null && resumed.adventure.battleHP==e.adventure.battleHP,"mid-battle state restored");
        String fixture=new String(Files.readAllBytes(Paths.get("tests/fixtures/v2-save.txt")),StandardCharsets.US_ASCII);
        resumed=Engine.decode(fixture);
        check(resumed!=null && resumed.chapter==5 && resumed.gold==8000,"world save preserves story and funds");
        check(resumed.hasVisited(53) && resumed.inventory[4]==5,"world migration keeps distant ports and cargo");
        check(resumed.adventure.pending==-1 && resumed.adventure.voyages==0,"old save starts clean adventure state");
        e.adventure.pending=8;check(Engine.decode(e.encode())==null,"invalid encounter kind rejected");
        e=encounter(2,120);check(Engine.decode(e.encode())==null,"invalid outcome roll rejected");
        e=encounter(2,20);e.adventure.battleHP=50;
        check(Engine.decode(e.encode())==null,"non-pirate battle state rejected");
    }
    private static void randomAdventure() {
        Engine e=new Engine();java.util.Random random=new java.util.Random(477);
        for(int step=0;step<1500;step++) {
            if(e.adventure.pending>=0) VoyageEvents.choose(e,random.nextInt(3));
            else randomAction(e,random);
            check(Engine.decode(e.encode())!=null,"adventure random state can reload at "+step);
            check(e.gold>=0 && e.hull>=1 && e.cargo()<=e.capacity(),"adventure invariant "+step);
        }
    }
    private static void randomAction(Engine e,java.util.Random random) {
        switch(random.nextInt(8)) {
            case 0: Adventure.sail(e,random.nextInt(World.COUNT),random.nextInt(2));break;
            case 1: e.buy(random.nextInt(6),5);break;
            case 2: e.sell(random.nextInt(6),5);break;
            case 3: e.repair();break;
            case 4: Contracts.accept(e);break;
            case 5: Contracts.deliver(e);break;
            case 6: Achievements.claim(e,random.nextInt(8));break;
            default:e.work();
        }
    }
    private static void journey(Engine e,int port) {
        while(e.gold<e.travelCost(port,1)+e.repairCost()+1500) e.work();
        if(e.hull<e.maxHull()) check(e.repair(),"campaign ship upkeep");
        check(Adventure.sail(e,port,1),"campaign actual voyage wrapper");
        if(e.adventure.pending>=0) check(VoyageEvents.choose(e,2),"campaign free/safe encounter choice");
        check(e.adventure.pending==-1,"campaign encounter completed");
    }
    private static void campaignWithEvents() {
        Engine e=new Engine();
        check(e.buy(0,5),"first story cargo");journey(e,1);check(e.sell(0,5),"first story delivery");
        journey(e,2);journey(e,3);
        reputation(e,10,2,3);
        check(e.chapter==2,"northern chapter with adventure events");
        journey(e,13);check(e.invest(),"library investment");
        while(e.gold<2400) e.work();check(e.upgrade(),"story ship upgrade");
        journey(e,21);check(e.chapter==4,"Cape reached with encounters enabled");
        journey(e,28);check(e.buy(3,5),"story spices");journey(e,32);check(e.sell(3,5),"spice delivery");
        journey(e,42);check(e.buy(4,5),"story tea");journey(e,44);check(e.sell(4,5),"tea delivery");
        journey(e,48);journey(e,53);
        reputation(e,40,48,53);
        while(e.gold<e.travelCost(0,1)+12000) e.work();journey(e,0);
        check(e.chapter==8 && e.adventure.resolved>0,"full campaign playable with events");
        check(Engine.decode(e.encode())!=null,"finished adventure campaign reloads");
    }
    private static void reputation(Engine e,int fame,int a,int b) {
        while(e.fame<fame) {
            while(e.gold<1000) e.work();
            e.invest();if(e.influence[e.port]==3) journey(e,e.port==a?b:a);
        }
    }
    public static void main(String[] args) throws Exception {
        choicesAndPersistence();piracy();departAndLocks();contractsAndAwards();newSaveAndLegacy();randomAdventure();campaignWithEvents();
        System.out.println("PASS: "+checks+" adventure checks");
    }
}
