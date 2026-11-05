package com.bluehorizon;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public final class CrewAbilitiesTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    private static void assignmentAndTrade() {
        Engine e=new Engine();
        check(e.buyPrice(0)==World.price(0,0)*95/100,"Ian initially reduces buying price");
        check(!CrewAbilities.assign(e,0,2),"locked specialist rejected");
        check(!CrewAbilities.assign(e,1,1),"duplicate specialist rejected");
        e.chapter=5;
        check(CrewAbilities.assign(e,1,5),"assign unlocked Ayu");
        int expected=Math.min(94,85+5);
        check(e.sellPrice(0)==World.price(e.port,0)*expected/100,"Ayu improves sale proceeds");
        for(int port=0;port<World.COUNT;port++) {
            e.port=port;e.visited|=1L<<port;e.influence[port]=3;
            for(int good=0;good<6;good++) check(e.sellPrice(good)<=e.buyPrice(good),"crew effects cannot create same-port arbitrage");
        }
        e.adventure.pending=0;
        check(!CrewAbilities.assign(e,0,2),"cannot swap crew in a pending event");
    }
    private static void travelAndRisk() {
        Engine e=new Engine();e.chapter=5;
        int cost=e.travelCost(28,0),fame=e.fame;
        check(CrewAbilities.assign(e,1,2),"assign Mira");
        check(e.travelCost(28,0)<cost,"Mira reduces long trip supply cost");
        check(e.sail(1,1) && e.fame==fame+1,"captain earns first-visit fame");
        e.sail(0,1);fame=e.fame;e.sail(1,1);
        check(e.fame==fame,"repeat arrivals do not farm captain bonus");
        CrewAbilities.assign(e,1,4);
        check(e.travelDamage(2,1)==1,"Thabo reduces ordinary travel damage to minimum one");
        e=new Engine();e.chapter=3;CrewAbilities.assign(e,1,3);
        e.adventure.pending=2;e.adventure.roll=65;e.adventure.voyages=2;
        check(VoyageEvents.choose(e,1) && e.adventure.souvenirs!=0,"Nadia improves wreck success threshold");
    }
    private static void recordsAndSaves() throws Exception {
        Engine e=new Engine();e.profit=5000;int gold=e.gold;
        check(CrewAbilities.claimStory(e,1) && e.gold>gold,"personal story pays once");
        check(!CrewAbilities.claimStory(e,1),"cannot repeat diary reward");
        e.chapter=5;CrewAbilities.assign(e,1,5);
        Engine restored=Engine.decode(e.encode());
        check(restored!=null && restored.crew.second==5 && restored.crew.stories==e.crew.stories,"crew state restored");
        e.crew.second=e.crew.first;
        check(Engine.decode(e.encode())==null,"duplicate crew in save rejected");
        String old=new String(Files.readAllBytes(Paths.get("tests/fixtures/v3-save.txt")),StandardCharsets.US_ASCII);
        restored=Engine.decode(old);
        check(restored!=null && restored.chapter==5 && restored.adventure.battleHP==38,"legacy adventure battle retained");
        check(restored.adventure.target==3 && restored.crew.first==1,"legacy contract retained with default specialist");
    }
    public static void main(String[] args) throws Exception {
        assignmentAndTrade();travelAndRisk();recordsAndSaves();System.out.println("PASS: "+checks+" crew ability checks");
    }
}
