package com.towerfront;

public final class RecordsTest {
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        recordChecks();progressChecks();debriefChecks();
        System.out.println("Personal records, progression and next-target checks passed");
    }
    private static Engine victory(int stage,double seconds) {
        Engine e=new Engine(stage,1);e.elapsed=seconds;
        for(Engine.Tower tower:e.towers) tower.owner=1;
        return e;
    }
    private static void recordChecks() {
        BattleRecords records=new BattleRecords();
        check(records.bestMillis(1,1)==-1,"legacy clears have no invented time");
        Engine e=victory(1,95.125);records.recordBattle(e);records.recordBattle(e);
        check(records.bestMillis(1,1)==95125,"repeated saves are idempotent");
        records.recordBattle(victory(1,100));check(records.bestMillis(1,1)==95125,"slower retry keeps best");
        records.recordBattle(victory(1,80));check(records.bestMillis(1,1)==80000,"faster retry improves best");
        check(records.bestMillis(1,0)==-1,"difficulties stay independent");
        check(records.encode().equals(BattleRecords.decode(records.encode()).encode()),"exact record round trip");
        check(BattleRecords.decode("broken").bestMillis(1,1)==-1,"corrupt records recover safely");
        check(BattleRecords.decode("TR1:"+"9".repeat(25000)).bestMillis(1,1)==-1,"oversized data rejected");
        check(BattleRecords.decode(records.encode().replace("80000","-2")).bestMillis(1,1)==-1,"invalid value rejects whole record set");
        check(BattleRecords.decode(records.encode()+",").bestMillis(1,1)==-1,"wrong field count rejected");
        e.elapsed=Double.NaN;records.recordBattle(e);check(records.bestMillis(1,1)==80000,"invalid time ignored");
        Engine free=Skirmish.create(0,3,1,42);for(Engine.Tower t:free.towers) t.owner=1;
        records.recordBattle(free);check(records.bestMillis(1,1)==80000,"free play cannot change mission records");
        records.recordBattle(new Engine(2));check(records.bestMillis(2,1)==-1,"unfinished battle cannot set best");
    }
    private static void progressChecks() {
        Profile p=new Profile();check(CampaignProgress.next(p,1)==1,"new player starts first mission");
        for(int stage=1;stage<=100;stage++) p.record(stage,1,stage==7?2:3);
        check(CampaignProgress.completed(p,1)==100,"cleared count includes all missions");
        check(CampaignProgress.perfect(p,1)==99,"perfect count excludes two-star clear");
        check(CampaignProgress.chapterStars(p,1,0)==29,"sector total is accurate");
        check(CampaignProgress.next(p,1)==7,"completed campaign suggests missing star");
        p.record(7,1,3);check(CampaignProgress.next(p,1)==100,"fully perfect campaign remains replayable");
        check(CampaignProgress.completed(p,0)==0,"progression respects selected difficulty");
    }
    private static void debriefChecks() {
        Profile p=new Profile();BattleRecords records=new BattleRecords();Engine e=victory(1,100);
        records.recordBattle(e);p.recordBattle(e);
        check(BattleDebrief.recordText(e,records,-1).contains("첫 기록"),"first time record does not mislabel legacy clears");
        check(BattleDebrief.nextTarget(e,p).contains("90초"),"two stars explains three-star target");
        check(BattleDebrief.recordText(e,records,110000).contains("신기록"),"faster clear celebrates new best");
        check(!BattleDebrief.recordText(e,records,100000).contains("신기록"),"reopening result cannot invent record");
        Engine survive=victory(5,60);
        for(Engine.Tower tower:survive.towers) tower.owner=2;
        survive.towers.get(0).owner=1;survive.rules.progress=survive.rules.goalSeconds;p.recordBattle(survive);
        check(BattleDebrief.nextTarget(survive,p).contains("거점"),"survival target uses bases rather than time");
        check(BattleDebrief.clock(95125).equals("01:35.1"),"record clock shows tenths");
    }
}
