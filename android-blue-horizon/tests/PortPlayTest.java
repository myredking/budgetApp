package com.bluehorizon;

public final class PortPlayTest {
    private static int checks;
    private static void check(boolean value,String text) { checks++;if(!value) throw new AssertionError(text); }
    private static void firstJourney() {
        Engine e=new Engine();CaptainPlan plan=CaptainPlan.next(e);
        check(plan.page==1 && plan.good==0 && plan.amount==5,"opening points directly to wood order");
        e.buy(0,5);plan=CaptainPlan.next(e);
        check(plan.target==1 && plan.page==0,"cargo ready points to Seville route");
        e.sail(1,0);plan=CaptainPlan.next(e);
        check(plan.page==1 && plan.selling && plan.amount==5,"arrival points to story sale");
        e.sell(0,5);plan=CaptainPlan.next(e);
        check(plan.title.contains("보상") && plan.page==5,"earned achievement is visible and claimable");
        Achievements.claim(e,0);plan=CaptainPlan.next(e);
        check(plan.target==2,"after reward, northward objective resumes");
    }
    private static void priorityAndSafety() {
        Engine e=new Engine();e.hull=20;
        check(CaptainPlan.next(e).page==2,"repair before unsafe departure");
        e=new Engine();Contracts.accept(e);e.port=e.adventure.target;e.inventory[e.adventure.good]=e.adventure.amount;
        check(CaptainPlan.next(e).deliver,"ready delivery cannot hide behind story");
        e.hull=20;check(CaptainPlan.next(e).deliver,"zero-day delivery precedes repair");e.hull=100;
        e.day=e.adventure.deadline+1;
        check(!CaptainPlan.next(e).deliver,"expired contract not falsely deliverable");
        e=new Engine();e.adventure.pending=0;
        check(CaptainPlan.next(e).event,"pending encounter takes priority");
        for(int chapter=0;chapter<=8;chapter++) {
            e=new Engine();e.chapter=chapter;
            check(CaptainPlan.next(e).title.length()>0,"every chapter has usable guidance");
        }
    }
    private static void readyStory() {
        Engine e=new Engine();e.chapter=7;e.gold=12000;e.adventure.claims=255;
        check(CaptainPlan.next(e).continueStory,"ready publication continues at current port");
        e.chapter=2;e.port=13;e.visited|=1L<<13;e.influence[13]=1;
        check(CaptainPlan.next(e).continueStory,"ready library is actionable");
        e.chapter=3;e.port=21;e.visited|=1L<<21;e.level=2;
        check(CaptainPlan.next(e).continueStory,"ready Cape is actionable");
    }
    private static void variedContracts() {
        for(int port=0;port<World.COUNT;port++) {
            Engine e=new Engine();e.port=port;
            Contract[] offers=Contracts.offers(e);
            check(offers.length==3,"three styles per port");
            check(offers[0].target!=offers[1].target && offers[1].target!=offers[2].target,"distinct routes");
            for(int choice=0;choice<3;choice++) {
                Contract c=offers[choice];
                check(c.amount<=e.capacity() && c.deadline>e.day+e.travelDays(c.target),"achievable cargo and deadline");
                check(c.reward>e.buyPrice(c.good)*c.amount+e.travelCost(c.target,1),"reward covers local cargo and safe supplies");
                check(Contracts.offers(e)[choice].target==c.target,"preview never rerolls");
            }
        }
        Engine e=new Engine();Contract c=Contracts.offers(e)[2];
        check(Contracts.accept(e,2) && e.adventure.target==c.target,"accept selected offer");
        Contracts.cancel(e);check(Contracts.offers(e)[2].target==c.target,"cancel cannot reroll board");
        check(!Contracts.accept(e,3) && e.adventure.target<0,"invalid choice safe");
    }
    public static void main(String[] args) {
        firstJourney();priorityAndSafety();variedContracts();readyStory();
        System.out.println("PASS: "+checks+" port gameplay checks");
    }
}
