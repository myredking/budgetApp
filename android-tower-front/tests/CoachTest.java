package com.towerfront;

public final class CoachTest {
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Engine first=new Engine(1);
        String before=SaveCodec.encode(first);
        check(BattleCoach.tip(first,-1).contains("파란 타워"),"first step selects ally");
        check(BattleCoach.tip(first,0).contains("출병"),"selected ally teaches dispatch");
        check(before.equals(SaveCodec.encode(first)),"guidance cannot change battle");
        first.send(0,2);
        check(BattleCoach.tip(first,-1).contains("도착"),"dispatch teaches arrival");
        first.captures=1;
        check(BattleCoach.tip(first,-1).contains("점령한"),"capture advances tutorial");
        Engine second=new Engine(2);second.towers.get(0).units=19;
        check(BattleCoach.tip(second,0).contains("20명"),"upgrade explains required troops");
        second.towers.get(0).units=40;second.upgrade(0);
        check(BattleCoach.tip(second,0).contains("강화 완료"),"upgrade completes lesson");
        Engine third=new Engine(3);
        check(BattleCoach.tip(third,0).contains("스킬"),"third mission teaches skills");
        third.useSkill(BattleRules.REINFORCE,0);
        check(BattleCoach.tip(third,0).contains("사용 완료"),"skill completes lesson");
        check(BattleCoach.tip(third,999)!=null,"invalid selection safe");
        check(BattleCoach.tip(Skirmish.create(0,10,1,42),-1).contains("전술"),"free play has general hint");
        check(BattleCoach.defeatHint(new Engine(1)).contains("출병"),"no dispatch gives action hint");
        Engine hold=new Engine(4);hold.dispatched=3;
        check(BattleCoach.defeatHint(hold).contains("보급"),"hold loss teaches objective");
        System.out.println("Onboarding steps, defeat hints and state isolation checks passed");
    }
}
