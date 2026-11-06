package com.bluehorizon;
import java.io.*;

public final class ExpeditionState {
    public long clues;
    public int treasures,trophies,talks,practical,bossRegion=-1,bossHP,bossTurn;
    public final int[] trust=new int[5];
    public boolean active() { return bossRegion>=0; }
    public void clearBattle() { bossRegion=-1;bossHP=0;bossTurn=0; }
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(1);out.writeLong(clues);
        for(int value:new int[]{treasures,trophies,talks,practical,bossRegion,bossHP,bossTurn}) out.writeInt(value);
        for(int value:trust) out.writeInt(value);
    }
    public static ExpeditionState read(DataInputStream in) throws IOException {
        if(in.readInt()!=1) throw new IOException("Unknown expedition state");
        ExpeditionState x=new ExpeditionState();x.clues=in.readLong();x.treasures=in.readInt();x.trophies=in.readInt();
        x.talks=in.readInt();x.practical=in.readInt();x.bossRegion=in.readInt();x.bossHP=in.readInt();x.bossTurn=in.readInt();
        for(int i=0;i<5;i++) x.trust[i]=in.readInt();return x;
    }
    public boolean valid(Engine e) {
        return validClues(e) && validFlags() && validMaps(e) && validTrust(e) && validBattle(e);
    }
    private boolean validClues(Engine e) { return clues>=0 && (clues>>>56)==0 && (clues & ~e.visited)==0; }
    private boolean validFlags() {
        if(treasures<0 || treasures>127 || trophies<0 || (trophies & ~treasures)!=0) return false;
        return talks>=0 && talks<=1023 && practical>=0 && (practical & ~talks)==0;
    }
    private boolean validMaps(Engine e) {
        for(int region=0;region<7;region++) {
            if((treasures & (1<<region))!=0 && Expeditions.fragments(e,region)<3) return false;
        }
        return true;
    }
    private boolean validTrust(Engine e) {
        for(int i=0;i<5;i++) {
            int recorded=0;
            for(int stage=0;stage<2;stage++) {
                int bit=1<<(i*2+stage);if((talks & bit)==0) continue;
                if(!CrewConversations.unlocked(e,i+1,stage)) return false;
                recorded+=(practical & bit)!=0?1:2;
            }
            if(trust[i]!=recorded) return false;
        }
        return true;
    }
    private boolean validBattle(Engine e) {
        if(!active()) return bossRegion==-1 && bossHP==0 && bossTurn==0;
        if(!battleLocation(e) || !battleUnlocked()) return false;
        return bossHP>0 && bossHP<=RegionBoss.maxHP(bossRegion) && bossTurn>=0 && bossTurn<30;
    }
    private boolean battleLocation(Engine e) {
        return bossRegion<=6 && bossRegion==World.region(e.port) && !e.town.moving() && e.adventure.pending<0;
    }
    private boolean battleUnlocked() { return (treasures & (1<<bossRegion))!=0 && (trophies & (1<<bossRegion))==0; }
}
