package com.bluehorizon;
public final class BattleFrameTest {
    private static int count;
    private static void check(boolean value,String why) { count++;if(!value) throw new AssertionError(why); }
    public static void main(String[] args) {
        BattleFrame attack=new BattleFrame(0,100,92,100,80,55,80);
        check(attack.ourDamage==8 && attack.enemyDamage==25,"actual state deltas supply damage numbers");
        check(attack.ourFraction(0)==1 && Math.abs(attack.ourFraction(1)-.92f)<.001,"hull bar interpolates previous to current");
        check(Math.abs(attack.enemyFraction(1)-55/80f)<.001,"enemy bar shows current hull");
        check(attack.firing() && !new BattleFrame(-1,100,100,100,80,80,80).firing(),"idle opening never fires");
        check(new BattleFrame(1,100,97,100,80,65,80).guarding(),"defend visual distinct");
        check(!new BattleFrame(2,100,96,100,80,80,80).firing(),"retreat never fires");
        check(attack.progress(-5)==0 && attack.progress(5000)==1,"animation time bounded");
        check(attack.ourFraction(-1)==1 && Math.abs(attack.enemyFraction(2)-55/80f)<.001,"bar progress bounded");
        BattleFrame invalid=new BattleFrame(0,-10,200,0,0,-4,0);
        check(invalid.ourFraction(1)<=1 && invalid.enemyFraction(1)>=0,"invalid rendering inputs never overflow bars");
        forcedDefeat();
        System.out.println("PASS: "+count+" battle animation checks");
    }
    private static void forcedDefeat() {
        Engine sea=new Engine();sea.adventure.pending=1;sea.adventure.battleHP=100;sea.adventure.battleTurn=11;
        SeaBattle.choose(sea,0);
        check(sea.adventure.battleHP==0 && sea.battleEnemyAfter>0 && sea.battleEnemyAfter<100,"sea defeat keeps actual last shot for visuals");
        Engine boss=new Engine();boss.expedition.bossRegion=0;boss.expedition.bossHP=80;boss.expedition.bossTurn=29;
        RegionBoss.choose(boss,0);
        check(!boss.expedition.active() && boss.battleEnemyAfter>0 && boss.battleEnemyAfter<80,"boss defeat keeps actual last shot for visuals");
        check(Engine.decode(boss.encode()).battleEnemyAfter==-1,"visual cache never persists into restored turns");
    }
}
