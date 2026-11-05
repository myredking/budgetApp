package com.towerfront;

/** Fixed-policy pacing smoke test; this does not replace human playtesting. */
public final class CampaignPlaytest {
    public static void main(String[] args) {
        for(int mode=0;mode<3;mode++) {
            int wins=0;
            for(int stage=1;stage<=12;stage++) {
                Engine e=play(stage,mode);
                if(e.result()==1) wins++;
                if(mode==0 || (mode==1 && stage<=6)) requireWin(e);
                if(e.result()==0) throw new AssertionError("Battle stalled");
            }
            System.out.println("Half-dispatch with reinforcement policy mode="+mode+" wins="+wins+"/12");
        }
    }
    private static Engine play(int stage,int mode) {
        Engine e=new Engine(stage,mode);
        for(int step=0;step<12000 && e.result()==0;step++) {
            if(step%8==0) command(e);
            e.update(.05);
        }
        return e;
    }
    private static void command(Engine e) {
        for(int i=0;i<e.towers.size();i++) {
            if(e.towers.get(i).owner!=1) continue;
            if(e.towers.get(i).units<45) e.useSkill(BattleRules.REINFORCE,i);
            int target=choose(e,i);
            if(target>=0) e.send(i,target);
        }
    }
    private static int incoming(Engine e,int target) {
        int amount=0;
        for(Engine.Fleet f:e.fleets) if(f.to==target && f.owner==1) amount+=f.units;
        return amount;
    }
    private static int choose(Engine e,int source) {
        Engine.Tower a=e.towers.get(source);int force=a.units/2,best=-1;
        double score=1e9;
        for(int i=0;i<e.towers.size();i++) {
            Engine.Tower b=e.towers.get(i);
            if(b.owner==1 || incoming(e,i)>b.units+4) continue;
            double distance=Math.hypot(a.x-b.x,a.y-b.y);
            double defense=b.units+(b.owner==0?0:distance*9);
            if(force<=defense+1) continue;
            double value=defense+distance*10-(b.kind==Engine.FACTORY?8:0);
            if(value<score) {best=i;score=value;}
        }
        return best;
    }
    private static void requireWin(Engine e) {
        if(e.result()!=1) throw new AssertionError("Onboarding pacing regressed stage="+e.level+" mode="+e.difficulty);
    }
}
