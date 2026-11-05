package com.bluehorizon;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class RivalState {
    public final int[] relations=new int[3],pressure=new int[7],letters=new int[3],wins=new int[3];
    public final int[] lastNegotiation={-10,-10,-10};
    public int allies,lastDay=1,lastRaceDay=-7;
    public int raceRival=-1,raceFrom=-1,raceTarget=-1,raceGood=-1,raceAmount,raceDeadline,raceReward;
    public RivalState() { Arrays.fill(pressure,35); }
    public void write(DataOutputStream out) throws IOException {
        for(int[] array:new int[][]{relations,pressure,letters,wins,lastNegotiation}) for(int value:array) out.writeInt(value);
        for(int value:new int[]{allies,lastDay,lastRaceDay,raceRival,raceFrom,raceTarget,raceGood,raceAmount,raceDeadline,raceReward}) out.writeInt(value);
    }
    public static RivalState read(DataInputStream in) throws IOException {
        RivalState s=new RivalState();
        for(int[] array:new int[][]{s.relations,s.pressure,s.letters,s.wins,s.lastNegotiation}) for(int i=0;i<array.length;i++) array[i]=in.readInt();
        s.allies=in.readInt();s.lastDay=in.readInt();s.lastRaceDay=in.readInt();
        s.raceRival=in.readInt();s.raceFrom=in.readInt();s.raceTarget=in.readInt();s.raceGood=in.readInt();
        s.raceAmount=in.readInt();s.raceDeadline=in.readInt();s.raceReward=in.readInt();return s;
    }
    public boolean valid(Engine e) {
        if(!ranges(relations,-50,100) || !ranges(pressure,0,80)) return false;
        if(!ranges(letters,0,e.adventure.deliveries) || !ranges(wins,0,10000000)) return false;
        if(!ranges(lastNegotiation,-10,e.day)) return false;
        if(allies<0 || allies>7 || lastDay<1 || lastDay>e.day) return false;
        return validRace(e.day);
    }
    private boolean validRace(int day) {
        if(lastRaceDay<-7 || lastRaceDay>day) return false;
        if(raceTarget==-1) return inactiveRace();
        if(raceRival<0 || raceRival>2 || !World.validPort(raceFrom) || !World.validPort(raceTarget)) return false;
        return validShipment();
    }
    private boolean validShipment() {
        if(raceFrom==raceTarget || World.region(raceFrom)!=World.region(raceTarget)) return false;
        if(Rivals.owner(raceFrom)!=raceRival || raceGood<0 || raceGood>5) return false;
        return raceAmount==5 && raceDeadline>=1 && raceDeadline<=10000000 && raceReward>0 && raceReward<=1000000;
    }
    private boolean inactiveRace() {
        for(int value:new int[]{raceRival,raceFrom,raceGood}) if(value!=-1) return false;
        return raceAmount==0 && raceDeadline==0 && raceReward==0;
    }
    private static boolean ranges(int[] values,int low,int high) {
        for(int value:values) if(value<low || value>high) return false;
        return true;
    }
    public void clearRace() { raceRival=-1;raceFrom=-1;raceTarget=-1;raceGood=-1;raceAmount=0;raceDeadline=0;raceReward=0; }
}
