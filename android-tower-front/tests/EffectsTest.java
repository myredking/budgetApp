package com.towerfront;

public final class EffectsTest {
    private static void check(boolean ok,String text) {
        if(!ok) throw new AssertionError(text);
    }
    public static void main(String[] args) {
        arrivalChecks();animationChecks();queueChecks();reinforcementCapChecks();frameChecks();
        System.out.println("Impact, capture, reinforcement and finite animation checks passed");
    }
    private static void arrivalChecks() {
        Engine hit=new Engine(1);hit.towers.get(2).units=40;
        hit.send(0,2);hit.update(1.4);
        Engine.Impact a=hit.drainImpacts().get(0);
        check(a.kind==Engine.HIT && a.amount==16 && a.target==2,"attack event exact");
        check(hit.drainImpacts().isEmpty(),"events consumed once");
        Engine capture=new Engine(1);capture.send(0,2);capture.update(1.4);
        check(capture.drainImpacts().get(0).kind==Engine.CAPTURE,"capture distinguished");
        Engine ally=new Engine(1);ally.towers.get(2).owner=1;
        ally.send(0,2);ally.update(1.4);
        check(ally.drainImpacts().get(0).kind==Engine.REINFORCE,"reinforcement distinguished");
        check(SaveCodec.decode(SaveCodec.encode(hit)).drainImpacts().isEmpty(),"effects never replay from save");
    }
    private static void animationChecks() {
        BattleEffects fx=new BattleEffects();
        fx.impact(new Engine.Impact(2,1,16,Engine.CAPTURE),100,200);
        fx.update(.15);
        check(fx.towerScale(2)>1,"capture bounce");
        check(!fx.bursts.isEmpty(),"visible burst");
        fx.update(2);
        check(fx.bursts.isEmpty() && fx.towerScale(2)==1,"impact cleanup");
        fx.celebrate();check(fx.confetti.size()==72,"victory particles");
        fx.celebrate();check(fx.confetti.size()==72,"victory starts once");
        fx.update(4);check(fx.confetti.isEmpty(),"celebration ends");
        fx.update(Double.NaN);fx.update(-1);
        check(fx.confetti.isEmpty(),"invalid animation time ignored");
    }
    private static void queueChecks() {
        Engine e=new Engine(1);e.towers.get(2).owner=1;
        for(int i=0;i<200;i++) e.fleets.add(new Engine.Fleet(0,2,1,1,.4));
        e.update(.5);check(e.drainImpacts().size()<=64,"event queue bounded");
        BattleEffects fx=new BattleEffects();
        for(int i=0;i<200;i++) fx.impact(new Engine.Impact(2,1,1,Engine.HIT),100,100);
        check(fx.bursts.size()<=32,"visual effects bounded");
    }
    private static void reinforcementCapChecks() {
        Engine e=new Engine(1);e.towers.get(2).owner=1;e.towers.get(2).units=98;
        e.fleets.add(new Engine.Fleet(0,2,1,10,.4));e.update(.5);
        check(e.drainImpacts().get(0).amount==1,"show actual received troops at cap");
        e.fleets.add(new Engine.Fleet(0,2,1,10,.4));e.update(.5);
        check(e.drainImpacts().isEmpty(),"no false reinforcement at full tower");
    }
    private static void frameChecks() {
        BattleEffects fx=new BattleEffects();
        check(fx.needsFrame(true,false,0),"active battle keeps animating");
        check(!fx.needsFrame(true,true,0),"paused battle does not redraw continuously");
        check(!fx.needsFrame(false,false,0),"background battle stops frames");
        check(!fx.needsFrame(true,false,2),"idle result screen stops frames");
        fx.celebrate();check(fx.needsFrame(true,false,1),"victory animation can finish");
        check(!fx.needsFrame(true,true,1),"pause also freezes celebration");
        fx.update(4);check(!fx.needsFrame(true,false,1),"completed celebration stops frames");
        fx.impact(new Engine.Impact(2,1,4,Engine.HIT),100,200);
        check(fx.needsFrame(true,false,2),"last hit effect can finish");
        fx.update(2);check(!fx.needsFrame(true,false,2),"completed last hit stops frames");
    }
}
