package com.bluehorizon;

public final class ShipPerformanceTest {
    private static int checks;
    private static void check(boolean value,String label) { checks++;if(!value) throw new AssertionError(label); }
    public static void main(String[] args) {
        Engine e=new Engine();int days=e.travelDays(28),cost=e.travelCost(28,1);
        e.level=2;check(e.travelDays(28)<days,"caravel upgrade makes long voyage faster");
        check(e.travelCost(28,1)<cost,"faster ship lowers total supply cost");
        days=e.travelDays(28);e.level=3;check(e.travelDays(28)<days,"galleon is faster again");
        days=e.travelDays(28);e.refits.levels[0]=2;
        check(e.travelDays(28)<days,"sail refit stacks with ship performance");
        for(int port=0;port<56;port++) {
            e=new Engine();int first=e.travelDays(port);e.level=2;int second=e.travelDays(port);e.level=3;
            check(e.travelDays(port)<=second && second<=first,"upgrade never slows a route");
            if(port!=0) check(e.travelDays(port)>=2,"short routes keep minimum duration");
        }
        System.out.println("PASS: "+checks+" ship performance checks");
    }
}
