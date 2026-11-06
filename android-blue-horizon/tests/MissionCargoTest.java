package com.bluehorizon;

public final class MissionCargoTest {
    private static int checks;
    private static void check(boolean value,String label) { checks++;if(!value) throw new AssertionError(label); }
    private static void storyCargo() {
        Engine e=new Engine();e.inventory[0]=12;
        check(MissionCargo.held(e,0)==5,"opening delivery wood protected before arrival");
        TradeOrder order=TradeOrder.quote(e,0,true,2,0,0,true);
        check(order.quantity==7 && order.reserved==5,"sell all excludes mission cargo visibly");
        check(order.execute(e) && e.inventory[0]==5,"confirmation keeps required wood");
        e.port=1;e.visited|=2;
        order=TradeOrder.quote(e,0,true,0,5,0,true);
        check(order.valid() && order.reserved==0,"story destination unlocks required sale");
        check(order.execute(e) && e.chapter==1,"protected order can complete opening chapter");
        e=new Engine();e.inventory[0]=5;
        check(!TradeOrder.quote(e,0,true,2,0,0,true).valid(),"all cargo reserved prevents accidental emptying");
        check(TradeOrder.quote(e,0,true,2,0,0,false).quantity==5,"explicit opt out preserves player choice");
    }
    private static void contractsAndRace() {
        Engine e=new Engine();Contracts.accept(e);int good=e.adventure.good;
        e.inventory[good]=20;
        int story=MissionCargo.held(e,good);
        check(story>=e.adventure.amount,"active contract cargo protected");
        e.rivals.raceTarget=1;e.rivals.raceGood=good;e.rivals.raceAmount=4;e.rivals.raceDeadline=e.day+10;
        check(MissionCargo.held(e,good)==Math.min(20,story+4),"overlapping goals require separate cargo");
        e.day=e.adventure.deadline+20;e.rivals.raceDeadline=e.day-1;
        check(MissionCargo.held(e,good)==(good==0?5:0),"expired assignments do not lock cargo");
        check(MissionCargo.held(e,-1)==0 && MissionCargo.held(e,6)==0,"invalid goods safe");
    }
    private static void sourceVisits() {
        Engine e=new Engine();e.chapter=4;e.port=32;e.inventory[3]=5;
        check(MissionCargo.held(e,3)==5,"Malacca arrival before Calicut visit keeps story spices");
        e.visited|=1L<<28;check(MissionCargo.held(e,3)==0,"eligible spice delivery releases cargo");
        e=new Engine();e.chapter=5;e.port=44;e.inventory[4]=5;
        check(MissionCargo.held(e,4)==5,"Nagasaki arrival before Hangzhou visit keeps story tea");
        e.visited|=1L<<42;check(MissionCargo.held(e,4)==0,"eligible tea delivery releases cargo");
    }
    public static void main(String[] args) {
        storyCargo();contractsAndRace();sourceVisits();System.out.println("PASS: "+checks+" mission cargo checks");
    }
}
