package com.bluehorizon;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class ExpeditionTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    private static void port(Engine e,int port) { e.port=port;e.visited|=1L<<port; }
    private static Engine treasure(int region) {
        Engine e=new Engine();e.gold=10000;
        for(int offset:new int[]{0,2,4}) { port(e,region*8+offset);check(Expeditions.clue(e),"unique port clue"); }
        port(e,region*8+7);check(Expeditions.dig(e),"three clues unlock final site");return e;
    }
    private static void maps() {
        Engine e=new Engine();int gold=e.gold,day=e.day;
        check(Expeditions.clue(e) && e.gold==gold-40 && e.day==day+1,"clue expedition costs disclosed money/time");
        gold=e.gold;check(!Expeditions.clue(e) && e.gold==gold,"cannot farm port clue");
        check(!Expeditions.dig(e),"incomplete map cannot dig");
        for(int region=0;region<7;region++) {
            e=treasure(region);gold=e.gold;
            check(Expeditions.fragments(e,region)==3,"three map fragments retained");
            check((e.expedition.treasures & (1<<region))!=0,"regional treasure recorded");
            check(!Expeditions.dig(e) && e.gold==gold,"treasure reward only once");
            check(Engine.decode(e.encode())!=null,"treasure saves");
        }
    }
    private static void conversations() {
        Engine e=new Engine();int morale=e.town.morale;
        check(CrewConversations.choose(e,1,0,0),"available companion dialogue choice");
        check(e.expedition.trust[0]==2 && e.town.morale==morale+10,"empathetic choice trust and morale");
        check(!CrewConversations.choose(e,1,0,1),"conversation choice cannot farm");
        check(!CrewConversations.choose(e,5,0,0),"unjoined companion locked");
        check(!CrewConversations.choose(e,1,1,0),"second talk requires story and treasure");
        e.chapter=1;e.expedition.treasures=1;e.expedition.clues=21;e.visited|=21;
        check(CrewConversations.choose(e,1,1,1) && e.expedition.trust[0]==3,"second talk remembers practical choice");
        check(Engine.decode(e.encode())!=null,"conversation decisions survive save");
    }
    private static void bosses() {
        for(int region=0;region<7;region++) battleIn(region);
        Engine e=treasure(0);RegionBoss.begin(e);int gold=e.gold;
        int turn=e.expedition.bossTurn;check(RegionBoss.choose(e,0,turn),"fresh battle input accepted");
        int hp=e.expedition.bossHP;
        check(!RegionBoss.choose(e,0,turn) && e.expedition.bossHP==hp,"duplicate old turn cannot attack twice");
        check(RegionBoss.choose(e,2) && !e.expedition.active() && e.gold==gold,"retreat available without reward");
        check(!RegionBoss.choose(e,0),"cannot attack absent boss");
    }
    private static void battleIn(int region) {
            Engine e=treasure(region);e.level=3;e.hull=e.maxHull();
            check(RegionBoss.begin(e),"treasure unlocks local rival");
            check(CaptainPlan.next(e).title.contains("강적"),"active rival briefing names correct encounter");
            int gold=e.gold;
            check(!e.work() && !e.buy(0,1) && !Expeditions.clue(e),"battle locks unrelated actions");
            for(int turn=0;turn<30 && e.expedition.active();turn++) {
                Engine loaded=Engine.decode(e.encode());check(loaded!=null,"battle turn restores");e=loaded;
                int choice=RegionBoss.intent(e)==0?1:0;
                check(RegionBoss.choose(e,choice),"telegraphed attack or defense");
            }
            check(!e.expedition.active() && (e.expedition.trophies & (1<<region))!=0,"strategic play wins regional trophy");
            check(e.gold>gold && !RegionBoss.begin(e),"unique boss reward cannot repeat");
    }
    private static void legacyAndValidation() throws Exception {
        Engine old=Engine.decode(new String(Files.readAllBytes(Paths.get("tests/fixtures/v7-save.txt")),StandardCharsets.US_ASCII));
        check(old!=null && old.town.moving() && old.town.morale==83,"BLUE7 voyage survives migration");
        check(old.expedition.clues==0 && old.expedition.talks==0,"old saves receive empty expedition state");
        Engine e=new Engine();e.expedition.clues=1L<<60;
        check(Engine.decode(e.encode())==null,"unknown clue bits rejected");
        e=new Engine();e.expedition.trust[0]=4;
        check(Engine.decode(e.encode())==null,"trust must match recorded choices");
        e=treasure(0);RegionBoss.begin(e);e.adventure.pending=0;
        check(Engine.decode(e.encode())==null,"overlapping encounters rejected");
    }
    public static void main(String[] args) throws Exception {
        maps();conversations();bosses();legacyAndValidation();System.out.println("PASS: "+checks+" expedition arc checks");
    }
}
