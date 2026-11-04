package com.towerfront;

public final class FortressTest {
    private static void check(boolean ok,String text) {
        if(!ok) throw new AssertionError(text);
    }
    public static void main(String[] args) {
        damageChecks();captureChecks();campaignChecks();
        System.out.println("Defense tower, persistence and campaign pacing checks passed");
    }
    private static void damageChecks() {
        Engine e=new Engine(1);Engine.Tower t=e.towers.get(2);
        t.kind=Engine.FORTRESS;t.units=30;
        e.fleets.add(new Engine.Fleet(0,2,1,16,.4));e.update(.5);
        check(t.units==18,"fortress reduces sixteen attack to twelve");
        check(e.drainImpacts().get(0).amount==12,"damage label shows actual impact");
        Engine restored=SaveCodec.decode(SaveCodec.encode(e));
        check(restored!=null && restored.towers.get(2).kind==Engine.FORTRESS,"defense tower persists");
        e.fleets.add(new Engine.Fleet(0,2,1,1,.4));e.update(.5);
        check(t.units==17,"minimum damage remains one");
    }
    private static void captureChecks() {
        Engine e=new Engine(1);Engine.Tower t=e.towers.get(2);
        t.kind=Engine.FORTRESS;t.units=10;
        e.fleets.add(new Engine.Fleet(0,2,1,16,.4));e.update(.5);
        check(t.owner==1 && t.units==2,"capture uses reduced damage");
        check(t.kind==Engine.FORTRESS,"captured fortress keeps defense");
        t.units=10;t.growth=0;
        e.fleets.add(new Engine.Fleet(0,2,1,8,.4));e.update(.5);
        check(t.units==18,"friendly reinforcement not reduced");
    }
    private static void campaignChecks() {
        check(new Engine(4).towers.get(5).kind==Engine.FORTRESS,"defense introduced stage four");
        check(new Engine(1).towers.get(1).units==16,"normal onboarding enemy count");
        check(new Engine(2,0).towers.get(6).owner==0,"easy extra tower begins neutral");
        check(new Engine(6,1).towers.get(6).owner==0,"normal delays second enemy base");
        check(new Engine(7,1).towers.get(6).owner==2,"normal second base introduced later");
        check(new Engine(7,1).towers.get(5).owner==1,"normal late campaign starts with defense outpost");
        check(new Engine(6,0).towers.get(0).units>new Engine(6,2).towers.get(0).units,"easy starting reserve");
    }
}
