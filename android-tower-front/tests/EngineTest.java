package com.towerfront;
public class EngineTest {
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
    public static void main(String[] args) {
        Engine e = new Engine(1);
        int before = e.towers.get(0).units;
        check(!e.send(1,0), "cannot control enemy");
        check(!e.send(0,0), "cannot attack self");
        check(e.send(0,2), "send troops");
        check(e.towers.get(0).units == before-before/2, "half dispatched");
        check(e.towers.get(2).owner == 0, "travel is delayed");
        e.update(2);
        check(e.towers.get(2).owner == 1, "neutral captured");
        Engine f = new Engine(2);
        for (Engine.Tower t : f.towers) t.owner=1;
        check(f.result()==1, "win");
        for (Engine.Tower t : f.towers) t.owner=2;
        check(f.result()==2, "lose");
        Engine g = new Engine(1);
        g.update(1);
        check(g.towers.get(0).units > 32, "production");
        g.towers.get(0).units=1;
        check(!g.send(0,2), "empty force blocked");
        Engine h = new Engine(1);
        h.towers.get(2).units=16;
        h.send(0,2); h.update(1.5);
        check(h.towers.get(2).owner==0, "equal force does not capture");
        Engine k = new Engine(3);
        k.update(3);
        check(k.fleets.size()>0, "enemy AI dispatches");
        extraChecks();
        System.out.println("13 battle checks passed");
    }
    static void extraChecks() {
        Engine a=new Engine(1);
        a.towers.get(2).owner=1;
        a.send(0,2);a.update(1.5);
        check(a.towers.get(2).units>=24, "friendly reinforcement");
        Engine b=new Engine(1);
        b.towers.get(2).owner=2;
        b.send(0,2);b.update(1.5);
        check(b.towers.get(2).owner==1, "enemy tower captured");
        Engine c=new Engine(1);
        c.send(0,2);
        for(Engine.Tower t:c.towers) if(t.owner==1) t.owner=0;
        check(c.result()==0, "blue fleet prevents defeat");
        Engine d=new Engine(1);
        d.update(3);
        for(Engine.Tower t:d.towers) if(t.owner==2) t.owner=0;
        check(d.result()==0, "red fleet prevents victory");
    }
}
