package com.towerfront;

/** Release smoke test: fixed policy is a reproducible baseline, not a human player. */
public final class FullCampaignTest {
    private static void check(boolean ok,String message) {
        if(!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Profile easy=new Profile();
        for(int mode=0;mode<3;mode++) {
            int wins=0;
            for(int stage=1;stage<=100;stage++) {
                continuation(new Engine(stage,mode));
                Engine e=CampaignPlaytest.play(stage,mode);
                check(e.result()!=0,"fixed policy must finish within 600s: "+stage+":"+mode);
                if(e.result()==1) wins++;
                if(mode==0) {
                    check(easy.available(stage,mode),"easy progression remains unlocked");
                    check(e.result()==1,"easy fixed-policy regression: "+stage);
                    easy.recordBattle(e);
                }
            }
            System.out.println("100-mission baseline mode="+mode+" wins="+wins+"/100");
        }
        check(easy.unlocked(0)==100 && easy.total(0)>=100,"easy campaign can complete");
        System.out.println("300 active-battle save continuations passed");
    }
    private static void continuation(Engine e) {
        int source=-1,target=-1;
        for(int i=0;i<e.towers.size();i++) {
            if(e.towers.get(i).owner==1) source=i;else target=i;
        }
        check(source>=0 && target>=0,"every mission has both sides");
        e.useSkill(BattleRules.SHIELD,source);e.send(source,target);e.update(.5);
        check(e.result()==0 && !e.fleets.isEmpty(),"snapshot contains active combat");
        String encoded=SaveCodec.encode(e);Engine restored=SaveCodec.decode(encoded);
        check(restored!=null && encoded.equals(SaveCodec.encode(restored)),"exact snapshot restores");
        for(int step=0;step<100;step++) {e.update(.05);restored.update(.05);}
        check(SaveCodec.encode(e).equals(SaveCodec.encode(restored)),"active replay diverged: "+e.level+":"+e.difficulty);
    }
}
