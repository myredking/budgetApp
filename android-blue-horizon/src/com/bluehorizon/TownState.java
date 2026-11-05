package com.bluehorizon;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class TownState {
    public int morale=70,from=-1,startDay,previousChapter=-1;
    public long rumors;
    public boolean moving() { return from>=0; }
    public void write(DataOutputStream out)throws IOException {
        out.writeInt(morale);out.writeLong(rumors);out.writeInt(from);out.writeInt(startDay);out.writeInt(previousChapter);
    }
    public static TownState read(DataInputStream in)throws IOException {
        TownState t=new TownState();t.morale=in.readInt();t.rumors=in.readLong();
        t.from=in.readInt();t.startDay=in.readInt();t.previousChapter=in.readInt();return t;
    }
    public boolean valid(Engine e) {
        if(morale<0 || morale>100 || rumors<0 || (rumors>>>World.COUNT)!=0) return false;
        if(from==-1) return startDay==0 && previousChapter==-1;
        return validJourney(e);
    }
    private boolean validJourney(Engine e) {
        if(!World.validPort(from) || from==e.port) return false;
        return startDay>=1 && startDay<=e.day && previousChapter>=0 && previousChapter<=e.chapter;
    }
    public void clearJourney() { from=-1;startDay=0;previousChapter=-1; }
}
