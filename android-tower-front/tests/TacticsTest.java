package com.towerfront;

public final class TacticsTest {
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        skills();defenses();speed();objectives();supply();persistence();survivalMedals();legacyAndMalformed();timing();
        System.out.println("Skills, objectives, supply, timing and persistence checks passed");
    }
    private static void skills() {
        Engine e=new Engine(1);e.rules.energy=100;
        check(e.useSkill(BattleRules.REINFORCE,0),"reinforcement casts");
        check(e.towers.get(0).units==52 && e.rules.energy==65,"reinforcement cost and amount");
        check(!e.useSkill(0,0),"cooldown prevents repeated cast");
        check(!e.useSkill(1,1) && !e.useSkill(3,0) && !e.useSkill(1,-1),"bad targets and skills rejected");
        e.rules.cooldowns[0]=0;e.towers.get(0).units=99;
        double energy=e.rules.energy;
        check(!e.useSkill(0,0) && e.rules.energy==energy,"full tower does not spend energy");
        e.rules.energy=0;check(!e.useSkill(1,0),"insufficient energy rejected");
        advance(e,1);check(e.rules.energy>0 && e.rules.energy<=100,"energy regenerates");
    }
    private static void defenses() {
        Engine e=new Engine(1);e.towers.get(0).units=30;e.rules.energy=100;
        check(e.useSkill(1,0),"shield casts");
        Engine.Fleet attack=new Engine.Fleet(1,0,2,20,.5);attack.progress=.9;e.fleets.add(attack);
        e.update(.1);check(e.towers.get(0).units==20,"shield halves damage");
        e.rules.shields[0]=.01;e.towers.get(0).units=40;
        attack=new Engine.Fleet(1,0,2,20,.5);attack.progress=.9;e.fleets.add(attack);
        e.update(.1);check(e.towers.get(0).units==20,"expired shield has no effect");
        e.rules.shields[0]=5;e.rules.haste[0]=5;e.towers.get(0).units=1;
        attack=new Engine.Fleet(1,0,2,99,.5);attack.progress=.99;e.fleets.add(attack);
        e.update(.05);
        check(e.towers.get(0).owner==2 && e.rules.shields[0]==0 && e.rules.haste[0]==0,"capture clears buffs");
    }
    private static void speed() {
        Engine a=new Engine(1),b=new Engine(1);a.rules.energy=100;
        a.send(0,2);b.send(0,2);a.useSkill(2,0);
        a.update(.25);b.update(.25);
        check(a.fleets.get(0).progress>b.fleets.get(0).progress,"haste speeds existing fleet");
        check(a.fleets.get(0).duration==b.fleets.get(0).duration,"base duration remains stable");
    }
    private static void objectives() {
        Engine hold=new Engine(4);check(hold.rules.objective==BattleRules.HOLD,"hold mission assigned");
        hold.towers.get(hold.rules.supplyIndex).owner=1;advance(hold,5);
        check(hold.rules.progress>4.9,"holding supply progresses");
        hold.towers.get(hold.rules.supplyIndex).owner=2;advance(hold,.1);
        check(hold.rules.progress==0,"losing supply resets timer");
        hold.towers.get(hold.rules.supplyIndex).owner=1;advance(hold,hold.rules.goalSeconds+.1);
        check(hold.result()==1,"hold objective wins while enemies remain");
        Engine survive=new Engine(5);advance(survive,survive.rules.goalSeconds+.1);
        check(survive.result()==1,"survival timer wins");
        check(survive.rules.progress==survive.rules.goalSeconds,"winning timer displays full goal");
        check(survive.elapsed>=survive.rules.goalSeconds,"survival clock reaches its required duration");
        Engine control=new Engine(6);
        control.towers.get(2).owner=1;control.towers.get(4).owner=1;
        advance(control,control.rules.goalSeconds+.1);check(control.result()==1,"three bases held wins");
    }
    private static void supply() {
        Engine a=new Engine(1),b=new Engine(1);
        check(a.rules.supplyIndex>=0,"supply exists");
        a.towers.get(a.rules.supplyIndex).owner=1;
        a.towers.get(0).units=0;b.towers.get(0).units=0;
        advance(a,2);advance(b,2);
        check(a.towers.get(0).units>b.towers.get(0).units,"supply boosts global production");
        check(a.rules.energy>b.rules.energy,"supply improves energy regeneration");
        Engine many=Skirmish.create(2,10,1,7);
        check(many.remainingEnemies()==10,"supply setup preserves ten factions");
        check(many.rules.objective==BattleRules.CONQUEST,"free mode remains conquest");
    }
    private static void persistence() {
        Engine e=new Engine(4);e.rules.energy=100;e.useSkill(1,0);e.useSkill(2,0);
        e.towers.get(e.rules.supplyIndex).owner=1;advance(e,1);
        String saved=SaveCodec.encode(e);Engine restored=SaveCodec.decode(saved);
        check(restored!=null && saved.equals(SaveCodec.encode(restored)),"tactics state exact restore");
        e.update(.2);restored.update(.2);
        check(SaveCodec.encode(e).equals(SaveCodec.encode(restored)),"restored tactics evolve identically");
    }
    private static void survivalMedals() {
        Engine e=new Engine(5);advance(e,e.rules.goalSeconds+.1);
        check(e.medals()==1,"surviving with one base earns one star");
        e=new Engine(5);e.towers.get(2).owner=1;e.towers.get(4).owner=1;
        advance(e,e.rules.goalSeconds+.1);check(e.medals()==3,"three bases earn survival three stars");
        Engine fleetOnly=new Engine(5);
        for(Engine.Tower t:fleetOnly.towers) t.owner=2;
        fleetOnly.fleets.add(new Engine.Fleet(0,1,1,5,1));
        fleetOnly.rules.progress=fleetOnly.rules.goalSeconds;
        check(fleetOnly.result()==1 && fleetOnly.medals()>=1,"surviving fleet victory still earns a star");
    }
    private static void legacyAndMalformed() {
        Engine e=new Engine(4);byte[] data=java.util.Base64.getDecoder().decode(SaveCodec.encode(e));
        int rulesAt=44+e.towers.size()*40+21;
        byte[] old=java.util.Arrays.copyOf(data,rulesAt);java.nio.ByteBuffer.wrap(old).putInt(0,0x54465734);
        Engine migrated=SaveCodec.decode(java.util.Base64.getEncoder().encodeToString(old));
        check(migrated!=null && migrated.rules.objective==BattleRules.CONQUEST && migrated.rules.supplyIndex==-1,"v5 battle keeps old objective");
        byte[] bad=data.clone();java.nio.ByteBuffer.wrap(bad).putDouble(rulesAt+24,Double.NaN);
        check(reject(bad),"NaN energy rejected");
        bad=data.clone();java.nio.ByteBuffer.wrap(bad).putDouble(rulesAt+32,100);
        check(reject(bad),"invalid cooldown rejected");
        bad=data.clone();java.nio.ByteBuffer.wrap(bad).putDouble(rulesAt+56,7);
        check(reject(bad),"invalid shield time rejected");
    }
    private static boolean reject(byte[] data) {
        return SaveCodec.decode(java.util.Base64.getEncoder().encodeToString(data))==null;
    }
    private static void timing() {
        Engine e=new Engine(4);int target=e.rules.supplyIndex;
        e.fleets.add(new Engine.Fleet(0,target,1,99,.2));e.update(1);
        check(e.rules.progress>.7 && e.rules.progress<.9,"hold time starts after actual capture");
        e=new Engine(4);target=e.rules.supplyIndex;
        e.towers.get(target).owner=1;e.towers.get(target).units=1;e.rules.progress=10;
        e.fleets.add(new Engine.Fleet(1,target,2,99,.1));
        e.fleets.add(new Engine.Fleet(0,target,1,99,.1));e.update(.1);
        check(e.towers.get(target).owner==1 && e.rules.progress==0,"same-frame recapture still resets hold");
        e=new Engine(1);e.towers.get(0).kind=Engine.FORTRESS;e.towers.get(0).units=30;
        e.rules.energy=100;e.useSkill(1,0);e.fleets.add(new Engine.Fleet(1,0,2,16,.1));e.update(.1);
        check(e.towers.get(0).units==24,"fortress and shield combine multiplicatively");
    }
    private static void advance(Engine e,double seconds) {
        while(seconds>1e-8 && e.result()==0) {
            double dt=Math.min(.25,seconds);e.aiTime=0;e.update(dt);seconds-=dt;
        }
    }
}
