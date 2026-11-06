package com.bluehorizon;

public final class FundingGuideTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    public static void main(String[] args) {
        Engine e=new Engine();e.chapter=7;e.port=53;e.visited|=1L<<53;e.gold=3000;e.level=3;e.hull=e.maxHull();e.adventure.claims=255;
        check(FundingPlan.page(e)==9 && CaptainPlan.next(e).page==9,"publication funding exposes regional exploration");
        e.expedition.treasures=1<<6;
        e.expedition.clues=(1L<<48)|(1L<<50)|(1L<<52);e.visited|=e.expedition.clues;
        check(FundingPlan.page(e)==9,"unclaimed affordable boss is a funding opportunity");
        e.expedition.trophies=1<<6;
        check(FundingPlan.page(e)==5,"completed region points to commissions");
        e.expedition.trophies=0;e.level=2;e.hull=e.maxHull();
        check(FundingPlan.page(e)==5,"unaffordable required upgrade points to commissions");
        e.gold=5000;check(FundingPlan.page(e)==2,"funded required upgrade points to shipyard");
        e.level=3;e.hull=30;check(FundingPlan.page(e)==2,"low hull is repaired before boss suggestion");
        e.gold=0;check(FundingPlan.page(e)==2,"recovery work remains available without startup funds");
        e.gold=12000;check(CaptainPlan.next(e).target!=0,"publication budget leaves safe return supplies");
        e.gold=FundingPlan.required(e);check(CaptainPlan.next(e).target==0,"fully funded publication returns to Lisbon");
        check(e.sail(0,1) && e.chapter==8,"safe return preserves enough funds for ending");
        check(Engine.decode(e.encode())!=null,"ending from funding plan is restorable");
        e=new Engine();e.port=7;e.visited=149;e.expedition.clues=21;e.gold=50;
        check(FundingPlan.page(e)==2,"complete map with insufficient dig fee points to recovery");
        e.gold=80;check(FundingPlan.page(e)==9,"onsite exact dig fee is enough without a travel fee");
        e.town.morale=0;check(FundingPlan.page(e)==9,"low morale does not add supply fee to stationary digging");
        e=new Engine();e.port=17;e.visited|=1L<<17;e.expedition.clues=1L<<17;e.gold=40;
        check(FundingPlan.page(e)==2,"investigated remote port needs affordable next-clue travel");
        System.out.println("PASS: "+checks+" funding guide checks");
    }
}
