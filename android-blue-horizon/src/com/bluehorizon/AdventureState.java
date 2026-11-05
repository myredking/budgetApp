package com.bluehorizon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class AdventureState {
    public int voyages,resolved,battles,deliveries,rescues,surveys,souvenirs,claims;
    public int pending=-1,roll,battleHP,battleTurn,lastKind=-1;
    public int origin=-1,target=-1,good=-1,amount,deadline,reward;
    public long rng=17011;
    public int draw(int bound) { rng=rng*48271%2147483647;return (int)(rng%bound); }
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(1);out.writeLong(rng);
        int[] values={voyages,resolved,battles,deliveries,rescues,surveys,souvenirs,claims,
            pending,roll,battleHP,battleTurn,lastKind,origin,target,good,amount,deadline,reward};
        for(int value:values) out.writeInt(value);
    }
    public static AdventureState read(DataInputStream in) throws IOException {
        if(in.readInt()!=1) throw new IOException("Unknown adventure state");
        AdventureState a=new AdventureState();a.rng=in.readLong();
        a.voyages=in.readInt();a.resolved=in.readInt();a.battles=in.readInt();
        a.deliveries=in.readInt();a.rescues=in.readInt();a.surveys=in.readInt();
        a.souvenirs=in.readInt();a.claims=in.readInt();a.pending=in.readInt();a.roll=in.readInt();
        a.battleHP=in.readInt();a.battleTurn=in.readInt();a.lastKind=in.readInt();
        a.origin=in.readInt();a.target=in.readInt();a.good=in.readInt();
        a.amount=in.readInt();a.deadline=in.readInt();a.reward=in.readInt();
        return a;
    }
    public boolean valid() {
        if(rng<1 || rng>=2147483647) return false;
        for(int value:new int[]{voyages,resolved,battles,deliveries,rescues,surveys}) {
            if(value<0 || value>10000000) return false;
        }
        if(!range(souvenirs,0,127) || !range(claims,0,255)) return false;
        return validEvent() && validContract();
    }
    private boolean validEvent() {
        if(!range(pending,-1,7) || !range(roll,0,99) || !range(lastKind,-1,7)) return false;
        if(pending==-1) return roll==0 && battleHP==0 && battleTurn==0;
        return validBattle();
    }
    private boolean validBattle() {
        if(!range(battleHP,0,200) || !range(battleTurn,0,12)) return false;
        if(battleHP>0) return pending==1;
        return battleTurn==0;
    }
    private boolean validContract() {
        if(target==-1) return inactiveContract();
        if(!World.validPort(target) || !World.validPort(origin) || target==origin) return false;
        return range(good,0,5) && range(amount,1,20) && range(deadline,1,10000000) && range(reward,1,1000000);
    }
    private boolean inactiveContract() { return origin==-1 && good==-1 && amount==0 && deadline==0 && reward==0; }
    private static boolean range(int value,int low,int high) { return value>=low && value<=high; }
    public void clearContract() { origin=-1;target=-1;good=-1;amount=0;deadline=0;reward=0; }
}
