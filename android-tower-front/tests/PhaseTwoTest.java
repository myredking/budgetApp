package com.towerfront;
public class PhaseTwoTest {
    static void check(boolean ok,String s) {if(!ok) throw new AssertionError(s);}
    public static void main(String[] args) {
        Engine a=new Engine(1);
        check(a.upgrade(0),"upgrade affordable");
        check(a.towers.get(0).rank==2 && a.towers.get(0).units==12,"upgrade costs 20");
        check(!a.upgrade(0),"insufficient units");
        check(!a.upgrade(1) && !a.upgrade(-1),"ownership and bounds");
        a.towers.get(0).units=99;
        check(a.upgrade(0),"rank three");
        check(a.towers.get(0).units==59,"second upgrade costs 40");
        check(!a.upgrade(0),"rank cap");
        Engine b=new Engine(1),c=new Engine(1);
        b.towers.get(0).kind=Engine.FACTORY;
        b.update(1);c.update(1);
        check(b.towers.get(0).units>c.towers.get(0).units,"factory production");
        Engine d=new Engine(1),e=new Engine(1);
        d.towers.get(0).kind=Engine.RELAY;
        d.send(0,2);e.send(0,2);
        check(d.fleets.get(0).duration<e.fleets.get(0).duration,"relay speed");
        check(new Engine(2).towers.get(0).x!=new Engine(1).towers.get(0).x,"map variety");
        productionChecks();
        System.out.println("Phase two checks passed");
    }
    static void productionChecks() {
        Engine a=new Engine(1),b=new Engine(1),c=new Engine(1);
        b.towers.get(0).units=99;b.upgrade(0);
        c.towers.get(0).units=99;c.upgrade(0);c.upgrade(0);
        a.towers.get(0).units=0;b.towers.get(0).units=0;c.towers.get(0).units=0;
        a.update(2);b.update(2);c.update(2);
        check(a.towers.get(0).units==3,"base production");
        check(b.towers.get(0).units==5,"rank two production 150 percent");
        check(c.towers.get(0).units==7,"rank three production 200 percent");
    }
}
